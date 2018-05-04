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
package com.waz.zclient.calling.controllers

import android.media.AudioManager
import android.os.{PowerManager, Vibrator}
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.api.{IConversation, Verification, VideoSendState}
import com.waz.avs.{VideoPreview, VideoRenderer}
import com.waz.model.{UserData, UserId}
import com.waz.service.call.Avs.VideoReceiveState
import com.waz.service.call.CallInfo
import com.waz.service.call.CallInfo.CallState.{SelfJoining, _}
import com.waz.service.{AccountsService, GlobalModule, NetworkModeService, ZMessaging}
import com.waz.threading.{CancellableFuture, Threading}
import com.waz.utils._
import com.waz.utils.events.{ButtonSignal, ClockSignal, EventContext, Signal}
import com.waz.zclient.calling.CallingActivity
import com.waz.zclient.common.controllers.SoundController
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.{DeprecationUtils, LayoutSpec}
import com.waz.zclient.{Injectable, Injector, R, WireContext}
import org.threeten.bp.Duration
import org.threeten.bp.Duration.between
import org.threeten.bp.Instant.now

import scala.concurrent.duration._

class CallController(implicit inj: Injector, cxt: WireContext, eventContext: EventContext) extends Injectable {

  import Threading.Implicits.Background

  private val screenManager  = new ScreenManager
  val soundController        = inject[SoundController]
  val conversationController = inject[ConversationController]
  val networkMode            = inject[NetworkModeService].networkMode
  val accounts               = inject[AccountsService]

  //The zms of the account that's currently active (if any)
  val activeZmsOpt = inject[Signal[Option[ZMessaging]]]

  //the zms of the account that currently has an active call (if any)
  val callingZmsOpt =
    for {
      acc <- inject[GlobalModule].calling.activeAccount
      zms <- acc.fold(Signal.const(Option.empty[ZMessaging]))(id => Signal.future(ZMessaging.currentAccounts.getZms(id)))
    } yield zms
  val callingZms = callingZmsOpt.collect { case Some(z) => z }

  val currentCallOpt: Signal[Option[CallInfo]] = callingZmsOpt.flatMap {
    case Some(z) => z.calling.currentCall
    case _ => Signal.const(None)
  }
  val currentCall = currentCallOpt.collect { case Some(c) => c }

  val callConvIdOpt     = currentCallOpt.map(_.map(_.convId))
  val callConvId        = callConvIdOpt.collect { case Some(c) => c }

  val isCallActive      = currentCallOpt.map(_.isDefined)
  val isCallActiveDelay = isCallActive.flatMap {
    case true  => Signal.future(CancellableFuture.delay(300.millis).future.map(_ => true)).orElse(Signal.const(false))
    case false => Signal.const(false)
  }

  val callStateOpt      = currentCallOpt.map(_.flatMap(_.state))
  val callState         = callStateOpt.collect { case Some(s) => s }

  val isCallEstablished = callStateOpt.map(_.contains(SelfConnected))
  val isCallOutgoing    = callStateOpt.map(_.contains(SelfCalling))
  val isCallIncoming    = callStateOpt.map(_.contains(OtherCalling))

  val isMuted           = currentCall.map(_.muted)
  val isVideoCall       = currentCall.map{ c =>
    c.videoSendState != VideoSendState.DONT_SEND || c.videoReceiveState != VideoReceiveState.Stopped
  }
  val videoSendState    = currentCall.map(_.videoSendState)
  val videoReceiveState = currentCall.map(_.videoReceiveState)
  val isGroupCall       = currentCall.map(_.isGroup)
  val cbrEnabled        = currentCall.map(_.isCbrEnabled)

  val duration = currentCall.map(_.estabTime).flatMap {
    case Some(inst) => ClockSignal(Duration.ofSeconds(1).asScala).map(_ => Option(between(inst, now)))
    case None => Signal.const(Option.empty[Duration])
  }

  val durationFormatted = duration.map {
    case Some(d) =>
      val seconds = ((d.toMillis / 1000) % 60).toInt
      val minutes = ((d.toMillis / 1000) / 60).toInt
      f"$minutes%02d:$seconds%02d"
    case None => ""
  }

  val flowManager = callingZms.map(_.flowmanager)

  val captureDevices = flowManager.flatMap(fm => Signal.future(fm.getVideoCaptureDevices))

  //TODO when I have a proper field for front camera, make sure it's always set as the first one
  val currentCaptureDeviceIndex = Signal(0)

  val currentCaptureDevice = captureDevices.zip(currentCaptureDeviceIndex).map {
    case (devices, devIndex) if devices.nonEmpty => Some(devices(devIndex % devices.size))
    case _ => None
  }

  (for {
    fm     <- flowManager
    conv   <- conversation
    device <- currentCaptureDevice
  } yield (fm, conv, device)) {
    case (fm, conv, Some(currentDevice)) => fm.setVideoCaptureDevice(conv.remoteId, currentDevice.id)
    case _ =>
  }

  val cameraFailed = flowManager.flatMap(_.cameraFailedSig)

  val userStorage = callingZms map (_.usersStorage)
  val prefs       = callingZms.map(_.prefs)

  val callingService = callingZms.map(_.calling).disableAutowiring()

  val callingServiceAndCurrentConvId =
    for {
      cs <- callingService
      c  <- callConvId
    } yield (cs, c)


  val conversation = callingZms.zip(callConvId) flatMap { case (z, cId) => z.convsStorage.signal(cId) }
  val conversationName = conversation map (data => if (data.convType == IConversation.Type.GROUP) data.name.filter(!_.isEmpty).getOrElse(data.generatedName) else data.generatedName)

  val otherUser = Signal(isGroupCall, userStorage, callConvId).flatMap {
    case (isGroupCall, usersStorage, convId) if !isGroupCall =>
      usersStorage.optSignal(UserId(convId.str)) // one-to-one conversation has the same id as the other user, so we can access it directly
    case _ => Signal.const[Option[UserData]](None) //Need a none signal to help with further signals
  }

  def leaveCall(): Unit = {
    verbose(s"leaveCall")
    for {
      cId <- callConvId.head
      cs  <- callingService.head
    } yield cs.endCall(cId)
  }

  def toggleMuted(): Unit = {
    verbose(s"toggleMuted")
    for {
      muted <- isMuted.head
      cs    <- callingService.head
    } yield cs.setCallMuted(!muted)
  }

  def toggleVideo(): Unit = {
    verbose(s"toggleVideo")
    for {
      st  <- videoSendState.head
      cId <- callConvId.head
      cs  <- callingService.head
    } yield cs.setVideoSendActive(cId, if(st == VideoSendState.SEND) false else true)
  }

  private var _wasUiActiveOnCallStart = false

  def wasUiActiveOnCallStart = _wasUiActiveOnCallStart

  val onCallStarted = isCallActive.onChanged.filter(_ == true).map { _ =>
    val active = ZMessaging.currentGlobal.lifecycle.uiActive.currentValue.getOrElse(false)
    _wasUiActiveOnCallStart = active
    active
  }

  onCallStarted.on(Threading.Ui) { _ =>
    CallingActivity.start(cxt)
  }(EventContext.Global)

  isCallEstablished.onChanged.filter(_ == true) { _ =>
    soundController.playCallEstablishedSound()
  }

  isCallActive.onChanged.filter(_ == false) { _ =>
    soundController.playCallEndedSound()
  }

  isCallActive.onChanged.filter(_ == false).on(Threading.Ui) { _ =>
    screenManager.releaseWakeLock()
  }(EventContext.Global)

  (for {
    v <- isVideoCall
    st <- callStateOpt
  } yield (v, st)) {
    case (true, _) => screenManager.setStayAwake()
    case (false, Some(OtherCalling)) => screenManager.setStayAwake()
    case (false, Some(SelfCalling | SelfJoining | SelfConnected)) => screenManager.setProximitySensorEnabled()
    case _ => screenManager.releaseWakeLock()
  }

  (for {
    m <- isMuted
    i <- isCallIncoming
  } yield (m, i)) { case (m, i) =>
    soundController.setIncomingRingTonePlaying(!m && i)
  }

  val convDegraded = conversation.map(_.verified == Verification.UNVERIFIED)
    .orElse(Signal(false))
    .disableAutowiring()

  val degradationWarningText = convDegraded.flatMap {
    case false => Signal("")
    case true =>
      (for {
        zms <- callingZms
        convId <- callConvId
      } yield {
        zms.membersStorage.activeMembers(convId).flatMap { ids =>
          zms.usersStorage.listSignal(ids)
        }.map(_.filter(_.verified != Verification.VERIFIED).toList)
      }).flatten.map {
        case u1 :: u2 :: _ =>
          //TODO handle more than 2 users
          getString(R.string.conversation__degraded_confirmation__header__multiple_user, u1.name, u2.name)
        case List(u) =>
          //TODO handle string for case where user adds multiple clients
          getQuantityString(R.plurals.conversation__degraded_confirmation__header__single_user, 1, u.name)
        case _ => ""
      }
  }

  val degradationConfirmationText = convDegraded.flatMap {
    case false => Signal("")
    case true => isCallOutgoing.map {
      case true  => R.string.conversation__degraded_confirmation__place_call
      case false => R.string.conversation__degraded_confirmation__accept_call
    }.map(getString)
  }

  (for {
    v <- isVideoCall
    o <- isCallOutgoing
    d <- convDegraded
  } yield (v, o & !d)) { case (v, play) =>
    soundController.setOutgoingRingTonePlaying(play, v)
  }

  //Use Audio view to show conversation degraded screen for calling
  val showVideoView = convDegraded.flatMap {
    case true  => Signal(false)
    case false => isVideoCall
  }.disableAutowiring()

  val selfUser = callingZms flatMap (_.users.selfUser)

  val callerId = currentCallOpt flatMap {
    case Some(info) =>
      (info.others, info.state) match {
        case (_, Some(SelfCalling)) => selfUser.map(_.id)
        case (others, Some(OtherCalling)) if others.size == 1 => Signal.const(others.head)
        case _ => Signal.empty[UserId] //TODO Dean do I need this information for other call states?
      }
    case _ => Signal.empty[UserId]
  }

  val callerData = userStorage.zip(callerId).flatMap { case (storage, id) => storage.signal(id) }

  /////////////////////////////////////////////////////////////////////////////////
  /// TODO A lot of the following code should probably be moved to some other UI controller for the views that use them
  /////////////////////////////////////////////////////////////////////////////////

  val flowId = for {
    zms    <- callingZms
    convId <- callConvId
    conv   <- zms.convsStorage.signal(convId)
    rConvId = conv.remoteId
    userData <- otherUser
  } yield (rConvId, userData.map(_.id))

  def setVideoPreview(view: Option[VideoPreview]): Unit = {
    flowManager.on(Threading.Ui) { fm =>
      verbose(s"Setting VideoPreview on Flowmanager, view: $view")
      fm.setVideoPreview(view.orNull)
    }
  }

  def setVideoView(view: Option[VideoRenderer]): Unit = {
    (for {
      fm <- flowManager
      (rConvId, userId) <- flowId
    } yield (fm, rConvId, userId)).on(Threading.Ui) {
      case (fm, rConvId, userId) =>
        verbose(s"Setting ViewRenderer on Flowmanager, rConvId: $rConvId, userId: $userId, view: $view")
        view.foreach(fm.setVideoView(rConvId, userId, _))
    }
  }

  val callBannerText = Signal(isVideoCall, callState).map {
    case (_,     SelfCalling)   => R.string.call_banner_outgoing
    case (true,  OtherCalling)  => R.string.call_banner_incoming_video
    case (false, OtherCalling)  => R.string.call_banner_incoming
    case (_,     SelfJoining)   => R.string.call_banner_joining
    case (_,     SelfConnected) => R.string.call_banner_tap_to_return_to_call
    case _                      => R.string.empty_string
  }

  val subtitleText: Signal[String] = convDegraded.flatMap {
    case true => Signal("")
    case false => (for {
      video <- isVideoCall
      state <- callState
      dur   <- durationFormatted
    } yield (video, state, dur)).map {
      case (true,  SelfCalling,  _)  => cxt.getString(R.string.calling__header__outgoing_video_subtitle)
      case (false, SelfCalling,  _)  => cxt.getString(R.string.calling__header__outgoing_subtitle)
      case (true,  OtherCalling, _)  => cxt.getString(R.string.calling__header__incoming_subtitle__video)
      case (false, OtherCalling, _)  => cxt.getString(R.string.calling__header__incoming_subtitle)
      case (_,     SelfJoining,  _)  => cxt.getString(R.string.calling__header__joining)
      case (false, SelfConnected, d) => d
      case _ => ""
    }
  }

  val stateMessageText = Signal(callState, cameraFailed, videoReceiveState, conversationName).map { vs =>
    verbose(s"$vs")
    import VideoReceiveState._
    vs match {
      case (SelfCalling,   true, _,             _)             => Option(cxt.getString(R.string.calling__self_preview_unavailable_long))
      case (SelfJoining,   _,    _,             _)             => Option(cxt.getString(R.string.ongoing__connecting))
      case (SelfConnected, _,    BadConnection, _)             => Option(cxt.getString(R.string.ongoing__poor_connection_message))
      case (SelfConnected, _,    Stopped,       otherUserName) => Option(cxt.getString(R.string.ongoing__other_turned_off_video, otherUserName))
      case (SelfConnected, _,    Unknown,       otherUserName) => Option(cxt.getString(R.string.ongoing__other_unable_to_send_video, otherUserName))
      case _ => None
    }
  }

  val showOngoingControls = convDegraded.flatMap {
    case true => Signal(true)
    case false => callState.map {
      case OtherCalling => false
      case _ => true
    }
  }.orElse(Signal(true)) //ensure that controls are ALWAYS visible in case something goes wrong...

  val participantIdsToDisplay = currentCall.map(_.others.toVector)

  def continueDegradedCall(): Unit = callingServiceAndCurrentConvId.head.map {
    case (cs, _) => cs.continueDegradedCall()
  }

  def vibrate(): Unit = {
    import com.waz.zclient.utils.ContextUtils._
    val audioManager = Option(inject[AudioManager])
    val vibrator = Option(inject[Vibrator])

    val disableRepeat = -1
    (audioManager, vibrator) match {
      case (Some(am), Some(vib)) if am.getRingerMode != AudioManager.RINGER_MODE_SILENT =>
        DeprecationUtils.vibrate(vib, getIntArray(R.array.call_control_enter).map(_.toLong), disableRepeat)
      case _ =>
    }
  }

  val speakerButton = ButtonSignal(callingZms.map(_.mediamanager), callingZms.flatMap(_.mediamanager.isSpeakerOn)) {
    case (mm, isSpeakerSet) => mm.setSpeaker(!isSpeakerSet)
  }

  val isTablet = Signal(!LayoutSpec.isPhone(cxt))
}

private class ScreenManager(implicit injector: Injector) extends Injectable {

  private val TAG = "CALLING_WAKE_LOCK"

  private val powerManager = Option(inject[PowerManager])

  private var stayAwake = false
  private var wakeLock: Option[PowerManager#WakeLock] = None

  def setStayAwake() = {
    (stayAwake, wakeLock) match {
      case (_, None) | (false, Some(_)) =>
        this.stayAwake = true
        createWakeLock();
      case _ => //already set
    }
  }

  def setProximitySensorEnabled() = {
    (stayAwake, wakeLock) match {
      case (_, None) | (true, Some(_)) =>
        this.stayAwake = false
        createWakeLock();
      case _ => //already set
    }
  }

  private def createWakeLock() = {
    val flags = if (stayAwake)
      DeprecationUtils.WAKE_LOCK_OPTIONS
    else PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
    releaseWakeLock()
    wakeLock = powerManager.map(_.newWakeLock(flags, TAG))
    verbose(s"Creating wakelock")
    wakeLock.foreach(_.acquire())
    verbose(s"Acquiring wakelock")
  }

  def releaseWakeLock() = {
    for (wl <- wakeLock if wl.isHeld) {
      wl.release()
      verbose(s"Releasing wakelock")
    }
    wakeLock = None
  }
}

