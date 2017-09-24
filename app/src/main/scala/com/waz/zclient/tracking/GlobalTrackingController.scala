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

import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.api.EphemeralExpiration
import com.waz.api.Message.Type._
import com.waz.content.{UserPreferences, UsersStorage}
import com.waz.model.ConversationData.ConversationType
import com.waz.model.UserData.ConnectionStatus.Blocked
import com.waz.model.{UserId, _}
import com.waz.service.{ZMessaging, ZmsLifeCycle}
import com.waz.threading.Threading
import com.waz.utils._
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient._
import com.waz.zclient.controllers.SignInController.SignInMethod
import com.waz.zclient.tracking.ContributionEvent.fromMime
import org.json.JSONObject

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.language.{implicitConversions, postfixOps}

class GlobalTrackingController(implicit inj: Injector, cxt: WireContext, eventContext: EventContext) extends Injectable {
  import GlobalTrackingController._
  import Threading.Implicits.Background

  private val mixpanel = MixpanelApiToken.map(MixpanelAPI.getInstance(cxt.getApplicationContext, _))
  verbose(s"For build: ${BuildConfig.APPLICATION_ID}, mixpanel: $mixpanel was created using token: ${BuildConfig.MIXPANEL_APP_TOKEN}")

  val zmsOpt      = inject[Signal[Option[ZMessaging]]]
  val zMessaging  = inject[Signal[ZMessaging]]
  val currentConv = inject[Signal[ConversationData]]

  val trackingEnabled = zMessaging.flatMap(_.userPrefs.preference(UserPreferences.AnalyticsEnabled).signal).disableAutowiring()

  inject[ZmsLifeCycle].uiActive.onChanged {
    case false =>
      mixpanel.foreach { m =>
        verbose("flushing mixpanel events")
        m.flush()
      }
    case _ =>
  }

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

  /**
    * Register tracking event listeners on SE services in this method. We need a method here, since whenever the signal
    * zms fires, we want to discard the previous reference to the subscriber. Not doing so will cause this class to keep
    * reference to old instances of the services under zms (?)
    */
  private def registerTrackingEventListeners(zms: ZMessaging) = {

    val loader      = zms.assetLoader
    val messages    = zms.messagesStorage
    val assets      = zms.assetsStorage
    val convsUI     = zms.convsUi
    val handlesSync = zms.handlesSync
    val connStats   = zms.websocket.connectionStats

    convsUI.assetUploadStarted.map(_.id) { assetTrackingData(_).map {
      case AssetTrackingData(convType, withOtto, exp, assetSize, m) =>
        trackEvent(zms, ContributionEvent(fromMime(m), convType, exp, withOtto))
    }}
  }

  /**
    * Sets super properties and actually performs the tracking of an event. Super properties are user scoped, so for that
    * reason, we need to ensure they're correctly set based on whatever account (zms) they were fired within.
    */
  def trackEvent(zms: ZMessaging, event: TrackingEvent): Unit = {
    def send() = {
      for {
        teamSize <- zms.teamId.fold(Future.successful(0))(_ => zms.teams.searchTeamMembers().head.map(_.size))
      } yield {
        val customSuperProperties = returning(new JSONObject()) { o =>
          o.put("team.is_member", zms.teamId.isDefined)
          o.put("team.size", teamSize) //TODO option of int for non-teams?
        }

        verbose(
          s"""
             |trackEvent: ${event.name}
             |properties: ${event.props.map(_.toString(2))}
             |superProps: ${customSuperProperties.toString(2)}
          """.stripMargin)

        mixpanel.foreach { m =>
          m.registerSuperProperties(customSuperProperties)
          m.track(event.name, event.props.orNull)
        }
      }
    }

    event match {
      case OptEvent(enabled) =>
        send() //always send opt events (isTrackingEnabled will be false when the user opts out)
        mixpanel.foreach { m =>
          if (enabled) {
            verbose("Opted in to analytics, re-registering")
            m.unregisterSuperProperty(MixpanelIgnoreProperty)
          }
          else {
            verbose("Opted out of analytics, flushing and de-registering")
            m.flush()
            m.registerSuperProperties(returning(new JSONObject()) { o =>
              o.put(MixpanelIgnoreProperty, true)
            })
          }
        }
      case _ =>
        trackingEnabled.head.map {
          case true => send()
          case _ => //no action
        }
    }
  }

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
    } yield AssetTrackingData(conv.convType, withOtto, msg.ephemeral, asset.size, asset.mime)
  }

  //Should wait until a ZMS instance exists before firing the event
  def onSignInSuccessful(method: SignInMethod): Unit = {
    for {
      acc <- ZMessaging.currentAccounts.activeAccount.collect { case Some(acc) => acc }.head
      zms <- ZMessaging.currentAccounts.activeZms.collect { case Some(zms) => zms }.head
    } yield {
      //TODO when are generic tokens still used?
      trackEvent(zms, SignInEvent(method, acc.invitationToken))
    }
  }

}

object GlobalTrackingController {

  private lazy val MixpanelIgnoreProperty = "$ignore"

  //For build flavours that don't have tracking enabled, this should be None
  private lazy val MixpanelApiToken = Option(BuildConfig.MIXPANEL_APP_TOKEN).filter(_.nonEmpty)

  private implicit class RichBoolean(val b: Boolean) extends AnyVal {
    def ? : Int = if (b) 1 else 0
  }

  def isOtto(conv: ConversationData, users: UsersStorage): Future[Boolean] =
    if (conv.convType == ConversationType.OneToOne) users.get(UserId(conv.id.str)).map(_.exists(_.isWireBot))(Threading.Background)
    else successful(false)

  case class AssetTrackingData(conversationType: ConversationType, withOtto: Boolean, expiration: EphemeralExpiration, assetSize: Long, mime:Mime)
}
