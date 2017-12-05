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
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.{Gravity, View}
import android.widget.{LinearLayout, TextView}
import com.waz.model.{Contact, UserData}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.usersearch.views.ContactListItemTextView._
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.StringUtils
import com.waz.zclient.{Injectable, R, ViewHelper}
import ContactListItemTextView._
import com.waz.zclient.messages.UsersController
import com.waz.zclient.views.AvailabilityView


object ContactListItemTextView {
  private val SEPARATOR_SYMBOL: String = " · "

  def getFormattedSubLabel(name: String, userHandle: String, addressBookName: String, showContactDetails: Boolean)(implicit context: Context): String = {
    val usernameString = StringUtils.formatHandle(userHandle)
    val otherString =
      if (addressBookName.nonEmpty && showContactDetails) {
        if (name.equalsIgnoreCase(addressBookName))
          getString(R.string.people_picker__contact_list_contact_sub_label_address_book_identical)
        else
          getString(R.string.people_picker__contact_list_contact_sub_label_address_book, addressBookName)
      } else ""

    if (TextUtils.isEmpty(userHandle))
      otherString
    else if (TextUtils.isEmpty(otherString))
      usernameString
    else
      usernameString + ContactListItemTextView.SEPARATOR_SYMBOL + otherString
  }

  def getFormattedSubLabel(userData: UserData, contact: Option[Contact], showContactDetails: Boolean)(implicit context: Context): String = {
    val username = userData.handle.fold("")(_.string)
    val addressBookName = contact.fold("")(_.name)
    val name = userData.getDisplayName.trim
    getFormattedSubLabel(name, username, addressBookName, showContactDetails)
  }
}

//TODO: the conversion from java got a bit messy due to the Contact model
//TODO: listening to the model could be delegated to the view holder or the container view in this case
class ContactListItemTextView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends LinearLayout(context, attrs, defStyleAttr) with ViewHelper with Injectable {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  inflate(R.layout.contact_list_item_text_layout, this, addToParent = true)

  private val nameView = findById[TextView](R.id.ttv__contactlist__user__name)
  private val subLabelView = findById[TextView](R.id.ttv__contactlist__user__username_and_address_book)

  val zms = inject[Signal[ZMessaging]]
  val userDataSignal = Signal[UserData]
  lazy val usersController = inject[UsersController]
  val showContactDetailsSignal = Signal[Boolean]
  val userDetails = for {
    z <- zms
    userData <- userDataSignal
    showContactDetails <- showContactDetailsSignal
    contactDetails <- z.contacts.contactForUser(userData.id)
  } yield (userData, contactDetails, showContactDetails)

  userDetails.on(Threading.Ui) {
    case (userData, contactDetails, showContactDetails) =>
      drawUser(userData, contactDetails, showContactDetails)
  }

  def setUser(userData: UserData, showContactDetails: Boolean): Unit = {
    recycle()
    drawUser(userData, None, showContactDetails)
    userDataSignal ! userData
    showContactDetailsSignal ! showContactDetails
  }

  private def drawUser(userData: UserData, contact: Option[Contact], showContactDetails: Boolean): Unit = {
    val sublabel  = getFormattedSubLabel(userData, contact, showContactDetails)

    contact.fold{
      if (TextUtils.isEmpty(sublabel)) {
        nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL)
      }
      else {
        nameView.setGravity(Gravity.START | Gravity.BOTTOM)
        subLabelView.setVisibility(View.VISIBLE)
        subLabelView.setText(sublabel)
      }
      nameView.setText(userData.getDisplayName)
      usersController.availability(userData.id).head.foreach { av =>
        AvailabilityView.displayLeftOfText(nameView, av, nameView.getCurrentTextColor)
      }(Threading.Ui)
    } { contact =>
      nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL)
      nameView.setText(contact.name)
      subLabelView.setVisibility(View.GONE)
    }
  }

  def setContact(contact: com.waz.api.Contact): Unit = {
    Option(contact.getUser).fold{
      nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL)
      Option(contact.getDetails).foreach(details => nameView.setText(details.getDisplayName))
      subLabelView.setVisibility(View.GONE)
    }{ user =>
      val sublabel = getFormattedSubLabel(user.getDisplayName, user.getUsername, Option(contact.getDetails).map(_.getDisplayName).getOrElse(""), showContactDetails = true)
      if (TextUtils.isEmpty(sublabel)) {
        nameView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL)
      }
      else {
        nameView.setGravity(Gravity.START | Gravity.BOTTOM)
        subLabelView.setVisibility(View.VISIBLE)
        subLabelView.setText(sublabel)
      }
      nameView.setText(user.getDisplayName)
    }
  }

  def recycle(): Unit = {
    nameView.setText("")
    AvailabilityView.hideAvailabilityIcon(nameView)
    subLabelView.setText("")
    subLabelView.setVisibility(View.GONE)
  }

  def applyDarkTheme(): Unit = {
    nameView.setTextColor(ContextCompat.getColor(getContext, R.color.text__primary_dark))
  }
}
