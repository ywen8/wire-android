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

import java.util

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, ViewGroup}
import com.waz.ZLog
import com.waz.api.User
import com.waz.model.ConversationData.ConversationType
import com.waz.model.SearchQuery.{Recommended, TopPeople}
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.SeqMap
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.adapters.PickUsersAdapter._
import com.waz.zclient.pages.main.pickuser.TopUserAdapter
import com.waz.zclient.pages.main.pickuser.views.viewholders._
import com.waz.zclient.{Injectable, Injector, R}

class PickUsersAdapter(context: Context)
                      (implicit injector: Injector) extends RecyclerView.Adapter[RecyclerView.ViewHolder] with Injectable {

  implicit private val ec = EventContext.Implicits.global
  implicit private val logTag = ZLog.logTagFor[PickUsersAdapter]

  val teamFilter = Signal[Option[TeamId]](None)
  val filter = Signal[String]("")
  //TODO: for java
  def setFiler(string: String): Unit = {
    filter ! string
  }

  val zms = inject[Signal[ZMessaging]]
  val darkTheme = false //TODO: remove this

  private val topUsersSignal = for {
    z <- zms
    filterStr <- filter
    users <- if (filterStr.isEmpty) z.userSearch.searchUserData(TopPeople) else Signal(SeqMap.empty[UserId, UserData])
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
    convs <- z.convsStorage.convsSignal
  } yield convs.conversations.filter{ conv =>
    conv.convType == ConversationType.Group &&
    (filterStr.isEmpty || conv.displayName.contains(filterStr)) && //TODO: this filter?
    (teamId.isEmpty || conv.team.exists(_.id == teamId.get))
  }.toSeq

  private val searchSignal = for {
    z <- zms
    teamId <- teamFilter
    filterStr <- filter
    users <- z.userSearch.searchUserData(Recommended(filterStr)) //TODO: check if it has a @
  } yield users.values//.filter(_.connection == UserData.ConnectionStatus.Accepted)

  private val contactsSignal = for {
    z <- zms
    teamId <- teamFilter
    filterStr <- filter
    contacts <- Signal.wrap(z.contacts.contactsLoaded)
  } yield contacts.filter(_.name.contains(filterStr)) //TODO: where to get this filtered?

  private var topUsers = Seq[UserData]()
  private var teamMembers = Seq[UserData]()
  private var conversations = Seq[ConversationData]()
  private var connectedUsers = Seq[UserData]()
  private var contacts = Seq[Contact]()
  private var directoryResults = Seq[UserData]()

  private var mergedResult = Seq[SearchResult]()

  var selectedUsers = Seq[UserData]()//TODO: How to fill this?

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
  searchSignal.on(Threading.Ui) { data =>
    connectedUsers = data.filter(_.connection == UserData.ConnectionStatus.Accepted)
    directoryResults = data.filter(_.connection == UserData.ConnectionStatus.Unconnected)
    updateMergedResults()
  }

  private def updateMergedResults(): Unit ={
    mergedResult = Seq()

    //TOP PEOPLE
    if (topUsers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TopUsersSection, 0, "TOP PEOPLE"))
      mergedResult = mergedResult ++ Seq(SearchResult(TopUsers, TopUsersSection, 0, ""))
    }

    //TEAM MEMBERS
    if (teamMembers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TeamMembersSection, 0, "TEAM MEMBERS"))
      mergedResult = mergedResult ++ teamMembers.indices.map { i =>
        SearchResult(ConnectedUser, TeamMembersSection, i, teamMembers(i).displayName)
      }
    }

    //GROUP CONVERSATIONS
    if (teamMembers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, GroupConversationsSection, 0, "GROUP CONVERSATIONS"))
      mergedResult = mergedResult ++ conversations.indices.map { i =>
        SearchResult(GroupConversation, GroupConversationsSection, i, conversations(i).displayName)
      }
    }

    //CONTACTS
    if (contacts.nonEmpty || connectedUsers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, ContactsSection, 0, "CONTACTS"))
      var contactsSection = Seq[SearchResult]()

      contactsSection = contactsSection ++ contacts.indices.map { i =>
        SearchResult(AddressBookContact, ContactsSection, i, contacts(i).name)
      }

      contactsSection = contactsSection ++ connectedUsers.indices.map { i =>
        SearchResult(ConnectedUser, ContactsSection, i, connectedUsers(i).name)
      }

      contactsSection = contactsSection.sortBy(_.name)
      //TODO: ADD INITIALS HERE OR AS DECORATORS?
      mergedResult = mergedResult ++ contactsSection
    }

    //CONNECT
    if (directoryResults.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, DirectorySection, 0, "CONNECT"))
      mergedResult = mergedResult ++ directoryResults.indices.map { i =>
        SearchResult(UnconnectedUser, DirectorySection, i, directoryResults(i).displayName)
      }
    }

    ZLog.verbose(s"Merged contacts updated: ${mergedResult.size}")
    notifyDataSetChanged()
  }

  override def getItemCount = mergedResult.size

  override def onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = {
    val item = mergedResult(position)
    item.itemType match {
      case TopUsers =>
        //holder.asInstanceOf[TopUsersViewHolder].bind(topUsers)
        //holder.asInstanceOf[TopUsersViewHolder].bindOnItemTouchListener(topUsersOnItemTouchListener)
      case GroupConversation =>
        val conversation = conversations(item.index)
        //holder.asInstanceOf[ConversationViewHolder].bind(conversation)
      case ConnectedUser =>
        val connectedUser = connectedUsers(item.index)
        val contactIsSelected = selectedUsers.exists(_.id == connectedUser.id)
        //holder.asInstanceOf[UserViewHolder].bind(connectedUser, contactIsSelected)
      case UnconnectedUser =>
        val connectedUser = directoryResults(item.index)
        val contactIsSelected = selectedUsers.exists(_.id == connectedUser.id)
      case SectionHeader =>
        holder.asInstanceOf[SectionHeaderViewHolder].bind(item.section)
      case NameInitialSeparator =>
        holder.asInstanceOf[AddressBookSectionHeaderViewHolder].bind(item.name)
      case AddressBookContact =>
        val contact  = contacts(item.index)
        //holder.asInstanceOf[AddressBookContactViewHolder].bind(contact, null, 0) //TODO: accent color and callback
      case Expand =>
        /*
        if (getSectionForPosition(position) == ITEM_TYPE_CONNECTED_USER) holder.asInstanceOf[SectionExpanderViewHolder].bind(mergedContacts.size, new View.OnClickListener() {
          def onClick(v: View) {
            setContactsCollapsed(false)
          }
        })
        else holder.asInstanceOf[SectionExpanderViewHolder].bind(conversations.length, new View.OnClickListener() {
          def onClick(v: View) {
            setGroupsCollapsed(false)
          }
        })
        */
      case _ =>
    }
  }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = {
    viewType match {
      case TopUsers =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_top_users, parent, false)
        val topUserAdapter: TopUserAdapter = new TopUserAdapter(new TopUserAdapter.Callback() {
          override def getSelectedUsers = {new util.HashSet[User]()}//TODO: what is this?
        })
        new TopUsersViewHolder(view, topUserAdapter, parent.getContext)
      case ConnectedUser | UnconnectedUser =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_user, parent, false)
        new UserViewHolder(view, darkTheme, true)
      case GroupConversation =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_conversation, parent, false)
        new ConversationViewHolder(view)
      case SectionHeader =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_header, parent, false)
        new SectionHeaderViewHolder(view)
      case NameInitialSeparator =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_header, parent, false)
        new AddressBookSectionHeaderViewHolder(view, darkTheme)
      case AddressBookContact =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.contactlist_user, parent, false)
        new AddressBookContactViewHolder(view, darkTheme)
      case Expand =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_expander, parent, false)
        new SectionExpanderViewHolder(view)
    }
  }

  override def getItemViewType(position: Int) = mergedResult(position).itemType
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
}

case class SearchResult(itemType: Int, section: Int, index: Int, name: String)
