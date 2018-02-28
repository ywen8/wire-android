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
import android.widget.{ImageView, TextView}
import com.waz.model.{UserData, UserId}
import com.waz.service.ZMessaging
import com.waz.utils.events.{EventContext, EventStream, Signal, SourceStream}
import com.waz.zclient.common.views.SingleUserRowView
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.paintcode.{ForwardNavigationIcon, GuestIcon}
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.{RichView, ViewUtils}
import com.waz.zclient.{Injectable, Injector, R}

class ParticipantsAdapter(numOfColumns: Int)(implicit context: Context, injector: Injector, eventContext: EventContext)
  extends RecyclerView.Adapter[ViewHolder] with Injectable {
  import ParticipantsAdapter._

  private lazy val zms = inject[Signal[ZMessaging]]
  private lazy val participantsController = inject[ParticipantsController]

  private var items = List.empty[Either[ParticipantData, Int]]

  val onClick = EventStream[UserId]()
  val onGuestOptionsClick = EventStream[Unit]()

  lazy val users = for {
    z       <- zms
    userIds <- participantsController.otherParticipants.map(_.toSeq)
    users   <- Signal.sequence(userIds.filterNot(_ == z.selfUserId).map(z.users.userSignal): _*)
  } yield users.map(u => ParticipantData(u, u.isGuest(z.teamId) && !u.isWireBot)).sortBy(_.userData.getDisplayName)

  private val shouldShowGuestButton = inject[ConversationController].currentConv.map(_.accessRole.isDefined)

  private lazy val positions = for {
    users <- users
    isTeam <- participantsController.currentUserBelongsToConversationTeam
    guestButton <- shouldShowGuestButton
  } yield {
    val (bots, people) = users.toList.partition(_.userData.isWireBot)

    (if (isTeam && guestButton) List(Right(GuestOptions))
      else Nil
      ) :::
    (if (people.nonEmpty) List(Right(PeopleSeparator))
      else Nil
      ) ::: people.map(data => Left(data)) :::
    (if (bots.nonEmpty) List(Right(BotsSeparator))
      else Nil
        ) ::: bots.map(data => Left(data))
  }

  positions.onUi { list =>
    items = list
    notifyDataSetChanged()
  }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = viewType match {
    case GuestOptions =>
      val view = LayoutInflater.from(parent.getContext).inflate(R.layout.guest_options_button, parent, false)
      view.onClick(onGuestOptionsClick ! {})
      GuestOptionsButtonViewHolder(view)
    case UserRow =>
      val view = LayoutInflater.from(parent.getContext).inflate(R.layout.single_user_row, parent, false).asInstanceOf[SingleUserRowView]
      view.showArrow(true)
      ParticipantRowViewHolder(view, onClick)
    case _ => SeparatorViewHolder(getSeparatorView(parent))
  }

  override def onBindViewHolder(holder: ViewHolder, position: Int): Unit = (items(position), holder) match {
    case (Left(userData), h: ParticipantRowViewHolder) => h.bind(userData)

    case (Right(sepType), h: SeparatorViewHolder) if Set(PeopleSeparator, BotsSeparator).contains(sepType) =>
      val count = items.count {
        case Left(a)
          if sepType == PeopleSeparator && !a.userData.isWireBot ||
             sepType == BotsSeparator && a.userData.isWireBot => true
        case _ => false
      }.toString
      h.setTitle(getString(if (sepType == PeopleSeparator) R.string.participants_divider_people else R.string.participants_divider_services, count))
      h.setId(if (sepType == PeopleSeparator) R.id.participants_section else R.id.services_section)

    case _ =>
  }

  def getSpanSize(position: Int): Int = getItemViewType(position) match {
    case PeopleSeparator | BotsSeparator => numOfColumns
    case _                                   => 1
  }

  override def getItemCount: Int = items.size

  override def getItemId(position: Int): Long = items(position) match {
    case Left(user)   => user.userData.id.hashCode()
    case Right(sepType) => sepType
  }

  setHasStableIds(true)

  override def getItemViewType(position: Int): Int = items(position) match {
    case Right(sepType) => sepType
    case _              => UserRow
  }

  private def getSeparatorView(parent: ViewGroup): View =
    LayoutInflater.from(parent.getContext).inflate(R.layout.participants_separator_row, parent, false)

}

object ParticipantsAdapter {
  val UserRow = 0
  val PeopleSeparator = 1
  val BotsSeparator = 2
  val GuestOptions = 3

  case class ParticipantData(userData: UserData, isGuest: Boolean)

  case class GuestOptionsButtonViewHolder(view: View) extends ViewHolder(view) {
    private implicit val ctx = view.getContext
    view.setId(R.id.guest_options)
    view.findViewById[ImageView](R.id.icon).setImageDrawable(GuestIcon(R.color.graphite))
    view.findViewById[ImageView](R.id.next_indicator).setImageDrawable(ForwardNavigationIcon(R.color.light_graphite))
  }

  case class SeparatorViewHolder(separator: View) extends ViewHolder(separator) {
    private val textView = ViewUtils.getView[TextView](separator, R.id.separator_title)

    def setTitle(title: String) = textView.setText(title)
    def setId(id: Int) = textView.setId(id)
  }

  case class ParticipantRowViewHolder(view: SingleUserRowView, onClick: SourceStream[UserId]) extends ViewHolder(view) {

    private var userId = Option.empty[UserId]

    view.onClick(userId.foreach(onClick ! _))

    def bind(participant: ParticipantData): Unit = {
      userId = Some(participant.userData.id)
      view.setUserData(participant.userData)
      view.setIsGuest(participant.isGuest)
    }
  }

}
