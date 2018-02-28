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

import com.waz.ZLog.ImplicitTag._
import com.waz.model.{ConvId, UserId}
import com.waz.service.ZMessaging
import com.waz.service.tracking._
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.utils.UiStorage
import com.waz.zclient.{Injectable, Injector}

import scala.concurrent.Future

class NewConversationController(implicit inj: Injector, ev: EventContext) extends Injectable {
  import com.waz.threading.Threading.Implicits.Background

  private lazy val conversationController = inject[ConversationController]
  private lazy val zms = inject[Signal[ZMessaging]]

  private implicit lazy val uiStorage = inject[UiStorage]
  private lazy val tracking = inject[TrackingService]

  val convId   = Signal(Option.empty[ConvId])
  val name     = Signal("")
  val users    = Signal(Set.empty[UserId])
  val teamOnly = Signal(true)

  teamOnly.onChanged {
    case true =>
      for {
        z   <- zms.head
        ids <- users.head
        us  <- Future.sequence(ids.map(z.users.getUser)).map(_.flatten)
      } yield users.mutate(_ -- us.filter(_.isGuest(z.teamId)).map(_.id))
    case false => //
  }

  val fromScreen = Signal[GroupConversationEvent.Method]()

  def setCreateConversation(preSelectedUsers: Set[UserId] = Set(), from: GroupConversationEvent.Method): Unit = {
    name ! ""
    users ! preSelectedUsers
    convId ! None
    fromScreen ! from
    teamOnly ! false
    tracking.track(CreateGroupConversation(from))
  }

  def setAddToConversation(conv: ConvId): Unit = {
    name ! ""
    users ! Set()
    convId ! Some(conv)
    fromScreen ! GroupConversationEvent.ConversationDetails
  }

  def createConversation(): Future[ConvId] =
    for {
      name     <- name.head
      users    <- users.head
      teamOnly <- teamOnly.head
      conv     <- conversationController.createGroupConversation(Some(name.trim), users, teamOnly)
      from     <- fromScreen.head
    } yield {
      tracking.track(GroupConversationSuccessful(users.nonEmpty, from))
      conv.id
    }

  def addUsersToConversation(): Future[Unit] = {
    for {
      Some(convId) <- convId.head
      users <- users.head
      _ <- conversationController.addMembers(convId, users)
    } yield ()
  }
}
