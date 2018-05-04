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
package com.waz.zclient.calling.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.GridLayout
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.api.VideoSendState
import com.waz.utils.events.{EventStream, Signal}
import com.waz.utils.returning
import com.waz.zclient.calling.controllers.CallController
import com.waz.zclient.calling.views.CallControlButtonView.ButtonColor
import com.waz.zclient.utils.RichView
import com.waz.zclient.{R, ViewHelper}

class ControlsView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends GridLayout(context, attrs, defStyleAttr) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  LayoutInflater.from(context).inflate(R.layout.calling__controls__grid, this, true)

  private lazy val controller = inject[CallController]

  val onClickEvent = EventStream[Unit]

  private val isVideoBeingSent = controller.videoSendState.map(_ != VideoSendState.DONT_SEND)

  private val incomingNotEstablished = Signal(controller.isCallIncoming, controller.isCallEstablished).map {
    case (in, est) => in && !est
  }

  private lazy val startedAsVideo = controller.isVideoCall.currentValue.getOrElse(false)

  // first row
  returning(findById[CallControlButtonView](R.id.mute_call)) { button =>
    button.set(R.string.glyph__microphone_off, R.string.incoming__controls__ongoing__mute, () => controller.toggleMuted())

    controller.isMuted.onUi(button.setButtonPressed)
  }

  returning(findById[CallControlButtonView](R.id.video_call)) { button =>
    button.set(R.string.glyph__video, R.string.incoming__controls__ongoing__video, () => controller.toggleVideo())

    isVideoBeingSent.onUi(button.setButtonPressed)

    Signal(controller.isCallIncoming, controller.isCallEstablished).map {
      case (in, est) => est || (startedAsVideo && in)
    }.onUi(button.setButtonActive)
  }

  returning(findById[CallControlButtonView](R.id.speaker_flip_call)) { button =>
    isVideoBeingSent.onUi {
      case true =>
        button.set(R.string.glyph__flip, R.string.incoming__controls__ongoing__flip, () => controller.currentCaptureDeviceIndex.mutate(_ + 1))
      case false =>
        button.set(R.string.glyph__speaker_loud, R.string.incoming__controls__ongoing__speaker, () => controller.speakerButton.press())
        controller.speakerButton.buttonState.onUi(button.setButtonPressed)
    }
  }

  // second row
  returning(findById[CallControlButtonView](R.id.reject_call)) { button =>
    button.set(R.string.glyph__end_call, R.string.empty_string, () => controller.leaveCall(), ButtonColor.Red)

    incomingNotEstablished.onUi(button.setVisible)
  }

  returning(findById[CallControlButtonView](R.id.end_call)) { button =>
    button.set(R.string.glyph__end_call, R.string.empty_string, () => controller.leaveCall(), ButtonColor.Red)

    incomingNotEstablished.map(!_).onUi(button.setVisible)
  }

  returning(findById[CallControlButtonView](R.id.accept_call)) { button =>
    button.set(R.string.glyph__call, R.string.empty_string, () => controller.continueDegradedCall(), ButtonColor.Green)

    incomingNotEstablished.onUi(button.setVisible)
  }

  this.onClick(onClickEvent ! Unit)
}
