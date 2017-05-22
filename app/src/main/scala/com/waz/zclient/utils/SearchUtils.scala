package com.waz.zclient.utils

import com.waz.model.UserData
import com.waz.model.UserData.ConnectionStatus
import com.waz.service.SearchKey

//TODO: this was removed from the UiModule API. Maybe it should be in the SE
object SearchUtils {

  def ConnectedUsersPredicate(searchTerm: String, limit: Int, filteredIds: Set[String], alsoSearchByEmail: Boolean, showBlockedUsers: Boolean, searchByHandleOnly: Boolean): UserData => Boolean = {
    val query = SearchKey(searchTerm)
    user =>
      ((query.isAtTheStartOfAnyWordIn(user.searchKey) && !searchByHandleOnly) ||
        user.handle.exists(_.containsQuery(searchTerm)) ||
        (alsoSearchByEmail && user.email.exists(e => searchTerm.trim.equalsIgnoreCase(e.str)))) &&
        !filteredIds.contains(user.id.str) &&
        (showBlockedUsers || (user.connection != ConnectionStatus.Blocked))
  }
}
