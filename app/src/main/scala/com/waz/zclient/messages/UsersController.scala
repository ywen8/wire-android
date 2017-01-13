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
package com.waz.zclient.messages

import android.content.Context
import com.waz.api.impl.AccentColor
import com.waz.model.ConversationData.ConversationType
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.zclient.messages.UsersController.DisplayName
import com.waz.zclient.messages.UsersController.DisplayName.{Me, Other}
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{Injectable, Injector, R}

class UsersController(implicit injector: Injector, context: Context) extends Injectable {

  private val zMessaging = inject[Signal[ZMessaging]]

  lazy val itemSeparator = getString(R.string.content__system__item_separator)
  lazy val lastSeparator = getString(R.string.content__system__last_item_separator)

  lazy val selfUserId = zMessaging map { _.selfUserId }

  //Always returns the other user for the conversation for a given message, regardless of who sent the message
  def getOtherUser(message: Signal[MessageData]): Signal[Option[UserData]] = for {
    zms <- zMessaging
    msg <- message
    conv <- zms.convsStorage.signal(msg.convId)
    user <- zms.users.userSignal(UserId(conv.id.str))
  } yield if (ConversationType.isOneToOne(conv.convType)) Some(user) else None

  def displayNameString(id: UserId): Signal[String] =
    displayName(id) map {
      case Me => getString(R.string.content__system__you)
      case Other(name) => name
    }

  def displayName(id: UserId): Signal[DisplayName] = zMessaging.flatMap { zms =>
    if (zms.selfUserId == id) Signal const Me
    else zms.users.userSignal(id).map(u => Other(u.getDisplayName))
  }

  def accentColor(id: UserId): Signal[AccentColor] = user(id).map(u => AccentColor(u.accent))

  def memberDisplayNames(message: Signal[MessageData]) =
    for {
      zms <- zMessaging
      msg <- message
      names <- Signal.sequence[String](msg.members.toSeq.sortBy(_.str).map(displayNameString): _*)
    } yield
      names match {
        case Seq() => ""
        case Seq(name) => name
        case _ =>
          val n = names.length
          s"${names.take(n - 1).mkString(itemSeparator + " ")} $lastSeparator  ${names.last}"
      }

  def userHandle(id: UserId): Signal[Option[Handle]] = user(id).map(_.handle)

  def userFirstContact(id: UserId): Signal[Option[Contact]] =
    for {
      zms <- zMessaging
      contact <- zms.contacts.contactForUser(id)
    } yield contact

  def user(id: UserId) = zMessaging flatMap { _.users.userSignal(id) }

  def conv(msg: MessageData) = {
    for {
      zms <- zMessaging
      conv <- zms.convsStorage.signal(msg.convId)
    } yield conv
  }

}

object UsersController {

  sealed trait DisplayName
  object DisplayName {
    case object Me extends DisplayName
    case class Other(name: String) extends DisplayName
  }
}
