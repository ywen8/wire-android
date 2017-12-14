/**
 * Wire
 * Copyright (C) 2017 Wire Swiss GmbH
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
import com.waz.api.{Contact, ContactDetails, User}
import com.waz.model.UserData.ConnectionStatus
import com.waz.model.UserId
import com.waz.threading.Threading
import com.waz.zclient.common.controllers.global.AccentColorController
import com.waz.zclient.common.views.ChatheadView
import com.waz.zclient.core.api.scala.ModelObserver
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.{Injectable, R, ViewHelper}

object ContactRowView {

  trait Callback {
    def onContactListUserClicked(user: User): Unit

    def onContactListContactClicked(contactDetails: ContactDetails): Unit

    @IPickUserController.ContactListDestination def getDestination: Int

    def isUserSelected(user: User): Boolean
  }

}

//TODO: remove the uiObservable for contact?
class ContactRowView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends FrameLayout(context, attrs, defStyleAttr) with UserRowView with ViewHelper with Injectable {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  inflate(R.layout.list_row_contactlist_user, this, addToParent = true)

  private val chathead = findById[ChatheadView](R.id.cv__contactlist__user__chathead)
  private val contactListItemTextView = findById[ContactListItemTextView](R.id.clitv__contactlist__user__text_view)
  private val contactInviteButton = findById[ZetaButton](R.id.zb__contactlist__user_selected_button)

  private var user = Option.empty[User]
  private var contactDetails = Option.empty[ContactDetails]
  private var callback: ContactRowView.Callback = null

  inject[AccentColorController].accentColor.on(Threading.Ui) { accentColor =>
    contactInviteButton.setAccentColor(accentColor.getColor())
  }

  val contactDetailsModelObserver = new ModelObserver[ContactDetails]() {
    override def updated(model: ContactDetails): Unit = {
      contactDetails = Option(model)
      redraw()

    }
  }

  val userModelObserver = new ModelObserver[User]() {
    override def updated(model: User): Unit = {
      user = Option(model)
      redraw()
    }
  }

  def setCallback(callback: ContactRowView.Callback): Unit = {
    this.callback = callback
  }

  def setContact(contact: Contact): Unit =
    Option(contact).foreach { c =>
      userModelObserver.clear()
      contactDetailsModelObserver.clear()
      contactDetails = None
      user = None
      contactDetailsModelObserver.setAndUpdate(c.getDetails)
      userModelObserver.setAndUpdate(c.getUser)
      contactListItemTextView.setContact(c)
    }

  def applyDarkTheme(): Unit = {
    contactListItemTextView.applyDarkTheme()
  }

  def getUser: Option[UserId] = user.map(user => UserId(user.getId))

  def onClicked(): Unit = {
    if (user.exists(_.getConnectionStatus eq User.ConnectionStatus.ACCEPTED)) {
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
    user.foreach{ user =>
      chathead.setUser(user)
      user.getConnectionStatus match {
        case ConnectionStatus.Unconnected | ConnectionStatus.Cancelled =>
          contactInviteButton.setVisibility(View.VISIBLE)
          contactInviteButton.setText(getResources.getText(R.string.people_picker__contact_list__contact_selection_button__label))
        case ConnectionStatus.Accepted =>
          setSelected(callback.isUserSelected(user))
          contactInviteButton.setVisibility(View.GONE)
        case ConnectionStatus.PendingFromUser | ConnectionStatus.PendingFromOther =>
          contactInviteButton.setVisibility(View.GONE)
        case _ =>
      }
      contactInviteButton.setOnClickListener(new View.OnClickListener() {
        def onClick(view: View): Unit =
          Option(callback).foreach(_.onContactListUserClicked(user))
      })
    }
  }

  private def drawContact(): Unit = {
    contactDetails.foreach{ details =>
      chathead.setContactDetails(details)

      if (details.hasBeenInvited) {
        contactInviteButton.setVisibility(View.GONE)
        setOnClickListener(new View.OnClickListener() {
          def onClick(view: View): Unit = Option(callback).foreach(_.onContactListContactClicked(details))
        })
      } else {
        contactInviteButton.setVisibility(View.VISIBLE)
        contactInviteButton.setText(getResources.getText(R.string.people_picker__contact_list__contact_selection_button__label))
        contactInviteButton.setOnClickListener(new View.OnClickListener() {
          def onClick(view: View): Unit = Option(callback).foreach(_.onContactListContactClicked(details))
        })
      }

    }
  }
}
