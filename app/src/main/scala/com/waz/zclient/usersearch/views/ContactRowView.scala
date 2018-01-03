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
package com.waz.zclient.usersearch.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.waz.api.{Contact, ContactDetails}
import com.waz.model.UserData.ConnectionStatus
import com.waz.model.{UserData, UserId}
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.common.controllers.global.AccentColorController
import com.waz.zclient.common.views.ChatheadView
import com.waz.zclient.core.api.scala.ModelObserver
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.usersearch.adapters.PickUsersAdapter
import com.waz.zclient.utils.{UiStorage, UserSignal}
import com.waz.zclient.{Injectable, R, ViewHelper}

//TODO: remove the uiObservable for contact?
class ContactRowView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends FrameLayout(context, attrs, defStyleAttr) with UserRowView with ViewHelper with Injectable {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  implicit private val uiStorage = inject[UiStorage]

  inflate(R.layout.list_row_contactlist_user, this)

  private val chathead = findById[ChatheadView](R.id.cv__contactlist__user__chathead)
  private val contactListItemTextView = findById[ContactListItemTextView](R.id.clitv__contactlist__user__text_view)
  private val contactInviteButton = findById[ZetaButton](R.id.zb__contactlist__user_selected_button)

  private val userId = Signal(Option.empty[UserId])
  private var user = Option.empty[UserData]
  private var contactDetails = Option.empty[ContactDetails]
  private var callback = Option.empty[PickUsersAdapter.Callback]

  inject[AccentColorController].accentColor.on(Threading.Ui) { accentColor =>
    contactInviteButton.setAccentColor(accentColor.getColor)
  }

  val contactDetailsModelObserver = new ModelObserver[ContactDetails]() {
    override def updated(model: ContactDetails): Unit = {
      contactDetails = Option(model)
      redraw()
    }
  }

  userId.flatMap {
    case Some(uId) => UserSignal(uId).map(Option(_))
    case None => Signal.const(Option.empty[UserData])
  }.onUi { data =>
    user = data
    redraw()
  }

  def setCallback(callback: PickUsersAdapter.Callback): Unit = this.callback = Some(callback)

  def setContact(contact: Contact): Unit =
    Option(contact).foreach { c =>
      contactDetailsModelObserver.clear()
      contactDetails = None
      user = None
      contactDetailsModelObserver.setAndUpdate(c.getDetails)
      userId ! Option(c.getUser).map(u => UserId(u.getId))
      contactListItemTextView.setContact(c)
    }

  def applyDarkTheme(): Unit = {
    contactListItemTextView.applyDarkTheme()
  }

  def getUser: Option[UserId] = user.map(_.id)

  def onClicked(): Unit = {
    if (user.exists(_.connection == UserData.ConnectionStatus.Accepted)) {
      setSelected(!isSelected)
    }
  }

  override def setSelected(selected: Boolean): Unit = {
    super.setSelected(selected)
    chathead.setSelected(selected)
  }

  private def redraw(): Unit = {
    if (user.nonEmpty) {
      drawUser()
    }
    else if (contactDetails.nonEmpty) {
      drawContact()
    }
  }

  private def drawUser(): Unit = {
    user.foreach { user =>
      chathead.setUserId(user.id)
      user.connection match {
        case ConnectionStatus.Unconnected | ConnectionStatus.Cancelled =>
          contactInviteButton.setVisibility(View.VISIBLE)
          contactInviteButton.setText(getResources.getText(R.string.people_picker__contact_list__contact_selection_button__label))
        case ConnectionStatus.Accepted =>
          setSelected(callback.exists(_.getSelectedUsers.contains(user.id)))
          contactInviteButton.setVisibility(View.GONE)
        case ConnectionStatus.PendingFromUser | ConnectionStatus.PendingFromOther =>
          contactInviteButton.setVisibility(View.GONE)
        case _ =>
      }
      contactInviteButton.setOnClickListener(new View.OnClickListener() {
        def onClick(view: View): Unit =
          callback.foreach(_.onContactListUserClicked(user.id))
      })
    }
  }

  private def drawContact(): Unit = {
    contactDetails.foreach{ details =>
      chathead.setContactDetails(details)

      if (details.hasBeenInvited) {
        contactInviteButton.setVisibility(View.GONE)
        setOnClickListener(new View.OnClickListener() {
          def onClick(view: View): Unit = callback.foreach(_.onContactListContactClicked(details))
        })
      } else {
        contactInviteButton.setVisibility(View.VISIBLE)
        contactInviteButton.setText(getResources.getText(R.string.people_picker__contact_list__contact_selection_button__label))
        contactInviteButton.setOnClickListener(new View.OnClickListener() {
          def onClick(view: View): Unit = callback.foreach(_.onContactListContactClicked(details))
        })
      }

    }
  }
}
