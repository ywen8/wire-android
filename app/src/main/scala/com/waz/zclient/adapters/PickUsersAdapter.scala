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
package com.waz.zclient.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.ZLog
import com.waz.api.{Contact, ContactDetails, Contacts, User}
import com.waz.model.SearchQuery.{Recommended, RecommendedHandle, TopPeople}
import com.waz.model._
import com.waz.service.{SearchKey, ZMessaging}
import com.waz.threading.Threading
import com.waz.utils.SeqMap
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient._
import com.waz.zclient.adapters.PickUsersAdapter._
import com.waz.zclient.controllers.TeamsAndUserController
import com.waz.zclient.pages.main.pickuser.SearchResultAdapter
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import com.waz.zclient.utils.SearchUtils
import com.waz.zclient.viewholders._
import com.waz.zclient.views.pickuser.ContactRowView.Callback

import scala.collection.JavaConverters._

class PickUsersAdapter(context: Context, topUsersOnItemTouchListener: SearchResultOnItemTouchListener, adapterCallback: SearchResultAdapter.Callback)
                      (implicit injector: Injector) extends RecyclerView.Adapter[RecyclerView.ViewHolder] with Injectable {

  implicit private val ec = EventContext.Implicits.global
  implicit private val logTag = ZLog.logTagFor[PickUsersAdapter]
  implicit private val ctx = context

  setHasStableIds(true)

  val teamsAndUserController = inject[TeamsAndUserController]
  val teamFilter = teamsAndUserController.currentTeamOrUser.map {
    case Left(_) => None
    case Right(team) => Some(team.id)
  }
  val filter = Signal[String]("")
  //TODO: for java
  def setFiler(string: String): Unit = {
    filter ! string
  }

  val zms = inject[Signal[ZMessaging]]

  private val topUsersSignal = for {
    z <- zms
    filterStr <- filter
    users <- if (filterStr.isEmpty && selectedUsers.isEmpty) z.userSearch.searchUserData(TopPeople) else Signal(SeqMap.empty[UserId, UserData])
  } yield users.values

  private val teamMembersSignal = for {
    z <- zms
    teamId <- teamFilter
    members <- Signal(Seq[UserData]())
  } yield members //TODO: do

  private val conversationsSignal = for {
    z <- zms
    teamId <- teamFilter
    filterStr <- filter
    convs <- if (filterStr.nonEmpty && selectedUsers.isEmpty) Signal.future(z.convsUi.findGroupConversations(SearchKey(filterStr), Int.MaxValue, handleOnly = false)) else Signal(Seq[ConversationData]())
  } yield convs.filter{ conv => teamId.forall(tId => conv.team.exists(_.id == tId)) }

  private val searchSignal = for {
    z <- zms
    teamId <- teamFilter
    filterStr <- filter
    users <- if (filterStr.nonEmpty && selectedUsers.isEmpty) z.userSearch.searchUserData(directorySearchQuery(filterStr)) else Signal(SeqMap.empty[UserId, UserData])
  } yield users.values

  private val searchedContacts = for {
    z <- zms
    filterStr <- filter
    users <- if (filterStr.isEmpty && selectedUsers.isEmpty) Signal(Vector[UserData]()) else
      z.users.acceptedOrBlockedUsers.map(_.valuesIterator.filter(SearchUtils.ConnectedUsersPredicate(filterStr, Set(), alsoSearchByEmail = true, showBlockedUsers = true, searchByHandleOnly = Handle.containsSymbol(filterStr))).toVector)
  } yield users

  //TODO: this logic shouldn't be here
  private def directorySearchQuery(str: String): SearchQuery = {
    if (Handle.containsSymbol(str))
      RecommendedHandle(Handle.stripSymbol(str))
    else
      Recommended(str)
  }

  private val contactsSignal = Signal[Seq[Contact]]()

  private var topUsers = Seq[UserData]()
  private var teamMembers = Seq[UserData]()
  private var conversations = Seq[ConversationData]()
  private var connectedUsers = Seq[UserData]()
  private var contacts = Seq[Contact]()
  private var directoryResults = Seq[UserData]()

  private var mergedResult = Seq[SearchResult]()
  private var collapsedContacts = true
  private var collapsedGroups = true

  def selectedUsers = adapterCallback.getSelectedUsers.asScala.map(u => UserId(u.getId)).toSet

  private val contactsCallback = new Callback {
    override def onContactListContactClicked(contactDetails: ContactDetails) = adapterCallback.onContactListContactClicked(contactDetails)
    override def getDestination = IPickUserController.STARTUI
    override def isUserSelected(user: User) = selectedUsers.contains(UserId(user.getId))
    override def onContactListUserClicked(user: User) = adapterCallback.onContactListUserClicked(user)
  }

  topUsersSignal.on(Threading.Ui) { data =>
    topUsers = data
    updateMergedResults()
  }
  teamMembersSignal.on(Threading.Ui) { data =>
    teamMembers = data
    updateMergedResults()
  }
  conversationsSignal.on(Threading.Ui) { data =>
    conversations = data
    updateMergedResults()
  }
  contactsSignal.on(Threading.Ui) { data =>
    contacts = data
    updateMergedResults()
  }
  searchedContacts.on(Threading.Ui) { data =>
    connectedUsers = data
    updateMergedResults()
  }
  searchSignal.on(Threading.Ui) { data =>
    directoryResults = data.filter(_.connection == UserData.ConnectionStatus.Unconnected)
    updateMergedResults()
  }

  private def updateMergedResults(): Unit ={
    mergedResult = Seq()

    //TOP PEOPLE
    if (topUsers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TopUsersSection, 0))
      mergedResult = mergedResult ++ Seq(SearchResult(TopUsers, TopUsersSection, 0))
    }

    //TEAM MEMBERS
    if (teamMembers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TeamMembersSection, 0))
      mergedResult = mergedResult ++ teamMembers.indices.map { i =>
        SearchResult(ConnectedUser, TeamMembersSection, i, teamMembers(i).id.str.hashCode)
      }
    }

    //CONTACTS
    if (contacts.nonEmpty || connectedUsers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, ContactsSection, 0))
      var contactsSection = Seq[SearchResult]()

      contactsSection = contactsSection ++ contacts.indices.map { i =>
        SearchResult(AddressBookContact, ContactsSection, i)
      }

      contactsSection = contactsSection ++ connectedUsers.indices.map { i =>
        SearchResult(ConnectedUser, ContactsSection, i, connectedUsers(i).id.str.hashCode)
      }

      val shouldCollapse = filter.currentValue.exists(_.nonEmpty) && collapsedContacts && contactsSection.size > CollapsedContacts

      contactsSection = contactsSection.sortBy(_.name).take(if (shouldCollapse) CollapsedContacts else contactsSection.size)

      mergedResult = mergedResult ++ contactsSection
      if (shouldCollapse) {
        mergedResult = mergedResult ++ Seq(SearchResult(Expand, ContactsSection, 0))
      }
    }

    //GROUP CONVERSATIONS
    if (conversations.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, GroupConversationsSection, 0))

      val shouldCollapse = collapsedGroups && conversations.size > CollapsedGroups

      mergedResult = mergedResult ++ conversations.indices.map { i =>
        SearchResult(GroupConversation, GroupConversationsSection, i, conversations(i).id.str.hashCode)
      }.take(if (shouldCollapse) CollapsedGroups else conversations.size)
      if (shouldCollapse) {
        mergedResult = mergedResult ++ Seq(SearchResult(Expand, GroupConversationsSection, 0))
      }
    }

    //CONNECT
    if (directoryResults.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, DirectorySection, 0))
      mergedResult = mergedResult ++ directoryResults.indices.map { i =>
        SearchResult(UnconnectedUser, DirectorySection, i, directoryResults(i).id.str.hashCode)
      }
    }

    ZLog.debug(s"Merged contacts updated: ${mergedResult.size}")
    notifyDataSetChanged()
  }

  override def getItemCount = mergedResult.size

  override def onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = {
    val item = mergedResult(position)
    item.itemType match {
      case TopUsers =>
        holder.asInstanceOf[TopUsersViewHolder].bind(topUsers)
        holder.asInstanceOf[TopUsersViewHolder].bindOnItemTouchListener(topUsersOnItemTouchListener)
      case GroupConversation =>
        val conversation = conversations(item.index)
        holder.asInstanceOf[ConversationViewHolder].bind(conversation)
      case ConnectedUser =>
        val connectedUser = connectedUsers(item.index)
        val contactIsSelected = selectedUsers.contains(connectedUser.id)
        holder.asInstanceOf[UserViewHolder].bind(connectedUser, contactIsSelected)
      case UnconnectedUser =>
        val connectedUser = directoryResults(item.index)
        val contactIsSelected = selectedUsers.contains(connectedUser.id)
        holder.asInstanceOf[UserViewHolder].bind(connectedUser, contactIsSelected)
      case SectionHeader =>
        holder.asInstanceOf[SectionHeaderViewHolder].bind(item.section)
      case NameInitialSeparator =>
        holder.asInstanceOf[AddressBookSectionHeaderViewHolder].bind(item.name)
      case AddressBookContact =>
        val contact  = contacts(item.index)
        holder.asInstanceOf[AddressBookContactViewHolder].bind(contact, contactsCallback)
      case Expand =>
        val itemCount = if (item.section == ContactsSection) contacts.size + connectedUsers.size else conversations.size
        holder.asInstanceOf[SectionExpanderViewHolder].bind(itemCount, new View.OnClickListener() {
          def onClick(v: View) {
            if (item.section == ContactsSection) expandContacts() else expandGroups()
          }
        })
      case _ =>
    }
  }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = {
    viewType match {
      case TopUsers =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_top_users, parent, false)
        val topUserAdapter: TopUserAdapter = new TopUserAdapter(new TopUserAdapter.Callback() {
          override def getSelectedUsers = selectedUsers
        })
        new viewholders.TopUsersViewHolder(view, topUserAdapter, parent.getContext)
      case ConnectedUser | UnconnectedUser =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_user, parent, false)
        new UserViewHolder(view, true)
      case GroupConversation =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_conversation, parent, false)
        new ConversationViewHolder(view)
      case SectionHeader =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_header, parent, false)
        new SectionHeaderViewHolder(view)
      case NameInitialSeparator =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_header, parent, false)
        new AddressBookSectionHeaderViewHolder(view, true)
      case AddressBookContact =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.contactlist_user, parent, false)
        new AddressBookContactViewHolder(view, true)
      case Expand =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_expander, parent, false)
        new SectionExpanderViewHolder(view)
    }
  }

  override def getItemViewType(position: Int) = mergedResult(position).itemType

  override def getItemId(position: Int) = mergedResult(position).id

  //TODO: this should be like the other signals
  def setContacts(contacts: Contacts): Unit = {
    contactsSignal ! (0 until contacts.size()).map(contacts.get)
  }

  def getSectionIndexForPosition(position: Int) = mergedResult(position).index

  private def expandContacts() = {
    collapsedContacts = false
    updateMergedResults()
  }

  private def expandGroups() = {
    collapsedGroups = false
    updateMergedResults()
  }
}

object PickUsersAdapter {

  //Item Types
  val TopUsers: Int = 0
  val NameInitialSeparator: Int = 1
  val AddressBookContact: Int = 2
  val ConnectedUser: Int = 3
  val UnconnectedUser: Int = 4
  val GroupConversation: Int = 5
  val SectionHeader: Int = 6
  val Expand: Int = 7

  //Sections
  val TopUsersSection = 0
  val TeamMembersSection = 1
  val GroupConversationsSection = 2
  val ContactsSection = 3
  val DirectorySection = 4

  //Constants
  val CollapsedContacts = 5
  val CollapsedGroups = 5
}

case class SearchResult(itemType: Int, section: Int, index: Int, id: Long, name: String)

object SearchResult{
  def apply(itemType: Int, section: Int, index: Int, id: Long): SearchResult = new SearchResult(itemType, section, index, id, "")
  def apply(itemType: Int, section: Int, index: Int, name: String): SearchResult = new SearchResult(itemType, section, index, itemType + section + index, name)
  def apply(itemType: Int, section: Int, index: Int): SearchResult = SearchResult(itemType, section, index, "")
}
