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
package com.waz.zclient.usersearch.adapters

import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog.verbose
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.utils.returning
import com.waz.zclient._
import com.waz.zclient.common.controllers.{IntegrationsController, UserAccountsController}
import com.waz.zclient.common.views.SingleUserRowView
import com.waz.zclient.paintcode.CreateGroupIcon
import com.waz.zclient.ui.text.GlyphTextView
import com.waz.zclient.usersearch.SearchResultOnItemTouchListener
import com.waz.zclient.usersearch.adapters.PickUsersAdapter._
import com.waz.zclient.usersearch.viewholders._
import com.waz.zclient.utils.RichView

import scala.concurrent.duration._

class PickUsersAdapter(topUsersOnItemTouchListener: SearchResultOnItemTouchListener,
                       adapterCallback: PickUsersAdapter.Callback,
                       integrationsController: IntegrationsController)
                      (implicit injector: Injector) extends RecyclerView.Adapter[RecyclerView.ViewHolder] with Injectable {

  implicit private val ec = EventContext.Implicits.global

  setHasStableIds(true)

  private val userAccountsController = inject[UserAccountsController]

  private var mergedResult = Seq[SearchResult]()
  private var collapsedContacts = true
  private var collapsedGroups = true

  private var teamId = Option.empty[TeamId]
  private var topUsers = IndexedSeq.empty[UserData]
  private var localResults = IndexedSeq.empty[UserData]
  private var conversations = IndexedSeq.empty[ConversationData]
  private var directoryResults = IndexedSeq.empty[UserData]
  private var integrations = IndexedSeq.empty[IntegrationData]
  private var currentUser = Option.empty[UserData]

  val filter = Signal("")

  val searchResults = for {
    z        <- inject[Signal[ZMessaging]]
    filter   <- filter
    res      <- z.userSearch.search(filter)
  } yield res

  val peopleOrServices = Signal[Boolean](false)

  peopleOrServices.on(Threading.Ui) { _ => updateMergedResults() }

  (for {
    team <- inject[Signal[ZMessaging]].map(_.teamId)
    res  <- searchResults
  } yield (team, res)).throttle(500.millis).onUi {
    case (team, res) =>
      verbose(res.toString)
      teamId           = team
      topUsers         = res.top
      localResults     = res.local
      conversations    = res.convs
      directoryResults = res.dir
      updateMergedResults()
  }

  integrationsController.searchIntegrations.throttle(500.millis).on(Threading.Ui) {
    case Some(newIntegrations) =>
      integrations = newIntegrations
      updateMergedResults()
    case _ =>
  }

  userAccountsController.currentUser.on(Threading.Ui){ user =>
    currentUser = user
    updateMergedResults()
  }

  private def updateMergedResults(): Unit = {
    mergedResult = Seq()

    val teamName = userAccountsController.teamData.map(_.name).getOrElse("")

    def addTopPeople(): Unit = {
      if (topUsers.nonEmpty) {
        mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, TopUsersSection, 0))
        mergedResult = mergedResult ++ Seq(SearchResult(TopUsers, TopUsersSection, 0))
      }
    }

    def addContacts(): Unit = {
      if (localResults.nonEmpty) {
        mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, ContactsSection, 0, teamName))
        var contactsSection = Seq[SearchResult]()

        contactsSection = contactsSection ++ localResults.indices.map { i =>
          SearchResult(ConnectedUser, ContactsSection, i, localResults(i).id.str.hashCode, localResults(i).getDisplayName)
        }

        val shouldCollapse = filter.currentValue.exists(_.nonEmpty) && collapsedContacts && contactsSection.size > CollapsedContacts

        contactsSection = contactsSection.sortBy(_.name).take(if (shouldCollapse) CollapsedContacts else contactsSection.size)

        mergedResult = mergedResult ++ contactsSection
        if (shouldCollapse) {
          mergedResult = mergedResult ++ Seq(SearchResult(Expand, ContactsSection, 0))
        }
      }
    }

    def addGroupConversations(): Unit = {
      if (conversations.nonEmpty) {
        mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, GroupConversationsSection, 0, teamName))

        val shouldCollapse = collapsedGroups && conversations.size > CollapsedGroups

        mergedResult = mergedResult ++ conversations.indices.map { i =>
          SearchResult(GroupConversation, GroupConversationsSection, i, conversations(i).id.str.hashCode)
        }.take(if (shouldCollapse) CollapsedGroups else conversations.size)
        if (shouldCollapse) {
          mergedResult = mergedResult ++ Seq(SearchResult(Expand, GroupConversationsSection, 0))
        }
      }
    }

    def addConnections(): Unit = {
      if (directoryResults.nonEmpty) {
        mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, DirectorySection, 0))
        mergedResult = mergedResult ++ directoryResults.indices.map { i =>
          SearchResult(UnconnectedUser, DirectorySection, i, directoryResults(i).id.str.hashCode)
        }
      }
    }

    def addIntegrations(): Unit = {
      if (integrations.nonEmpty) {
        mergedResult = mergedResult ++ Seq(SearchResult(SectionHeader, IntegrationsSection, 0))
        mergedResult = mergedResult ++ integrations.indices.map { i =>
          SearchResult(Integration, IntegrationsSection, i, integrations(i).id.str.hashCode)
        }
      }
    }

    def addGroupCreationButton(): Unit =
      mergedResult = mergedResult ++ Seq(SearchResult(NewConversation, TopUsersSection, 0))

    if (userAccountsController.isTeamAccount) {
      if (peopleOrServices.currentValue.contains(true)) {
        addIntegrations()
      } else {
        addGroupCreationButton()
        addContacts()
        addGroupConversations()
        addConnections()
      }
    } else  {
      addGroupCreationButton()
      addTopPeople()
      addContacts()
      addGroupConversations()
      addConnections()
    }

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
        val user = localResults(item.index)
        holder.asInstanceOf[UserViewHolder].bind(user, isGuest = user.isGuest(teamId))

      case UnconnectedUser =>
        val unconnectedUser = directoryResults(item.index)
        holder.asInstanceOf[UserViewHolder].bind(unconnectedUser, isGuest = false)

      case SectionHeader =>
        holder.asInstanceOf[SectionHeaderViewHolder].bind(item.section, item.name)

      case Expand =>
        val itemCount = if (item.section == ContactsSection) localResults.size else conversations.size
        holder.asInstanceOf[SectionExpanderViewHolder].bind(itemCount, new View.OnClickListener() {
          def onClick(v: View): Unit = {
            if (item.section == ContactsSection) expandContacts() else expandGroups()
          }
        })

      case Integration =>
        val integration = integrations(item.index)
        holder.asInstanceOf[IntegrationViewHolder].bind(integration)
      case _ =>
    }
  }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = {
    viewType match {
      case TopUsers =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_top_users, parent, false)
        new TopUsersViewHolder(view, new TopUserAdapter(), parent.getContext)

      case ConnectedUser | UnconnectedUser =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.single_user_row, parent, false).asInstanceOf[SingleUserRowView]
        val vh = new UserViewHolder(view)
        view.onClick(vh.userData.foreach(u => adapterCallback.onUserClicked(u.id, vh.itemView)))
        vh

      case GroupConversation =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_conversation, parent, false)
        new ConversationViewHolder(view)

      case SectionHeader =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_header, parent, false)
        new SectionHeaderViewHolder(view)

      case Expand =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.startui_section_expander, parent, false)
        new SectionExpanderViewHolder(view)

      case Integration =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.single_user_row, parent, false).asInstanceOf[SingleUserRowView]
        val vh = new IntegrationViewHolder(view)
        view.onClick(vh.integrationData.foreach(i => adapterCallback.onIntegrationClicked(i)))
        vh

      case NewConversation =>
        val view = returning(LayoutInflater.from(parent.getContext).inflate(R.layout.startui_create_conv, parent, false)) { l =>
          l.findViewById[GlyphTextView](R.id.icon).setBackground(CreateGroupIcon(R.color.white)(parent.getContext))
          l.onClick(adapterCallback.onCreateConvClicked())
        }
        new RecyclerView.ViewHolder(view) {}
    }
  }

  override def getItemViewType(position: Int) = mergedResult.lift(position).fold(-1)(_.itemType)

  override def getItemId(position: Int) = mergedResult.lift(position).fold(-1L)(_.id)

  def getSectionIndexForPosition(position: Int) = mergedResult.lift(position).fold(-1)(_.index)

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
  val ConnectedUser: Int = 1
  val UnconnectedUser: Int = 2
  val GroupConversation: Int = 3
  val SectionHeader: Int = 4
  val Expand: Int = 5
  val Integration: Int = 6
  val NewConversation: Int = 7

  //Sections
  val TopUsersSection = 0
  val GroupConversationsSection = 1
  val ContactsSection = 2
  val DirectorySection = 3
  val IntegrationsSection = 4

  //Constants
  val CollapsedContacts = 5
  val CollapsedGroups = 5

  trait Callback {
    def onUserClicked(userId: UserId, anchorView: View): Unit
    def onIntegrationClicked(data: IntegrationData): Unit
    def onCreateConvClicked(): Unit
  }

}

case class SearchResult(itemType: Int, section: Int, index: Int, id: Long, name: String)

object SearchResult{
  def apply(itemType: Int, section: Int, index: Int, id: Long): SearchResult = new SearchResult(itemType, section, index, id, "")
  def apply(itemType: Int, section: Int, index: Int, name: String): SearchResult = new SearchResult(itemType, section, index, itemType + section + index, name)
  def apply(itemType: Int, section: Int, index: Int): SearchResult = SearchResult(itemType, section, index, "")
}
