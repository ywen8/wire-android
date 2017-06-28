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
package com.waz.zclient.controllers.global

import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog.verbose
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.zclient.{Injectable, Injector}
import com.waz.utils.events.EventContext
import com.waz.threading.Threading

class PasswordController(implicit inj: Injector) extends Injectable {
  import Threading.Implicits.Background
  import EventContext.Implicits.global

  lazy val zms = inject[Signal[ZMessaging]]

  val password = zms.flatMap(_.account.accountData).map(_.password)

  password { p =>
    verbose(s"Password updated: $p")
  }

  //The password is never saved in the database, this will just update the in-memory version of the current account
  //so that the password is globally correct.
  def setPassword(p: String) =
    for {
      z <- zms.head
      _ <- z.accountsStorage.update(z.account.id, _.copy(password = Some(p)))
    } yield {}

}
