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
package com.waz.zclient.common.views

import android.content.Context
import android.graphics.Color
import android.support.v7.widget.AppCompatCheckBox
import android.util.AttributeSet
import android.view.View
import android.widget.{ImageView, RelativeLayout}
import com.waz.model.UserData
import com.waz.utils.returning
import com.waz.zclient.common.views.SingleUserRowView._
import com.waz.zclient.paintcode.{ForwardNavigationIcon, GuestIcon}
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils
import com.waz.zclient.{R, ViewHelper}

class SingleUserRowView(context: Context, attrs: AttributeSet, style: Int) extends RelativeLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.participant_row_view)
  setTheme(Light)

  private lazy val chathead = findById[ChatheadView](R.id.chathead)
  private lazy val nameView = findById[TypefaceTextView](R.id.name_text)
  private lazy val usernameView = findById[TypefaceTextView](R.id.username_text)
  private lazy val checkbox = findById[AppCompatCheckBox](R.id.checkbox)
  private lazy val verifiedShield = findById[ImageView](R.id.verified_shield)
  private lazy val guestIndicator = returning(findById[ImageView](R.id.guest_indicator))(_.setImageDrawable(GuestIcon(R.color.light_graphite)))
  private lazy val nextIndicator = returning(findById[ImageView](R.id.next_indicator))(_.setImageDrawable(ForwardNavigationIcon(R.color.light_graphite)))

  def setTitle(text: String): Unit = nameView.setText(text)

  def setSubtitle(text: Option[String]): Unit = text.fold(usernameView.setVisibility(View.GONE)) { t =>
    usernameView.setVisibility(View.VISIBLE)
    usernameView.setText(t)
  }

  def setChecked(checked: Boolean): Unit = checkbox.setChecked(checked)

  def setVerified(verified: Boolean): Unit = verifiedShield.setVisibility(if (verified) View.VISIBLE else View.GONE)

  def showArrow(show: Boolean): Unit = nextIndicator.setVisibility(if (show) View.VISIBLE else View.GONE)

  def setUserData(userData: UserData): Unit = {
    chathead.setUserId(userData.id)
    setTitle(userData.getDisplayName)
    setVerified(userData.isVerified)
    setSubtitle(userData.handle.map(_.string))
  }

  def setIsGuest(guest: Boolean): Unit = guestIndicator.setVisibility(if (guest) View.VISIBLE else View.GONE)

  def showCheckbox(show: Boolean) = checkbox.setVisibility(if (show) View.VISIBLE else View.GONE)

  //TODO: Proper colors and other stuff
  def setTheme(theme: Theme): Unit = {
    theme match {
      case Light =>
        setBackgroundColor(Color.WHITE)
      case Dark =>
        setBackgroundColor(Color.BLACK)
      case Transparent =>
        setBackground(ContextUtils.getDrawable(R.drawable.selector__transparent_button))
    }
  }
}

object SingleUserRowView {
  trait Theme
  object Light extends Theme
  object Dark extends Theme
  object Transparent extends Theme
}
