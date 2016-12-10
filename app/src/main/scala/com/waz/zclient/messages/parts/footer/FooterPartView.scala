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
package com.waz.zclient.messages.parts.footer

import android.content.Context
import android.util.AttributeSet
import android.widget.{FrameLayout, TextView}
import com.waz.ZLog.ImplicitTag._
import com.waz.model.{MessageContent, MessageData}
import com.waz.service.messages.MessageAndLikes
import com.waz.threading.Threading
import com.waz.zclient.messages.MessageView.MsgBindOptions
import com.waz.zclient.messages.{MessageViewPart, MsgPart}
import com.waz.zclient.ui.utils.TextViewUtils
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils._
import com.waz.zclient.{R, ViewHelper}

//TODO tracking ?
class FooterPartView(context: Context, attrs: AttributeSet, style: Int) extends FrameLayout(context, attrs, style) with MessageViewPart with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  override val tpe: MsgPart = MsgPart.Footer

  inflate(R.layout.message_footer_content)

  val controller = new FooterViewController
  val message = controller.message
  val selection = controller.selection
  val isLiked = controller.isLiked
  val focused = controller.focused

  val height = getDimen(R.dimen.content__footer__height).toInt

  private val likeButton: LikeButton = findById(R.id.like__button)
  private val timeStampAndStatus: TextView = findById(R.id.timestamp_and_status)

  private val likeDetails: LikeDetailsView = findById(R.id.like_details)

  likeButton.init(controller)
  likeDetails.init(controller)

  controller.showLikeBtn.on(Threading.Ui) { likeButton.setVisible }

  controller.timestampText.zip(controller.linkColor).on(Threading.Ui) { case (string, color) =>
    timeStampAndStatus.setText(string)
    if (string.contains('_')) {
      TextViewUtils.linkifyText(timeStampAndStatus, color, false, controller.linkCallback)
    }
  }

  controller.showTimestamp.on(Threading.Ui) { st =>
    timeStampAndStatus.setVisible(st)  // TODO: translate animation
    likeDetails.setVisible(!st)
  }

  override def set(msg: MessageData, part: Option[MessageContent], opts: MsgBindOptions): Unit = ()

  override def set(msg: MessageAndLikes, part: Option[MessageContent], opts: MsgBindOptions): Unit = {
    controller.isSelfMessage.publish(opts.isSelf, Threading.Ui)
    controller.messageAndLikes.publish(msg, Threading.Ui)
  }
}
