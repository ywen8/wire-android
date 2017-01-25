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
import com.waz.ZLog.{verbose, warn}
import com.waz.api.VoiceChannelState
import com.waz.model.ConvId
import com.waz.service.call.{CallingService, VoiceChannelService}
import com.waz.utils.events.Signal
import com.waz.zclient._
import com.waz.zclient.common.controllers.{CameraPermission, PermissionsController, RecordAudioPermission}

/**
  * This class is intended to be a relatively small controller that every PermissionsActivity can have access to in order
  * to start and accept calls. This controller requires a PermissionsActivity so that it can request and display the
  * related permissions dialogs, that's why it can't be in the GlobalCallController
  */
class CallPermissionsController(implicit inj: Injector, cxt: WireContext) extends Injectable {

  private implicit val eventContext = cxt.eventContext

  val globController = inject[GlobalCallingController]
  import globController._

  val permissionsController = inject[PermissionsController]

  val useV3 = prefs.flatMap(p => p.uiPreferenceStringSignal(p.callingV3Key).signal).flatMap {
    case "0" => Signal.const(false) // v2
    case "1" => v3Service.flatMap(_.requestedCallVersion).map { v =>
      verbose(s"Relying on backend switch: using calling version: $v")
      v == 3 // use BE switch
    }
    case "2" => Signal.const(true) // v3
    case _ =>
      warn("Unexpected calling v3 preference, defaulting to v2")
      Signal.const(false)
  }.disableAutowiring()

  (for {
    incomingCall <- callState.map {
      case VoiceChannelState.OTHER_CALLING => true
      case _ => false
    }
    autoAnswer <- prefs.flatMap(p => p.uiPreferenceBooleanSignal(p.autoAnswerCallPrefKey).signal)
  } yield (incomingCall, autoAnswer)) {
    case (true, true) => acceptCall()
    case _ => //
  }

  def startCall(convId: ConvId, withVideo: Boolean): Unit = {
    permissionsController.requiring(if (withVideo) Set(CameraPermission, RecordAudioPermission) else Set(RecordAudioPermission)) {
      if (useV3.currentValue.getOrElse(false))
        v3Service.currentValue.foreach(_.startCall(convId, withVideo))
      else
        v2Service.currentValue.foreach(_.joinVoiceChannel(convId, withVideo))
    }(R.string.calling__cannot_start__title,
      if (withVideo) R.string.calling__cannot_start__no_video_permission__message else R.string.calling__cannot_start__no_permission__message)
  }

  def acceptCall(): Unit = {
    val withVideo = videoCall.currentValue.getOrElse(false)
    val v3 = isV3Call.currentValue.getOrElse(false)

    def withV3(f: (CallingService, ConvId) => Unit) = v3ServiceAndCurrentConvId.currentValue.foreach { case (cs, id) => f(cs, id) }
    def withV2(f: (VoiceChannelService, ConvId) => Unit) = v2ServiceAndCurrentConvId.currentValue.foreach { case (vcs, id) => vcs.joinVoiceChannel(id, withVideo) }

    def acceptCall(): Unit = v3 match {
      case true => withV3 { (cs, id) => cs.acceptCall(id) }
      case _    => withV2 { (vcs, id) => vcs.joinVoiceChannel(id, withVideo) }
    }

    def rejectCall(): Unit = v3 match {
      case true => withV3 { (cs, id) => cs.endCall(id) }
      case _    => withV2 { (vcs, id) => vcs.silenceVoiceChannel(id) }
    }

    permissionsController.requiring(if (videoCall.currentValue.getOrElse(false)) Set(CameraPermission, RecordAudioPermission) else Set(RecordAudioPermission)) {
      acceptCall()
    } (R.string.calling__cannot_start__title, if (withVideo) R.string.calling__cannot_start__no_video_permission__message else R.string.calling__cannot_start__no_permission__message,
      rejectCall())
  }
}
