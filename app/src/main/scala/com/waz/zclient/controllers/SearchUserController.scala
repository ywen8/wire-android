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
package com.waz.zclient.controllers

import com.waz.api.{Contact, Contacts, UpdateListener}
import com.waz.model.SearchQuery.{Recommended, RecommendedHandle, TopPeople}
import com.waz.model._
import com.waz.service.{SearchKey, ZMessaging}
import com.waz.threading.Threading
import com.waz.utils.SeqMap
import com.waz.utils.events.{EventContext, EventStream, Signal}
import com.waz.zclient.{Injectable, Injector}
import com.waz.zclient.utils.SearchUtils

import scala.concurrent.duration._

case class SearchState(filter: String, hasSelectedUsers: Boolean, isAddingToConversation: Boolean){
  val shouldShowTopUsers = filter.isEmpty && !hasSelectedUsers
  val shouldShowAbContacts = !isAddingToConversation && !hasSelectedUsers
  val shouldShowGroupConversations = filter.nonEmpty && !hasSelectedUsers && !isAddingToConversation
  val shouldShowDirectorySerach = filter.nonEmpty && !hasSelectedUsers && !isAddingToConversation
  val shouldShowSearchedContacts = filter.nonEmpty
}

class SearchUserController(initialState: SearchState)(implicit injector: Injector) extends Injectable {
  implicit private val ec = EventContext.Implicits.global //TODO: change to implicit and get it from the fragment

  private val zms = inject[Signal[ZMessaging]]
  private val teamsAndUserController = inject[TeamsAndUserController]
  private val teamFilter = teamsAndUserController.currentTeamOrUser.map {
    case Left(_) => None
    case Right(team) => Some(team.id)
  }

  val searchState = Signal(initialState)
  val onDataChanged = EventStream[Unit]()

  def setFilter(filter: String): Unit = {
    searchState.mutate(_.copy(filter = filter))
  }

  def setHasSelectedUsers(hasSelectedUsers: Boolean): Unit = {
    searchState.mutate(_.copy(hasSelectedUsers = hasSelectedUsers))
  }

  def setState(filter: String, hasSelectedUsers: Boolean, isAddingToConversation: Boolean): Unit = {
    searchState ! SearchState(filter, hasSelectedUsers, isAddingToConversation)
  }

  private val topUsersSignal = for {
    z <- zms
    searchState <- searchState
    users <- if (searchState.shouldShowTopUsers) z.userSearch.searchUserData(TopPeople) else Signal(SeqMap.empty[UserId, UserData])
  } yield users.values

  private val teamMembersSignal = for {
    z <- zms
    teamId <- teamFilter
    searchState <- searchState
    members <- Signal(Seq[UserData]())
  } yield members //TODO: do

  private val conversationsSignal = for {
    z <- zms
    teamId <- teamFilter
    searchState <- searchState
      convs <- if (searchState.shouldShowGroupConversations) Signal.future(z.convsUi.findGroupConversations(SearchKey(searchState.filter), Int.MaxValue, handleOnly = false)) else Signal(Seq[ConversationData]())
  } yield convs.filter{ conv => teamId.forall(tId => conv.team.exists(_.id == tId)) }

  private val searchSignal = for {
    z <- zms
    teamId <- teamFilter
    searchState <- searchState
    users <- if (searchState.shouldShowDirectorySerach) z.userSearch.searchUserData(directorySearchQuery(searchState.filter)) else Signal(SeqMap.empty[UserId, UserData])
  } yield users.values

  private val connectedUsersSignal = for {
    z <- zms
    searchState <- searchState
    users <- if (searchState.shouldShowSearchedContacts) Signal(Vector[UserData]()) else
      z.users.acceptedOrBlockedUsers.map(_.valuesIterator.filter(SearchUtils.ConnectedUsersPredicate(searchState.filter, Set(), alsoSearchByEmail = true, showBlockedUsers = true, searchByHandleOnly = Handle.containsSymbol(searchState.filter))).toVector)
  } yield users

  //TODO: remove this old api....
  private val contactsSignal = Signal[Seq[Contact]]()
  var uiContacts: Option[Contacts] = None
  searchState.on(Threading.Ui) {
    case SearchState(filter ,_ ,_) =>
      uiContacts.foreach(_.search(filter))
  }
  private val contactsUpdateListener: UpdateListener = new UpdateListener() {
    def updated(): Unit = {
      uiContacts.foreach(contacts =>  contactsSignal ! (0 until contacts.size()).map(contacts.get))
    }
  }
  def setContacts(contacts: Contacts): Unit = {
    uiContacts.foreach(_.removeUpdateListener(contactsUpdateListener))
    uiContacts = Some(contacts)
    uiContacts.foreach(_.addUpdateListener(contactsUpdateListener))
    contactsUpdateListener.updated()
  }

  private val allDataSignal = for{
    topUsers         <- topUsersSignal.orElse(Signal(IndexedSeq()))
    teamMembers      <- teamMembersSignal.orElse(Signal(Seq()))
    conversations    <- conversationsSignal.orElse(Signal(Seq()))
    connectedUsers   <- connectedUsersSignal.orElse(Signal(Vector()))
    contacts         <- contactsSignal.orElse(Signal(Seq()))
    directoryResults <- searchSignal.orElse(Signal(IndexedSeq()))
  } yield (topUsers, teamMembers, conversations, connectedUsers, contacts, directoryResults)


  private def directorySearchQuery(str: String): SearchQuery = {
    if (Handle.containsSymbol(str))
      RecommendedHandle(Handle.stripSymbol(str))
    else
      Recommended(str)
  }

  var topUsers = Seq[UserData]()
  var teamMembers = Seq[UserData]()
  var conversations = Seq[ConversationData]()
  var connectedUsers = Seq[UserData]()
  var contacts = Seq[Contact]()
  var directoryResults = Seq[UserData]()

  allDataSignal.throttle(300.millis) {
    case (newTopUsers, newTeamMembers, newConversations, newConnectedUsers, newContacts, newDirectoryResults) =>
      topUsers = newTopUsers
      teamMembers = newTeamMembers
      conversations = newConversations
      connectedUsers = newConnectedUsers
      contacts = newContacts
      directoryResults = newDirectoryResults
      onDataChanged ! (())
  }
}
