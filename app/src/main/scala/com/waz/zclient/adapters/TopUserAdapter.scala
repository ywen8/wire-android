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
  *//**
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
package com.waz.zclient.adapters

import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, ViewGroup}
import com.waz.model.{UserData, UserId}
import com.waz.zclient.R
import com.waz.zclient.viewholders.TopUserViewHolder
import com.waz.zclient.views.ChatheadWithTextFooter

object TopUserAdapter {

  trait Callback {
    def getSelectedUsers: Set[UserId]
  }

}

class TopUserAdapter(var callback: TopUserAdapter.Callback) extends RecyclerView.Adapter[TopUserViewHolder] {
  private var topUsers = Seq[UserData]()

  def onCreateViewHolder(parent: ViewGroup, viewType: Int): TopUserViewHolder = {
    val v = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_top_user, parent, false).asInstanceOf[ChatheadWithTextFooter]
    v.applyDarkTheme()
    new TopUserViewHolder(v)
  }

  def onBindViewHolder(holder: TopUserViewHolder, position: Int): Unit = {
    val user: UserData = topUsers(position)
    holder.bind(user)
    val selected: Boolean = callback.getSelectedUsers.contains(user.id)
    holder.setSelected(selected)
  }

  def getItemCount: Int = topUsers.length

  def setTopUsers(topUsers: Seq[UserData]): Unit = {
    this.topUsers = topUsers
    notifyDataSetChanged()
  }

  def reset(): Unit = {
    topUsers = Seq()
  }
}
