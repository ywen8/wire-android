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
package com.waz.zclient.callquality

import com.waz.service.ZMessaging
import com.waz.service.call.CallInfo
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.calling.controllers.GlobalCallingController
import com.waz.zclient.{Injectable, Injector}

class CallQualityController(implicit inj: Injector, eventContext: EventContext) extends Injectable {

  val zms = inject[Signal[ZMessaging]]
  val callingController = inject[GlobalCallingController]

  val callToReport = Signal(Option.empty[CallInfo])

  zms.flatMap(_.calling.previousCall).on(Threading.Background) { call =>
    callToReport ! call
  }
}
