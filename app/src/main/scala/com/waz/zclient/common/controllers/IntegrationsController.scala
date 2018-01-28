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
package com.waz.zclient.common.controllers

import android.content.Context
import com.waz.api.impl.ErrorResponse
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.utils.ContextUtils.getString
import com.waz.zclient.{Injectable, Injector, R}
import com.waz.ZLog.ImplicitTag._

import scala.concurrent.Future

class IntegrationsController(implicit injector: Injector, context: Context) extends Injectable {
  import Threading.Implicits.Background

  private lazy val integrations = inject[Signal[ZMessaging]].map(_.integrations)
  private lazy val conversationController = inject[ConversationController]

  val searchQuery = Signal[String]("")

  def searchIntegrations = for {
    in        <- integrations
    startWith <- searchQuery
    data      <- in.searchIntegrations(startWith).map(Option(_)).orElse(Signal.const(Option.empty[Seq[IntegrationData]]))
  } yield data.map(_.toIndexedSeq)

  def getIntegration(pId: ProviderId, iId: IntegrationId): Future[IntegrationData] =
    integrations.head.flatMap(_.getIntegration(pId, iId))

  def addBot(cId: ConvId, pId: ProviderId, iId: IntegrationId): Future[Either[ErrorResponse, Unit]] =
    integrations.head.flatMap(_.addBotToConversation(cId, pId, iId))

  def createConvWithBot(pId: ProviderId, iId: IntegrationId): Future[Either[ErrorResponse, ConvId]] =
    integrations.head.flatMap(_.createConversationWithBot(pId, iId))

  def removeBot(cId: ConvId, userId: UserId): Future[Either[ErrorResponse, Unit]] =
    integrations.head.flatMap(_.removeBotFromConversation(cId, userId))

  def errorMessage(e: ErrorResponse): String =
    getString((e.code, e.label) match {
      case (403, "too-many-members") => R.string.conversation_errors_full
//      case (419, "too-many-bots")    => R.string.integrations_errors_add_service //TODO ???
      case (_, _)                    => R.string.integrations_errors_service_unavailable
    })
}

