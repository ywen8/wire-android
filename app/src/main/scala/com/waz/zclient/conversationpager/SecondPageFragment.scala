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
package com.waz.zclient.conversationpager

import android.content.Intent
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.api.IConversation
import com.waz.model.UserId
import com.waz.threading.Threading
import com.waz.zclient.common.controllers.UserAccountsController
import com.waz.zclient.connect.{ConnectRequestFragment, PendingConnectRequestManagerFragment}
import com.waz.zclient.controllers.navigation.{INavigationController, Page, PagerControllerObserver}
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.core.stores.connect.IConnectStore
import com.waz.zclient.core.stores.conversation.ConversationChangeRequester
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.conversation.ConversationManagerFragment
import com.waz.zclient.ui.utils.MathUtils
import com.waz.zclient.{FragmentHelper, OnBackPressedListener, R}

object SecondPageFragment {
  val Tag: String = classOf[SecondPageFragment].getName
  val ArgConversationId = "ARGUMENT_CONVERSATION_ID"
  private val StateSecondPagePosition = "SECOND_PAGE_POSITION"


  def newInstance = new SecondPageFragment

  trait Container {
    def onOpenUrl(url: String): Unit
  }

}

class SecondPageFragment extends BaseFragment[SecondPageFragment.Container]
  with FragmentHelper
  with OnBackPressedListener
  with ConversationManagerFragment.Container
  with PagerControllerObserver
  with PendingConnectRequestManagerFragment.Container
  with ConnectRequestFragment.Container {

  import Threading.Implicits.Ui
  import SecondPageFragment._

  private var currentPage: Page = _

  private lazy val navigationController = inject[INavigationController]
  private lazy val userAccountsController = inject[UserAccountsController]
  private lazy val conversationController = inject[ConversationController]

  override def setUserVisibleHint(isVisibleToUser: Boolean): Unit = {
    super.setUserVisibleHint(isVisibleToUser)
    if (isAdded) {
      val fragment = getChildFragmentManager.findFragmentById(R.id.fl__second_page_container)
      if (fragment != null) fragment.setUserVisibleHint(isVisibleToUser)
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_pager_second, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    if (savedInstanceState == null) currentPage = Page.NONE
    else {
      val pos = savedInstanceState.getInt(StateSecondPagePosition)
      currentPage = Page.values()(pos)
    }

    (for {
      conv <- conversationController.currentConv
      convMembers <- conversationController.currentConvMembers
    } yield conv -> convMembers.head).onUi { case (conv, userId) =>
      info(s"Conversation: ${conv.id} type: ${conv.convType}")

      // either starting from beginning or switching fragment
      val switchingToPendingConnectRequest = conv.convType == IConversation.Type.WAIT_FOR_CONNECTION
      val switchingToConnectRequestInbox = conv.convType == IConversation.Type.INCOMING_CONNECTION

      if (switchingToConnectRequestInbox) openPage(Page.CONNECT_REQUEST_INBOX, userId)
      else if (switchingToPendingConnectRequest) openPage(Page.CONNECT_REQUEST_PENDING, userId)
      else openPage(Page.MESSAGE_STREAM, userId)
    }

  }

  override def onSaveInstanceState(outState: Bundle): Unit = {
    outState.putInt(StateSecondPagePosition, currentPage.ordinal)
    super.onSaveInstanceState(outState)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    val fragment = getChildFragmentManager.findFragmentById(R.id.fl__second_page_container)
    if (fragment != null) fragment.onActivityResult(requestCode, resultCode, data)
  }

//  final private val callback = new Callback[ConversationController.ConversationChange]() {
//    override def callback(change: ConversationController.ConversationChange): Unit = {
//      if (change.toConvId == null) return
//      inject(classOf[ConversationController]).withConvLoaded(change.toConvId, new Callback[ConversationData]() {
//        override def callback(convData: ConversationData): Unit = {
//          info(s"Conversation: ${change.toConvId} type: ${convData.convType} requester: ${change.requester}")
//          // either starting from beginning or switching fragment
//          val switchingToPendingConnectRequest = convData.convType eq IConversation.Type.WAIT_FOR_CONNECTION
//          val switchingToConnectRequestInbox = convData.convType eq IConversation.Type.INCOMING_CONNECTION
//          if (switchingToConnectRequestInbox) {
//            val arguments = new Bundle
//            arguments.putString(SecondPageFragment.ArgConversationId, change.toConvId.str)
//            openPage(Page.CONNECT_REQUEST_INBOX, arguments)
//          }
//          else if (switchingToPendingConnectRequest) {
//            val arguments = new Bundle
//            arguments.putString(SecondPageFragment.ArgConversationId, change.toConvId.str)
//            openPage(Page.CONNECT_REQUEST_PENDING, arguments)
//          }
//          else openPage(Page.MESSAGE_STREAM, new Bundle)
//        }
//      })
//    }
//  }

  private def openPage(page: Page, userId: UserId) = {
    info(s"openPage ${page.name} userId $userId")
//    if (getContainer == null || !isResumed) return
//
//    val fragment = getChildFragmentManager.findFragmentById(R.id.fl__second_page_container)
//    if (currentPage != null && currentPage == page) { // Scroll to a certain connect request in inbox
//      if (fragment.isInstanceOf[ConnectRequestFragment]) { //TODO set a preference or something
//        fragment.asInstanceOf[ConnectRequestFragment].setVisibleConnectRequest(new UserId(arguments.getString(SecondPageFragment.ArgConversationId)))
//      }
//      if (page != Page.CONNECT_REQUEST_PENDING) return
//    }
    currentPage = page

    Some(page)
      .collect {
        case Page.CONNECT_REQUEST_PENDING =>
          navigationController.setRightPage(Page.PENDING_CONNECT_REQUEST_AS_CONVERSATION, Tag)
          PendingConnectRequestManagerFragment.Tag -> PendingConnectRequestManagerFragment.newInstance(
            userId,
            IConnectStore.UserRequester.CONVERSATION
          )
        case Page.CONNECT_REQUEST_INBOX =>
          navigationController.setRightPage(Page.CONNECT_REQUEST_INBOX, SecondPageFragment.Tag)
          ConnectRequestFragment.FragmentTag -> ConnectRequestFragment.newInstance(userId)
        case Page.MESSAGE_STREAM =>
          navigationController.setRightPage(Page.MESSAGE_STREAM, SecondPageFragment.Tag)
          ConversationManagerFragment.Tag -> ConversationManagerFragment.newInstance
      }
      .map { case (tag, pageFragment) =>
        getChildFragmentManager.beginTransaction
          .replace(R.id.fl__second_page_container, pageFragment, tag)
      }
      .map(_ -> navigationController.getCurrentPage)
      .collect {
        case (transaction, Page.CONVERSATION_LIST) =>
          transaction.setCustomAnimations(
            R.anim.message_fade_in,
            R.anim.message_fade_out,
            R.anim.message_fade_in,
            R.anim.message_fade_out
          )
        case (transaction, Page.CONNECT_REQUEST_INBOX | Page.CONNECT_REQUEST_PENDING) =>
          transaction.setCustomAnimations(
            R.anim.fragment_animation_second_page_slide_in_from_right,
            R.anim.fragment_animation_second_page_slide_out_to_left
          )
      }
      .foreach(transaction => transaction.commit())
  }

  override def onOpenUrl(url: String): Unit = {
    getContainer.onOpenUrl(url)
  }

  override def onBackPressed: Boolean = {
    val fragment = getChildFragmentManager.findFragmentById(R.id.fl__second_page_container)
    fragment.isInstanceOf[OnBackPressedListener] && fragment.asInstanceOf[OnBackPressedListener].onBackPressed
  }

  override def onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int): Unit = {
    if (position == 0 || MathUtils.floatEqual(positionOffset, 0f)) getView.setAlpha(1f)
    else getView.setAlpha(Math.pow(positionOffset, 4).toFloat)
  }

  override def onPageSelected(position: Int): Unit = {
  }

  override def onPageScrollStateChanged(state: Int): Unit = {
  }

  override def onAcceptedConnectRequest(userId: UserId): Unit = {
    info(s"onAcceptedConnectRequest $userId")
    userAccountsController.getConversationId(userId).flatMap { convId =>
      conversationController.selectConv(convId, ConversationChangeRequester.CONVERSATION_LIST)
    }
  }

  override def dismissInboxFragment(): Unit = {
    info("dismissInboxFragment")
    navigationController.setVisiblePage(Page.CONVERSATION_LIST, Tag)
  }

  override def onPagerEnabledStateHasChanged(enabled: Boolean): Unit = {
  }

  override def dismissUserProfile(): Unit = {
  }

  override def dismissSingleUserProfile(): Unit = {
  }

  override def showRemoveConfirmation(userId: UserId): Unit = {
  }
}
