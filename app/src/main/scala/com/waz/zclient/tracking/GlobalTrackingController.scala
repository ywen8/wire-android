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

import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.api.EphemeralExpiration
import com.waz.api.Message.Type._
import com.waz.content.UserPreferences.AnalyticsEnabled
import com.waz.content.UsersStorage
import com.waz.model.ConversationData.ConversationType
import com.waz.model.UserData.ConnectionStatus.Blocked
import com.waz.model.{UserId, _}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils._
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient._
import com.waz.zclient.preferences.PreferencesController

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.language.{implicitConversions, postfixOps}

class GlobalTrackingController(implicit inj: Injector, cxt: WireContext, eventContext: EventContext) extends Injectable {
  import GlobalTrackingController._
  import Threading.Implicits.Background

  val zmsOpt     = inject[Signal[Option[ZMessaging]]]
  val zMessaging = inject[Signal[ZMessaging]]

  private var registeredZmsInstances = Set.empty[ZMessaging]

  /**
    * WARNING: since we have to first listen to the zms signal in order to find the event streams that we care about for tracking,
    * whenever this signal changes, we will define a new signal subscription, in the closure of which we will generate new subscriptions
    * for all the event streams in that signal. This means that if zms changes and then changes back (switching accounts) or the signal fires
    * twice, we'll have two listeners to each event stream, and we'll end up tagging each event twice.
    *
    * Therefore, we keep a set of registered zms instances, and only register the listeners once.
    */
  zmsOpt {
    case Some(zms) if !registeredZmsInstances(zms) =>
      registeredZmsInstances += zms
      registerTrackingEventListeners(zms)
    case _ => //already registered to this zms, do nothing.
  }

  zMessaging.flatMap(_.userPrefs.preference(AnalyticsEnabled).signal) { enabled =>
    verbose(s"Analytics enabled?: $enabled")
  }

  /**
    * Register tracking event listeners on SE services in this method. We need a method here, since whenever the signal
    * zms fires, we want to discard the previous reference to the subscriber. Not doing so will cause this class to keep
    * reference to old instances of the services under zms (?)
    */
  private def registerTrackingEventListeners(zms: ZMessaging) = {

  }

  def tagEvent(event: TrackingEvent) = {

  }

  private def isTrackingEnabled =
    inject[PreferencesController].isAnalyticsEnabled && !BuildConfig.DISABLE_TRACKING_KEEP_LOGGING

  //-1 is the default value for non-logged in users (when zms is not defined)
  private def setCustomDims(): Future[Unit] = {
    import ConversationType._
    for {
      zms    <- zmsOpt.head
      self   =  zms.fold(UserId.Zero)(_.selfUserId)
      convs  <- zms.fold(Future.successful(Seq.empty[ConversationData]))(_.convsStorage.list)
      users  <- zms.fold(Future.successful(Seq.empty[UserData]))(_.usersStorage.list)
      voice  <- zms.fold(Future.successful(-1))(_.callLog.numberOfEstablishedVoiceCalls)
      video  <- zms.fold(Future.successful(-1))(_.callLog.numberOfEstablishedVideoCalls)
      texts  <- zms.fold(Future.successful(-1))(_.messagesStorage.countSentByType(self, TEXT))
      rich   <- zms.fold(Future.successful(-1))(_.messagesStorage.countSentByType(self, RICH_MEDIA))
      images <- zms.fold(Future.successful(-1))(_.messagesStorage.countSentByType(self, ASSET))
    } yield {

      val (groups, archived, muted) = if (self == UserId.Zero) (-1, -1, -1)
      else {
        convs.iterator.map { c =>
          ((c.convType == Group)?, c.archived?, c.muted?)
        }.foldLeft((0, 0, 0)) { case ((accG, accA, accM), (g, a, m)) =>
          (accG + g, accA + a, accM + m)
        }
      }

      val (contacts, autoConnected, blocked) = if (self == UserId.Zero) (-1, -1, -1)
      else {
        users.filter(u => !u.isWireBot && !u.isSelf).iterator.map { u =>
          ((u.isConnected && u.connection != Blocked)?, u.isAutoConnect?, (u.connection == Blocked)?)
        }.foldLeft((0, 0, 0)) { case ((accC, accA, accB), (c, a, b)) =>
          (accC + c, accA + a, accB + b)
        }
      }
    }
  }.logFailure(reportHockey = true)

  private def assetTrackingData(id: AssetId): Future[AssetTrackingData] = {
    for {
      zms         <- zMessaging.head
      Some(msg)   <- zms.messagesStorage.get(MessageId(id.str))
      Some(conv)  <- zms.convsContent.convById(msg.convId)
      Some(asset) <- zms.assetsStorage.get(id)
      withOtto    <- isOtto(conv, zms.usersStorage)
    } yield AssetTrackingData(conv.convType, withOtto, msg.isEphemeral, msg.ephemeral, asset.size, asset.mime)
  }

}

object GlobalTrackingController {

  private implicit class RichBoolean(val b: Boolean) extends AnyVal {
    def ? : Int = if (b) 1 else 0
  }

  def isOtto(conv: ConversationData, users: UsersStorage): Future[Boolean] =
    if (conv.convType == ConversationType.OneToOne) users.get(UserId(conv.id.str)).map(_.exists(_.isWireBot))(Threading.Background)
    else successful(false)

  case class AssetTrackingData(conversationType: ConversationType, withOtto: Boolean, isEphemeral: Boolean, expiration: EphemeralExpiration, assetSize: Long, mime:Mime)
}
