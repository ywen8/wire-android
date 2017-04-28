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
package com.waz.zclient.calling.controllers

import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog.{info, warn}
import com.waz.api.VoiceChannelState
import com.waz.model.ConvId
import com.waz.model.ConversationData.ConversationType
import com.waz.threading.Threading
import com.waz.zclient._
import com.waz.zclient.common.controllers.{CameraPermission, PermissionsController, RecordAudioPermission}

import scala.concurrent.Future

/**
  * This class is intended to be a relatively small controller that every PermissionsActivity can have access to in order
  * to start and accept calls. This controller requires a PermissionsActivity so that it can request and display the
  * related permissions dialogs, that's why it can't be in the GlobalCallController
  */
class CallPermissionsController(implicit inj: Injector, cxt: WireContext) extends Injectable {

  import Threading.Implicits.Background
  private implicit val eventContext = cxt.eventContext

  val globController = inject[GlobalCallingController]
  import globController._

  val permissionsController = inject[PermissionsController]

  private def useV3(convId: ConvId) = {
    isGroupCall(convId).flatMap {
      case true if !com.waz.zclient.BuildConfig.DEBUG => Future.successful(false) //Disable v3 group call from non debug builds
      case _ => {
        prefs.head.flatMap(p => p.uiPreferenceStringSignal(p.callingV3Key, "1").apply()).flatMap {
          case "0" => Future.successful(false) // v2
          case "1" => v3Service.flatMap(_.requestedCallVersion).head.map { v =>
            info(s"Relying on backend switch: using calling version: $v")
            v == 3
          }
          case "2" => Future.successful(true) // v3
          case _ =>
            warn("Unexpected calling v3 preference, defaulting to v2")
            Future.successful(false)
        }
      }
    }
  }

  private def isGroupCall(convId: ConvId) =
    for {
      z <- zms.head
      Some(conv) <- z.convsContent.convById(convId)
    } yield conv.convType == ConversationType.Group

  for {
    cId <- convId
    incomingCall <- callState.map {
      case VoiceChannelState.OTHER_CALLING => true
      case _ => false
    }
    autoAnswer <- prefs.flatMap(p => p.uiPreferenceBooleanSignal(p.autoAnswerCallPrefKey).signal)
  } if (incomingCall && autoAnswer) startCall(cId)

  /**
    * Determines if a call is available and either joins that call, or starts a new one. If there is
    * already an ongoing call, the withVideo and variableBitRate flags will be ignored by the
    * underlying service
    */
  def startCall(convId: ConvId, withVideo: Boolean = false, variableBitRate: Boolean = false): Unit = {
    def start(v3: Boolean) = v3 match {
      case true => v3Service.head.map(_.startCall(convId, withVideo))
      case _ => v2Service.head.map(_.joinVoiceChannel(convId, withVideo))
    }

    def reject(v3: Boolean) = v3 match {
      case true => v3Service.head.map(_.endCall(convId))
      case _ => v2Service.head.map(_.silenceVoiceChannel(convId))
    }

    for {
      incoming <- convIdOpt.head.map(_.isDefined)
      v3 <- if (incoming) isV3Call.head else useV3(convId)
    } {
      permissionsController.requiring(if (withVideo) Set(CameraPermission, RecordAudioPermission) else Set(RecordAudioPermission)) {
        if (!incoming) setVariableBitRateMode(variableBitRate)
        start(v3)
      }(R.string.calling__cannot_start__title, if (withVideo) R.string.calling__cannot_start__no_video_permission__message else R.string.calling__cannot_start__no_permission__message,
        if (incoming) reject(v3))
    }
  }

  def setVariableBitRateMode(enabled: Boolean): Unit = {
    val cbrOn = if(enabled) 0 else 1 // in SE it's reversed: we DISABLE cbr instead of enabling vbr and vice versa
    v3Service.head.map(_.setAudioConstantBitRateEnabled(cbrOn))
  }
}
