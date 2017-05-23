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
import com.waz.api.{ContactDetails, User}
import com.waz.model._
import com.waz.threading.Threading
import com.waz.utils.events.EventContext
import com.waz.zclient._
import com.waz.zclient.adapters.PickUsersAdapter._
import com.waz.zclient.controllers.SearchUserController
import com.waz.zclient.pages.main.pickuser.SearchResultAdapter
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import com.waz.zclient.viewholders._
import com.waz.zclient.views.pickuser.ContactRowView.Callback

import scala.collection.JavaConverters._

class PickUsersAdapter(context: Context, topUsersOnItemTouchListener: SearchResultOnItemTouchListener, adapterCallback: SearchResultAdapter.Callback, searchUserController: SearchUserController)
                      (implicit injector: Injector) extends RecyclerView.Adapter[RecyclerView.ViewHolder] with Injectable {

  implicit private val ec = EventContext.Implicits.global
  implicit private val logTag = ZLog.logTagFor[PickUsersAdapter]
  //implicit private val ctx = context

  setHasStableIds(true)
  searchUserController.onDataChanged.on(Threading.Ui){ _ =>
    updateMergedResults()
  }

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

  private def updateMergedResults(): Unit ={
    mergedResult = Seq()

    //TOP PEOPLE
    if (searchUserController.topUsers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TopUsersSection, 0))
      mergedResult = mergedResult ++ Seq(SearchResult(TopUsers, TopUsersSection, 0))
    }

    //TEAM MEMBERS
    if (searchUserController.teamMembers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TeamMembersSection, 0))
      mergedResult = mergedResult ++ searchUserController.teamMembers.indices.map { i =>
        SearchResult(ConnectedUser, TeamMembersSection, i, searchUserController.teamMembers(i).id.str.hashCode)
      }
    }

    //CONTACTS
    if (searchUserController.contacts.nonEmpty || searchUserController.connectedUsers.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, ContactsSection, 0))
      var contactsSection = Seq[SearchResult]()

      contactsSection = contactsSection ++ searchUserController.contacts.indices.map { i =>
        SearchResult(AddressBookContact, ContactsSection, i)
      }

      contactsSection = contactsSection ++ searchUserController.connectedUsers.indices.map { i =>
        SearchResult(ConnectedUser, ContactsSection, i, searchUserController.connectedUsers(i).id.str.hashCode)
      }

      val shouldCollapse = searchUserController.searchState.currentValue.exists(_.filter.nonEmpty) && collapsedContacts && contactsSection.size > CollapsedContacts

      contactsSection = contactsSection.sortBy(_.name).take(if (shouldCollapse) CollapsedContacts else contactsSection.size)

      mergedResult = mergedResult ++ contactsSection
      if (shouldCollapse) {
        mergedResult = mergedResult ++ Seq(SearchResult(Expand, ContactsSection, 0))
      }
    }

    //GROUP CONVERSATIONS
    if (searchUserController.conversations.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, GroupConversationsSection, 0))

      val shouldCollapse = collapsedGroups && searchUserController.conversations.size > CollapsedGroups

      mergedResult = mergedResult ++ searchUserController.conversations.indices.map { i =>
        SearchResult(GroupConversation, GroupConversationsSection, i, searchUserController.conversations(i).id.str.hashCode)
      }.take(if (shouldCollapse) CollapsedGroups else searchUserController.conversations.size)
      if (shouldCollapse) {
        mergedResult = mergedResult ++ Seq(SearchResult(Expand, GroupConversationsSection, 0))
      }
    }

    //CONNECT
    if (searchUserController.directoryResults.nonEmpty) {
      mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, DirectorySection, 0))
      mergedResult = mergedResult ++ searchUserController.directoryResults.indices.map { i =>
        SearchResult(UnconnectedUser, DirectorySection, i, searchUserController.directoryResults(i).id.str.hashCode)
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
        holder.asInstanceOf[TopUsersViewHolder].bind(searchUserController.topUsers)
        holder.asInstanceOf[TopUsersViewHolder].bindOnItemTouchListener(topUsersOnItemTouchListener)
      case GroupConversation =>
        val conversation = searchUserController.conversations(item.index)
        holder.asInstanceOf[ConversationViewHolder].bind(conversation)
      case ConnectedUser =>
        val connectedUser = searchUserController.connectedUsers(item.index)
        val contactIsSelected = selectedUsers.contains(connectedUser.id)
        holder.asInstanceOf[UserViewHolder].bind(connectedUser, contactIsSelected)
      case UnconnectedUser =>
        val connectedUser = searchUserController.directoryResults(item.index)
        val contactIsSelected = selectedUsers.contains(connectedUser.id)
        holder.asInstanceOf[UserViewHolder].bind(connectedUser, contactIsSelected)
      case SectionHeader =>
        holder.asInstanceOf[SectionHeaderViewHolder].bind(item.section)
      case NameInitialSeparator =>
        holder.asInstanceOf[AddressBookSectionHeaderViewHolder].bind(item.name)
      case AddressBookContact =>
        val contact  = searchUserController.contacts(item.index)
        holder.asInstanceOf[AddressBookContactViewHolder].bind(contact, contactsCallback)
      case Expand =>
        val itemCount =
          if (item.section == ContactsSection)
            searchUserController.contacts.size + searchUserController.connectedUsers.size
          else
            searchUserController.conversations.size
        holder.asInstanceOf[SectionExpanderViewHolder].bind(itemCount, new View.OnClickListener() {
          def onClick(v: View): Unit = {
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
