package com.waz.zclient.connect

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.model.{ConvId, UserId}
import com.waz.service.NetworkModeService
import com.waz.zclient.controllers.navigation.Page
import com.waz.zclient.core.stores.connect.IConnectStore
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.connect.{ConnectRequestLoadMode, UserProfileContainer}
import com.waz.zclient.participants.OptionsMenuFragment
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.{FragmentHelper, OnBackPressedListener, R}

/**
  * Created by admin on 3/8/18.
  */

object PendingConnectRequestManagerFragment {
  val TAG: String = classOf[PendingConnectRequestManagerFragment].getName
  val ArgUserId = "ARGUMENT_USER_ID"
  val ArgUserRequester = "ARGUMENT_USER_REQUESTER"

  def newInstance(userId: String, conversationId: String, loadMode: ConnectRequestLoadMode, userRequester: IConnectStore.UserRequester): PendingConnectRequestManagerFragment = {
    val newFragment = new PendingConnectRequestManagerFragment
    val args = new Bundle
    args.putString(ArgUserId, userId)
    args.putString(ArgUserRequester, userRequester.toString)
    newFragment.setArguments(args)
    newFragment
  }

  trait Container extends UserProfileContainer {
    def onAcceptedConnectRequest(userId: UserId): Unit
    def onAcceptedPendingOutgoingConnectRequest(userId: UserId): Unit
  }

}

class PendingConnectRequestManagerFragment extends BaseFragment[PendingConnectRequestManagerFragment.Container]
  with FragmentHelper
  with PendingConnectRequestFragment.Container
  with OnBackPressedListener {

  import PendingConnectRequestManagerFragment._

  private lazy val networkService = inject[NetworkModeService]

  private lazy val userRequester =
    IConnectStore.UserRequester.valueOf(getArguments.getString(ArgUserRequester))

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_connect_request_pending_manager, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    if (savedInstanceState == null) {
      val userId = getArguments.getString(PendingConnectRequestManagerFragment.ArgUserId)
      {
        import PendingConnectRequestFragment._
        getChildFragmentManager
          .beginTransaction
          .add(R.id.fl__pending_connect_request, newInstance(userId, userRequester), Tag)
          .commit
      }

      {
        import OptionsMenuFragment._
        getChildFragmentManager
          .beginTransaction
          .add(R.id.fl__pending_connect_request__settings_box, newInstance(inConvList = false), Tag)
          .commit
      }
    }
  }

  override def dismissUserProfile(): Unit = {
    getContainer.dismissUserProfile()
  }

  override def dismissSingleUserProfile(): Unit = {
    if (getChildFragmentManager.popBackStackImmediate) restoreCurrentPageAfterClosingOverlay()
  }

  override def showRemoveConfirmation(userId: UserId): Unit = {
    if (networkService.isOnlineMode) {
      getContainer.showRemoveConfirmation(userId)
    } else {
      ViewUtils.showAlertDialog(
        getActivity,
        R.string.alert_dialog__no_network__header,
        R.string.remove_from_conversation__no_network__message,
        R.string.alert_dialog__confirmation,
        null,
        true
      )
    }
  }

  override def onConversationUpdated(conversation: ConvId): Unit = {
    //todo Do we need this??
//    getContainer.onAcceptedPendingOutgoingConnectRequest(conversation)
  }

  private def restoreCurrentPageAfterClosingOverlay() = {
    if (getControllerFactory != null && !getControllerFactory.isTornDown) {
      val targetLeftPage =
        if (userRequester == IConnectStore.UserRequester.CONVERSATION)
          Page.PENDING_CONNECT_REQUEST_AS_CONVERSATION
        else
          Page.PENDING_CONNECT_REQUEST

      getControllerFactory.getNavigationController
        .setRightPage(targetLeftPage, PendingConnectRequestManagerFragment.TAG)
    }
  }

  override def onAcceptedConnectRequest(userId: UserId): Unit = {
    getContainer.onAcceptedConnectRequest(userId)
  }

  override def onBackPressed = false

}
