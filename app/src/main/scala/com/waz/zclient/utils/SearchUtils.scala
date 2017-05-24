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
package com.waz.zclient.utils

import com.waz.model.{TeamId, UserData}
import com.waz.model.UserData.ConnectionStatus
import com.waz.service.SearchKey

//TODO: this was removed from the UiModule API. Maybe it should be in the SE
object SearchUtils {

  def ConnectedUsersPredicate(searchTerm: String, filteredIds: Set[String], alsoSearchByEmail: Boolean, showBlockedUsers: Boolean, searchByHandleOnly: Boolean, teamId: Option[TeamId]): UserData => Boolean = {
    val query = SearchKey(searchTerm)
    user =>
      ((query.isAtTheStartOfAnyWordIn(user.searchKey) && !searchByHandleOnly) ||
        user.handle.exists(_.containsQuery(searchTerm)) ||
        (alsoSearchByEmail && user.email.exists(e => searchTerm.trim.equalsIgnoreCase(e.str)))) &&
        !filteredIds.contains(user.id.str) &&
        (showBlockedUsers || (user.connection != ConnectionStatus.Blocked))
      //TODO: team filter
      //&& teamId.forall(tid => user.teamId == tid)

  }
}
