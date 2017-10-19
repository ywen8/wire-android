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
package com.waz.zclient.messages

import java.util

import android.view.ViewGroup
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.model.ConversationData.ConversationType
import com.waz.model.{ConvId, Dim2, MessageData, MessageId}
import com.waz.service.ZMessaging
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.controllers.global.SelectionController
import com.waz.zclient.messages.MessageView.MsgBindOptions
import com.waz.zclient.messages.MessagesListView.UnreadIndex
import com.waz.zclient.messages.RecyclerCursor.RecyclerNotifier
import com.waz.zclient.{Injectable, Injector}

import scala.concurrent.Future

class MessagesListAdapter(listDim: Signal[Dim2])(implicit inj: Injector, ec: EventContext)
  extends MessagesListView.Adapter() with Injectable { adapter =>

  verbose("MessagesListAdapter created")

  private val zms = inject[Signal[ZMessaging]]
  private val selectedConversation = inject[SelectionController].selectedConv
  val ephemeralCount = Signal(Set.empty[MessageId])

  private val unreadIndex = Signal(UnreadIndex(0)).disableAutowiring()

  (for {
    zs <- zms
    convId <- selectedConversation
  } yield new RecyclerCursor(convId, zs, notifier)).onUi { c =>
    verbose(s"cursor changed: ${c.count}, conv: ${c.conv}")
    messages.foreach(_.close())
    messages = Some(c)
    notifier.notifyDataSetChanged()
  }

  private val convType = for {
    zs <- zms
    convId <- selectedConversation
    Some(conv) <- zs.convsStorage.optSignal(convId)
  } yield conv.convType

  private var lastSelfMessageId = Option.empty[MessageId]

  inject[MessagesController].lastSelfMessage.map(_.id).onUi { id =>
    lastSelfMessageId = Option(id)
  }

  private var messages = Option.empty[RecyclerCursor]

  override def getConvId: ConvId = selectedConversation.currentValue.orNull

  override def getUnreadIndex: UnreadIndex = unreadIndex.currentValue.getOrElse(UnreadIndex(0))

  override def getItemCount: Int = messages.fold(0)(_.count)

  override def getItemViewType(position: Int): Int = 0

  override def onBindViewHolder(holder: MessageViewHolder, pos: Int): Unit = {
    onBindViewHolder(holder, pos, new util.ArrayList[AnyRef])
  }

  override def onBindViewHolder(holder: MessageViewHolder, pos: Int, payloads: util.List[AnyRef]): Unit = messages.foreach { c =>
    val data = c(pos)

    val isLast = pos == c.count - 1
    val isSelf = zms.currentValue.exists(_.selfUserId == data.message.userId)
    val opts = MsgBindOptions(
      position = pos,
      isSelf = isSelf,
      isLast = isLast,
      isLastSelf = lastSelfMessageId.contains(data.message.id),
      pos > 0 && !isSelf && unreadIndex.currentValue.contains(pos),
      listDimensions = listDim.currentValue.getOrElse(Dim2(0, 0)),
      convType = convType.currentValue.getOrElse(ConversationType.Group)
    )

    val prev = if (pos == 0) None else Some(c(pos - 1).message)
    val next = if (isLast) None else Some(c(pos + 1).message)

    holder.bind(data, prev, next, opts)
    if (data.message.isEphemeral) ephemeralCount.mutate(_ + data.message.id)
  }

  override def onViewRecycled(holder: MessageViewHolder): Unit =
    if (holder.view.isEphemeral)
      holder.id.foreach(id => ephemeralCount.mutate(_ - id))


  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder =
    MessageViewHolder(MessageView(parent, viewType), adapter)

  def positionForMessage(messageData: MessageData): Future[Int] = messages.get.positionForMessage(messageData)

  lazy val notifier = new RecyclerNotifier {
    // view depends on two message entries,
    // most importantly, view needs to be refreshed if previous message was added or removed

    private def notifyChangedIfExists(position: Int) =
      if (position >= 0 && position < getItemCount)
        adapter.notifyItemChanged(position)

    override def notifyItemRangeInserted(index: Int, length: Int) = {
      adapter.notifyItemRangeInserted(index, length)
      notifyChangedIfExists(index + length + 1)
    }

    override def notifyItemRangeChanged(index: Int, length: Int) =
      adapter.notifyItemRangeChanged(index, length)

    override def notifyItemRangeRemoved(pos: Int, count: Int) = {
      adapter.notifyItemRangeRemoved(pos, count)
      notifyChangedIfExists(pos)
    }

    override def notifyDataSetChanged() = {
      unreadIndex ! UnreadIndex(messages.map(_.lastReadIndex + 1).getOrElse(0))
      adapter.notifyDataSetChanged()
    }
  }

}
