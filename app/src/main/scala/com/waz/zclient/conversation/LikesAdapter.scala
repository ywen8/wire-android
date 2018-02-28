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
package com.waz.zclient.conversation

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, ViewGroup}
import com.waz.api.User
import com.waz.model.{UserData, UserId}
import com.waz.utils.events.Signal
import com.waz.zclient.common.views.SingleUserRowView
import com.waz.zclient.conversation.LikesAdapter.ViewHolder
import com.waz.zclient.utils.{UiStorage, UserSetSignal}
import com.waz.zclient.{BaseActivity, R}

class LikesAdapter(context: Context) extends RecyclerView.Adapter[RecyclerView.ViewHolder] {
  private implicit val injector = context.asInstanceOf[BaseActivity].injector
  private implicit val ec = context.asInstanceOf[BaseActivity].eventContext
  private implicit val uiStorage = context.asInstanceOf[BaseActivity].inject[UiStorage]
  private val likesUserIds = Signal(Set[UserId]())
  private var likesUsers = Seq[UserData]()

  (for {
    userIds <- likesUserIds
    users   <- UserSetSignal(userIds)
  } yield users.toSeq).onUi { data =>
    likesUsers = data
    notifyDataSetChanged()
  }

  def onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    new ViewHolder(LayoutInflater.from(parent.getContext).inflate(R.layout.single_user_row, parent, false).asInstanceOf[SingleUserRowView])

  def onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int): Unit =
    holder.asInstanceOf[ViewHolder].bind(likesUsers(position))

  def getItemCount: Int = likesUsers.size

  def setLikes(likes: Array[User]): Unit = likesUserIds ! likes.map(u => UserId(u.getId)).toSet
}

object LikesAdapter {
  class ViewHolder(view: SingleUserRowView) extends RecyclerView.ViewHolder(view) {

    private var userData: Option[UserData] = None
    view.setTheme(SingleUserRowView.Transparent)

    def bind(userData: UserData): Unit = {
      this.userData = Some(userData)
      view.setUserData(userData)
    }
  }
}
