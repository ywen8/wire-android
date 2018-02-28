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
package com.waz.zclient.usersearch.viewholders

import android.content.Context
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.View
import com.waz.model.UserData
import com.waz.zclient.R
import com.waz.zclient.usersearch.SearchResultOnItemTouchListener
import com.waz.zclient.usersearch.adapters.TopUserAdapter
import com.waz.zclient.utils.ViewUtils

class TopUsersViewHolder(view: View, topUserAdapter: TopUserAdapter, context: Context) extends RecyclerView.ViewHolder(view) {

  val topUsersRecyclerView = ViewUtils.getView[RecyclerView](view, R.id.rv_top_users)
  val layoutManager = new LinearLayoutManager(context)
  layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL)
  topUsersRecyclerView.setLayoutManager(layoutManager)
  topUsersRecyclerView.setHasFixedSize(false)
  topUsersRecyclerView.setAdapter(this.topUserAdapter)

  def bind(users: Seq[UserData]): Unit = {
    topUserAdapter.setTopUsers(users)
  }

  def bindOnItemTouchListener(onItemTouchListener: SearchResultOnItemTouchListener): Unit = {
    topUsersRecyclerView.addOnItemTouchListener(onItemTouchListener)
  }
}
