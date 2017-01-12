/*
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.waz.zclient.common.views

import android.content.Context
import android.util.AttributeSet
import android.widget.{LinearLayout, TextView}
import com.waz.model.UserId
import com.waz.utils.events.Signal
import com.waz.zclient.messages.SyncEngineSignals
import com.waz.zclient.utils.StringUtils
import com.waz.zclient.{R, ViewHelper}

class UserDetailsView(val context: Context, val attrs: AttributeSet, val defStyle: Int) extends LinearLayout(context, attrs, defStyle) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  private lazy val userNameTextView: TextView = findById(R.id.ttv__user_details__user_name)
  private lazy val userInfoTextView: TextView = findById(R.id.ttv__user_details__user_info)
  inflate(R.layout.user__details, this, true)

  val signals = inject[SyncEngineSignals]
  val userId = Signal[UserId]
  val handle = signals.userHandle(userId)
  val firstContact = signals.userFirstContact(userId)
  val displayName = userId.flatMap(signals.displayName)

  val handleText = handle.map {
    case Some(handle) => StringUtils.formatUsername(handle.string)
    case None => ""
  }

  val contactText = for {
    c <- firstContact
    n <- displayName
  } yield {
      c match {
        case Some(cont) if cont.name == n => getContext.getString(R.string.content__message__connect_request__user_info, "")
        case Some(cont) => getContext.getString(R.string.content__message__connect_request__user_info, c.get.name)
        case _ => ""
      }
  }

  handleText { userNameTextView.setText }
  contactText { userInfoTextView.setText }

  def setUserId(userId: UserId) = Option(userId).fold(throw new IllegalArgumentException("UserId should not be null")){
    userNameTextView.setText("")
    userInfoTextView.setText("")
    this.userId ! _
  }

}
