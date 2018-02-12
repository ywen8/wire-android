/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.conversation.creation

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.{LinearLayoutManager, RecyclerView, Toolbar}
import android.view.{ContextThemeWrapper, LayoutInflater, View, ViewGroup}
import com.waz.ZLog.ImplicitTag._
import com.waz.model.{UserData, UserId}
import com.waz.service.ZMessaging
import com.waz.threading.{CancellableFuture, Threading}
import com.waz.utils.events._
import com.waz.utils.returning
import com.waz.zclient.common.views.PickableElement
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.usersearch.views.{PickerSpannableEditText, SearchEditText, SearchResultUserRowView}
import com.waz.zclient.utils.ContextUtils.getInt
import com.waz.zclient.utils.RichView
import com.waz.zclient.{FragmentHelper, R, ViewHelper}

import scala.collection.immutable.Set
import scala.concurrent.duration._

class NewConversationPickFragment extends Fragment with FragmentHelper {

  import NewConversationPickFragment._
  import Threading.Implicits.Background
  implicit def cxt: Context = getContext

  private lazy val zms               = inject[Signal[ZMessaging]]
  private lazy val newConvController = inject[NewConversationController]

  private lazy val searchFilter = Signal("")

  private lazy val searchResults = for {
    zms     <- zms
    filter  <- searchFilter
    convId  <- newConvController.convId
    results <- zms.userSearch.searchLocal(filter, toConv = convId)
  } yield results

  private lazy val adapter = NewConvAdapter(searchResults, newConvController.users)

  private lazy val toolbar = returning(view[Toolbar](R.id.toolbar)) { vh =>
    newConvController.convId.map(_.nonEmpty).onUi { hasConv =>
      vh.foreach(_.setVisibility(if (hasConv) View.VISIBLE else View.GONE))
    }
  }

  private lazy val nextButton = returning(view[TypefaceTextView](R.id.confirmation_button)) { vh =>
    newConvController.users.map(_.nonEmpty).onUi { hasUsers =>
      vh.foreach(_.setEnabled(hasUsers))
    }
  }

  private lazy val searchBox = returning(view[SearchEditText](R.id.search_box)) { vh =>
    new FutureEventStream[(UserId, Boolean), (PickableUser, Boolean)](adapter.onUserSelectionChanged, {
      case (userId, selected) =>
        zms.head.flatMap(_.users.getUser(userId).collect { case Some(u) => (PickableUser(userId, u.name), selected) })
    }).onUi {
      case (pu, selected) =>
        vh.foreach { v =>
          if (selected) v.addElement(pu) else v.removeElement(pu)
        }
    }
  }

  private val errorTextState = for {
    searchFilter <- searchFilter
    results <- searchResults
  } yield (results.isEmpty, searchFilter.isEmpty) match {
    case (true, true) => (true, R.string.new_conv_no_contacts)
    case (true, false) => (true, R.string.new_conv_no_results)
    case _ => (false, R.string.empty_string)
  }

  private lazy val errorText = returning(view[TypefaceTextView](R.id.empty_search_message)) { vh =>
    errorTextState.onUi { case (visible, text) =>
      vh.foreach { errorText =>
        errorText.setVisible(visible)
        errorText.setText(text)
      }
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View =
    inflater.cloneInContext(new ContextThemeWrapper(getActivity, R.style.Theme_Light))
      .inflate(R.layout.create_conv_pick_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    val recyclerView = findById[RecyclerView](R.id.recycler_view)
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext))
    recyclerView.setAdapter(adapter)

    toolbar.foreach(_.setVisibility(if (newConvController.convId.currentValue.flatten.nonEmpty) View.VISIBLE else View.GONE))

    nextButton.foreach { v =>
      v.onClick {
        newConvController.addUsersToConversation()
        getFragmentManager.popBackStack()
      }
      v.setEnabled(newConvController.users.currentValue.exists(_.nonEmpty))
    }

    searchBox.foreach { v =>
      v.applyDarkTheme(false)
      v.setCallback(new PickerSpannableEditText.Callback{
        override def onRemovedTokenSpan(element: PickableElement): Unit =
          newConvController.users.mutate(_ - UserId(element.id))
        override def afterTextChanged(s: String): Unit =
          searchFilter ! s
      })
    }

    errorText.foreach { errorText =>
      errorTextState.currentValue.foreach{ case (visible, text) =>
        errorText.setVisible(visible)
        errorText.setText(text)
      }
    }
  }

  override def onResume() = {
    super.onResume()
    CancellableFuture.delay(getInt(R.integer.people_picker__keyboard__show_delay).millis).map { _ =>
      searchResults.head.map(_.size > ShowKeyboardThreshold).map {
        case true =>
          searchBox.foreach { v =>
            v.setFocus()
            KeyboardUtils.showKeyboard(getActivity)
          }
        case _ =>
      } (Threading.Ui)
    }
  }
}

object NewConversationPickFragment {

  val ShowKeyboardThreshold = 10
  val Tag = implicitLogTag

  private case class PickableUser(userId : UserId, userName: String) extends PickableElement {
    def id: String = userId.str
    def name: String = userName
  }
}


case class NewConvUserViewHolder(v: SearchResultUserRowView) extends RecyclerView.ViewHolder(v) {
  def bind(userData: UserData, selected: Boolean) = {
    v.setUser(userData)
    v.setChecked(selected)
  }
}

case class NewConvAdapter(searchResults: Signal[IndexedSeq[UserData]], selectedUsers: SourceSignal[Set[UserId]])(implicit context: Context, eventContext: EventContext) extends RecyclerView.Adapter[NewConvUserViewHolder] {
  private implicit val ctx = context

  private var users = Seq.empty[(UserData, /*isSelected: */ Boolean)]

  val onUserSelectionChanged = EventStream[(UserId, Boolean)]()

  setHasStableIds(true)

  (for {
    res <- searchResults
    sel <- selectedUsers
  } yield (res, sel))
    .onUi {
      case (res, sel) =>
        if (users.map(_._1).toSet == res.toSet) {
          val changedPositions = users.map {
            case (user, selected) =>
              if (selected && !sel.contains(user.id) || !selected && sel.contains(user.id)) users.map(_._1).indexOf(user) else -1
          }
          this.users = res.map(u => (u, sel.contains(u.id)))
          changedPositions.filterNot(_ == -1).foreach(notifyItemChanged)
        } else {
          this.users = res.map(u => (u, sel.contains(u.id)))
          notifyDataSetChanged()
        }
    }

  override def getItemCount: Int = users.size

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): NewConvUserViewHolder = {
    val view = ViewHelper.inflate[SearchResultUserRowView](R.layout.startui_user, parent, addToParent = false)
    view.setIsAddingPeople(true)
    view.onSelectionChanged.onUi { selected =>
      view.getUser.foreach { user =>
        onUserSelectionChanged ! (user, selected)
        if (selected)
          selectedUsers.mutate(_ + user)
        else
          selectedUsers.mutate(_ - user)
      }
    }
    NewConvUserViewHolder(view)
  }


  override def getItemId(position: Int) = users(position)._1.id.str.hashCode

  override def onBindViewHolder(holder: NewConvUserViewHolder, position: Int): Unit = {
    val (user, selected) = users(position)
    holder.bind(user, selected)
  }
}
