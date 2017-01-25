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
package com.waz.zclient.tracking

import java.util

import com.waz.service.ZMessaging
import com.waz.utils.events.{EventContext, Signal}
import com.waz.utils.returning
import com.waz.zclient.controllers.tracking.ITrackingController
import com.waz.zclient.core.controllers.tracking.attributes.{Attribute, RangedAttribute}
import com.waz.zclient.{Injectable, Injector, WireContext}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

class GlobalTrackingController(implicit inj: Injector, cxt: WireContext, eventContext: EventContext) extends Injectable {

  val zMessaging = inject[Signal[ZMessaging]]
  //TODO steadily shift methods from ITrackingController to here..
  val legacyController = inject[ITrackingController]

}

object GlobalTrackingController {
  //implicit converter from Scala tracking event to Java tracking event for compatibility with older tracking code
  implicit def toJava(event: Event): com.waz.zclient.core.controllers.tracking.events.Event = new com.waz.zclient.core.controllers.tracking.events.Event {
    override def getName: String = event.name

    override def getRangedAttributes: util.Map[RangedAttribute, Integer] = {
      returning(new util.HashMap[RangedAttribute, Integer]()) { attrs =>
        event.rangedAttributes.foreach { case (ra, int) =>
          attrs.put(ra, Integer.valueOf(int))
        }
      }
    }

    override def getAttributes: util.Map[Attribute, String] = event.attributes.asJava
  }
}
