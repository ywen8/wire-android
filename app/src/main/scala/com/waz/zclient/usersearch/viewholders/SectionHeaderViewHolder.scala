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

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.waz.zclient.R
import com.waz.zclient.usersearch.adapters.PickUsersAdapter._
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.utils.ContextUtils._

class SectionHeaderViewHolder(val view: View) extends RecyclerView.ViewHolder(view) {
  private val sectionHeaderView: TextView = ViewUtils.getView(view, R.id.ttv_startui_section_header)
  private implicit val context = sectionHeaderView.getContext

  def bind(section: Int, teamName: String): Unit = {
    val title: String = section match {
      case TopUsersSection                                => getString(R.string.people_picker__top_users_header_title)
      case GroupConversationsSection if teamName.isEmpty  => getString(R.string.people_picker__search_result_conversations_header_title)
      case GroupConversationsSection                      => getString(R.string.people_picker__search_result_team_conversations_header_title, teamName)
      case ContactsSection                                => getString(R.string.people_picker__search_result_connections_header_title)
      case DirectorySection                               => getString(R.string.people_picker__search_result_others_header_title)
      case IntegrationsSection                            => getString(R.string.integrations_picker__section_title)
    }
    sectionHeaderView.setText(title)
  }
}
