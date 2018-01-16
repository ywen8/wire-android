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
import com.waz.model.{IntegrationData, IntegrationId, ProviderId}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.{Injectable, Injector}
import com.waz.ZLog.verbose
import com.waz.ZLog.ImplicitTag._

import scala.concurrent.Future

class IntegrationsController(implicit injector: Injector, context: Context) extends Injectable {

  private lazy val integrations = inject[Signal[ZMessaging]].map(_.integrations)

  val searchQuery = Signal[String]("")

//  def searchIntegrations =  for {
//    zms       <- inject[Signal[ZMessaging]]
//    startWith <- searchQuery
//    _ = verbose(s"IN looking for integration starting with $startWith")
//    data      <- zms.integrations.searchIntegrations(startWith).map(Option(_)).orElse(Signal.const(Option.empty[Seq[IntegrationData]]))
//    _ = verbose(s"IN found: ${data.map(_.map(b => (b.name, b.description, b.assets)))}")
//  } yield data.map(_.toIndexedSeq)

  private val fakeIntegration = IntegrationData(name = "service 1", summary = "service summary", description = "service description")

  def searchIntegrations = Signal.const(Option(IndexedSeq(fakeIntegration)))

  def getIntegration(providerId: ProviderId, integrationId: IntegrationId): Signal[IntegrationData] =
    if (integrationId == fakeIntegration.id) Signal.const(fakeIntegration)// Future.successful(fakeIntegration)
    else Signal.empty[IntegrationData] //Future.failed(new Exception("no integration"))
    //integrations.head.flatMap(_.getIntegration(providerId, integrationId))(Threading.Ui)

}
