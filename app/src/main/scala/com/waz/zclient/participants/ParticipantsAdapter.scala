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
package com.waz.zclient.participants

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.TextView
import com.waz.model.{UserData, UserId}
import com.waz.service.ZMessaging
import com.waz.utils.events.{EventContext, EventStream, Signal}
import com.waz.zclient.usersearch.views.{SearchResultUserRowView, ClickableUserRowViewHolder}
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.{Injectable, Injector, R}

class ParticipantsAdapter(numOfColumns: Int)(implicit context: Context, injector: Injector, eventContext: EventContext)
  extends RecyclerView.Adapter[ViewHolder] with Injectable {
  import ParticipantsAdapter._

  private lazy val zms = inject[Signal[ZMessaging]]
  private lazy val participantsController = inject[ParticipantsController]

  private var items = List.empty[Either[UserData, Int]]

  val onClick = EventStream[UserId]()

  private lazy val users = for {
    z       <- zms
    userIds <- participantsController.otherParticipants
    users   <- Signal.sequence(userIds.filterNot(_ == z.selfUserId).map(z.users.userSignal).toSeq: _*)
  } yield users

  private lazy val positions = users.map { users =>
    val (bots, people) = users.toList.partition(_.isWireBot)
    val (verified, unverified) = people.partition(_.isVerified)

    unverified.map(data => Left(data)) :::
      (if (unverified.nonEmpty && verified.nonEmpty) List(Right(SEPARATOR_VERIFIED))
       else Nil
      ) ::: verified.map(data => Left(data)) :::
      (if ((unverified.nonEmpty || verified.nonEmpty) && bots.nonEmpty) List(Right(SEPARATOR_BOTS))
       else Nil
      ) ::: bots.map(data => Left(data))
  }

  positions.onUi { list =>
    items = list
    notifyDataSetChanged()
  }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = viewType match {
    case CHATHEAD =>
      val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_user, parent, false)
      ClickableUserRowViewHolder(view.asInstanceOf[SearchResultUserRowView], onClick)
    case _ => new SeparatorViewHolder(getSeparatorView(parent))
  }

  override def onBindViewHolder(holder: ViewHolder, position: Int): Unit = (items(position), holder) match {
    case (Left(userId),   h: ClickableUserRowViewHolder)                           => h.setUser(userId)
    case (Right(sepType), h: SeparatorViewHolder) if sepType == SEPARATOR_VERIFIED => h.setTitle(R.string.pref_devices_device_verified)
    case (Right(sepType), h: SeparatorViewHolder) if sepType == SEPARATOR_BOTS     => h.setTitle(R.string.integrations_picker__section_title)
    case _ =>
  }

  def getSpanSize(position: Int): Int = getItemViewType(position) match {
    case SEPARATOR_VERIFIED | SEPARATOR_BOTS => numOfColumns
    case _                                   => 1
  }

  override def getItemCount: Int = items.size

  override def getItemId(position: Int): Long = items(position) match {
    case Left(user)   => user.id.hashCode()
    case Right(sepType) => sepType
  }

  setHasStableIds(true)

  override def getItemViewType(position: Int): Int = items(position) match {
    case Right(sepType) => sepType
    case _              => CHATHEAD
  }

  private def getSeparatorView(parent: ViewGroup): View =
    LayoutInflater.from(parent.getContext).inflate(R.layout.participants_separator_row, parent, false)

}

object ParticipantsAdapter {
  val CHATHEAD = 0
  val SEPARATOR_VERIFIED = 1
  val SEPARATOR_BOTS = 2

  class SeparatorViewHolder(separator: View) extends ViewHolder(separator) {
    def setTitle(title: Int): Unit =
      ViewUtils.getView[TextView](separator, R.id.separator_title).setText(title)
  }

}
