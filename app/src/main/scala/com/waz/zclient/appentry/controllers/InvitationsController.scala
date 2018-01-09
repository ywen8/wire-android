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
package com.waz.zclient.appentry.controllers

import android.content.Context
import com.waz.api.impl.ErrorResponse
import com.waz.model.EmailAddress
import com.waz.utils.events.{EventContext, Signal, SourceSignal}
import com.waz.zclient.appentry.controllers.InvitationsController._
import com.waz.zclient.{Injectable, Injector}

import scala.collection.immutable.ListMap
import scala.concurrent.Future

class InvitationsController(implicit inj: Injector, eventContext: EventContext, context: Context) extends Injectable {

  //TODO: temporary
  val invitations: SourceSignal[ListMap[EmailAddress, InvitationStatus]] = Signal(ListMap.empty[EmailAddress, InvitationStatus])


  def sendInvite(email: EmailAddress): Future[Either[ErrorResponse, Unit]] = {
      if (invitations.mutate(_ + (email -> Sent)))
        Future.successful(Right({}))
      else
        Future.successful(Left(ErrorResponse.internalError("Already sent")))
  }

  def inviteStatus(email: EmailAddress): Signal[InvitationStatus] = invitations.map(_.applyOrElse(email, (_: EmailAddress) => Failed))

}

object InvitationsController {
  trait InvitationStatus
  object Sending extends InvitationStatus
  object Sent extends InvitationStatus
  object Failed extends InvitationStatus
  object Accepted extends InvitationStatus
}
