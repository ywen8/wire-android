/**
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.controllers.global

import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.model.MessageId
import com.waz.service.ZMessaging
import com.waz.utils.events.{EventContext, EventStream, Signal}
import com.waz.zclient.{Injectable, Injector}
import org.threeten.bp.Instant

class SelectionController(implicit injector: Injector, ev: EventContext) extends Injectable {

  private val zms = inject[Signal[ZMessaging]]

  val selectedConv = zms.flatMap(_.convsStats.selectedConversationId).collect { case Some(convId) => convId }

  object messages {

    //(currentlyFocusedMsg, prevFocusedMsg, timeFocusChanged)
    private val focused = Signal((Option.empty[MessageId], Option.empty[MessageId], Instant.EPOCH))

    selectedConv.onChanged { _ => clear() }

    def clear() = focused ! (None, None, Instant.EPOCH)

    def isFocused(id: MessageId): Signal[Boolean] = focused.map {
      case (Some(`id`), _, _) => true
      case _ => false
    }

    //Returns the time that a message either gains or loses focus
    def focusChangedTime(id: MessageId): Signal[Instant] = focused.map {
      case (Some(`id`), _, time) => time
      case (_, Some(`id`), time) => time
      case _ => Instant.EPOCH
    }

    def onFocusChanged: EventStream[Option[MessageId]] = focused.map(_._1).onChanged

    def lastFocused: Option[MessageId] = focused.currentValue.flatMap(_._1)

    //Shuffle the currently focused message down to previously focused message. If they're the same, the current
    //focused message is then None (no focus), else, put the newly focused message in the current spot.
    def toggleFocused(id: MessageId) = {
      verbose(s"toggleFocused($id)")
      focused.mutate {
        case (oldId@Some(`id`), _, _) => (None, oldId, Instant.now)
        case (oldId, _, _)            => (Some(id), oldId, Instant.now)
      }
    }
  }
}
