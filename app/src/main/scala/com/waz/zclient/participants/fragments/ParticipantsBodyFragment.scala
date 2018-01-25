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

package com.waz.zclient.participants.fragments

import android.os.Bundle
import android.support.v7.widget.{GridLayoutManager, RecyclerView}
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import com.waz.api.IConversation
import com.waz.api.Message
import com.waz.api.NetworkMode
import com.waz.api.OtrClient
import com.waz.api.User
import com.waz.api.UsersList
import com.waz.model.{ConvId, ConversationData, IntegrationId, ProviderId}
import com.waz.zclient.{BaseActivity, FragmentHelper, R}
import com.waz.zclient.common.controllers.ThemeController
import com.waz.zclient.common.controllers.UserAccountsController
import com.waz.zclient.controllers.accentcolor.AccentColorObserver
import com.waz.zclient.controllers.confirmation.ConfirmationRequest
import com.waz.zclient.controllers.confirmation.IConfirmationController
import com.waz.zclient.controllers.confirmation.TwoButtonConfirmationCallback
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.core.stores.connect.ConnectStoreObserver
import com.waz.zclient.core.stores.connect.IConnectStore
import com.waz.zclient.core.stores.conversation.ConversationChangeRequester
import com.waz.zclient.core.stores.network.NetworkAction
import com.waz.zclient.core.stores.participants.ParticipantsStoreObserver
import com.waz.zclient.common.controllers.SoundController
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.conversation.controller.ConversationScreenControllerObserver
import com.waz.zclient.pages.main.conversation.controller.IConversationScreenController
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import com.waz.zclient.participants.ParticipantsChatheadAdapter
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.utils.Callback
import com.waz.zclient.utils.LayoutSpec
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.views.images.ImageAssetImageView
import com.waz.zclient.views.menus.FooterMenu
import com.waz.zclient.views.menus.FooterMenuCallback

class ParticipantBodyFragment extends BaseFragment[ParticipantBodyFragment.Container] with FragmentHelper
  with ConversationScreenControllerObserver with ParticipantsStoreObserver with AccentColorObserver with ConnectStoreObserver {
  private var participantsView: RecyclerView = _
  private var participantsAdapter: ParticipantsChatheadAdapter = _
  private var footerMenu: FooterMenu = _
  private var topBorder: View = _
  private var footerWrapper: LinearLayout = _
  private var unblockButton: ZetaButton = _
  private var userRequester: IConnectStore.UserRequester = _
  private var imageAssetImageView: ImageAssetImageView = _

  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation = {
    val parent = getParentFragment
    // Apply the workaround only if this is a child fragment, and the parent
    // is being removed.
    if (!enter && parent != null && parent.isRemoving) { // This is a workaround for the bug where child fragments disappear when
      // the parent is removed (as all children are first removed from the parent)
      // See https://code.google.com/p/android/issues/detail?id=55228
      val doNothingAnim = new AlphaAnimation(1, 1)
      doNothingAnim.setDuration(ViewUtils.getNextAnimationDuration(parent))
      doNothingAnim
    }
    else super.onCreateAnimation(transit, enter, nextAnim)
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val args = getArguments
    userRequester = args.getSerializable(ParticipantBodyFragment.ARG_USER_REQUESTER).asInstanceOf[IConnectStore.UserRequester]
  }

  override def onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup, savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_group_participant, viewGroup, false)
    footerMenu = ViewUtils.getView(view, R.id.fm__participants__footer)
    topBorder = ViewUtils.getView(view, R.id.v_participants__footer__top_border)
    footerWrapper = ViewUtils.getView(view, R.id.ll__participants__footer_wrapper)
    unblockButton = ViewUtils.getView(view, R.id.zb__single_user_participants__unblock_button)
    imageAssetImageView = ViewUtils.getView(view, R.id.iaiv__participant_body)
    imageAssetImageView.setDisplayType(ImageAssetImageView.DisplayType.CIRCLE)

    implicit val ctx = getContext

    val numberOfColumns = getResources.getInteger(R.integer.participant_column__count)
    participantsAdapter = new ParticipantsChatheadAdapter(numberOfColumns)
    participantsView = ViewUtils.getView(view, R.id.pgv__participants)

    participantsView.setAdapter(participantsAdapter)

    participantsAdapter.onClick.onUi { userId =>
      val user = getStoreFactory.pickUserStore.getUser(userId.str)
      getControllerFactory.getConversationScreenController.showUser(user)
    }

    val layoutManager = new GridLayoutManager(ctx, numberOfColumns)

    layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      override def getSpanSize(position: Int): Int = participantsAdapter.getSpanSize(position)
    })

    participantsView.setLayoutManager(layoutManager)

    // Hide footer until conversation is loaded
    footerMenu.setVisibility(View.GONE)
    unblockButton.setVisibility(View.GONE)
    // Toggle color background
    view.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View): Unit = {
        getContainer.onClickedEmptyBackground()
      }
    })

    view
  }

  override def onStart(): Unit = {
    super.onStart()
    if (userRequester eq IConnectStore.UserRequester.POPOVER) {
      getStoreFactory.connectStore.addConnectRequestObserver(this)
      val user = getStoreFactory.singleParticipantStore.getUser
      getStoreFactory.connectStore.loadUser(user.getId, userRequester)
    }
    else getStoreFactory.participantsStore.addParticipantsStoreObserver(this)
    getControllerFactory.getConversationScreenController.addConversationControllerObservers(this)
    getControllerFactory.getAccentColorController.addAccentColorObserver(this)
  }

  override def onStop(): Unit = {
    getStoreFactory.connectStore.removeConnectRequestObserver(this)
    getControllerFactory.getConversationScreenController.removeConversationControllerObservers(this)
    getStoreFactory.participantsStore.removeParticipantsStoreObserver(this)
    getControllerFactory.getAccentColorController.removeAccentColorObserver(this)
    super.onStop()
  }

  override def onDestroyView(): Unit = {
    imageAssetImageView = null
    participantsView = null
    participantsAdapter = null
    footerMenu = null
    topBorder = null
    footerWrapper = null
    super.onDestroyView()
  }

  override def onShowParticipants(anchorView: View, isSingleConversation: Boolean, isMemberOfConversation: Boolean, showDeviceTabIfSingle: Boolean): Unit = {
  }

  override def onHideParticipants(backOrButtonPressed: Boolean, hideByConversationChange: Boolean, isSingleConversation: Boolean): Unit = {
  }

  override def onShowEditConversationName(show: Boolean): Unit = {
  }

  override def onHeaderViewMeasured(participantHeaderHeight: Int): Unit = {
  }

  override def onScrollParticipantsList(verticalOffset: Int, scrolledToBottom: Boolean): Unit = {
    if (topBorder == null) return
    // Toggle footer border
    if (scrolledToBottom) topBorder.setVisibility(View.INVISIBLE)
    else topBorder.setVisibility(View.VISIBLE)
  }

  override def onShowUser(user: User): Unit = {
  }

  override def onHideUser(): Unit = {
  }

  override def onAddPeopleToConversation(): Unit = {
  }

  override def onShowConversationMenu(@IConversationScreenController.ConversationMenuRequester requester: Int, convId: ConvId): Unit = {
  }

  override def onShowOtrClient(otrClient: OtrClient, user: User): Unit = {
  }

  override def onShowCurrentOtrClient(): Unit = {
  }

  override def onHideOtrClient(): Unit = {
  }

  override def onShowLikesList(message: Message): Unit = {
  }
  
  override def onShowIntegrationDetails(providerId: ProviderId, integrationId: IntegrationId): Unit = {
  }

  override def conversationUpdated(conversation: IConversation): Unit = {
    footerMenu.setVisibility(View.VISIBLE)
    if (conversation.getType eq IConversation.Type.ONE_TO_ONE) {
      footerMenu.setLeftActionText(getString(R.string.glyph__plus))
      topBorder.setVisibility(View.INVISIBLE)
      footerMenu.setRightActionText(getString(R.string.glyph__more))
      getStoreFactory.singleParticipantStore.setUser(conversation.getOtherParticipant)
    } else {
      imageAssetImageView.setVisibility(View.GONE)
      // Check if self user is member for group conversation and has permission to add
      val permissionToAdd = getActivity.asInstanceOf[BaseActivity].injectJava(classOf[UserAccountsController]).hasAddConversationMemberPermission(new ConvId(conversation.getId))
      if (conversation.isMemberOfConversation && permissionToAdd) {
        footerMenu.setLeftActionText(getString(R.string.glyph__add_people))
        footerMenu.setRightActionText(getString(R.string.glyph__more))
        footerMenu.setLeftActionLabelText(getString(R.string.conversation__action__add_people))
      } else {
        footerMenu.setLeftActionText("")
        footerMenu.setRightActionText(getString(R.string.glyph__more))
        footerMenu.setLeftActionLabelText("")
      }
    }

    footerMenu.setCallback(new FooterMenuCallback() {
      override def onLeftActionClicked(): Unit = {
        if (userRequester eq IConnectStore.UserRequester.POPOVER) {
          val user = getStoreFactory.singleParticipantStore.getUser
          if (user.isMe) {
            getControllerFactory.getConversationScreenController.hideParticipants(true, false)
            // Go to conversation with this user
            getControllerFactory.getPickUserController.hidePickUserWithoutAnimations(getContainer.getCurrentPickerDestination)
            inject(classOf[ConversationController]).selectConv(new ConvId(user.getConversation.getId), ConversationChangeRequester.CONVERSATION_LIST)
            return
          }
        }
        val permissionToAdd = getActivity.asInstanceOf[BaseActivity].injectJava(classOf[UserAccountsController]).hasAddConversationMemberPermission(new ConvId(conversation.getId))
        if (!conversation.isMemberOfConversation || !permissionToAdd) return
        getControllerFactory.getConversationScreenController.addPeopleToConversation()
      }

      override def onRightActionClicked(): Unit = {
        getStoreFactory.networkStore.doIfHasInternetOrNotifyUser(new NetworkAction() {
          override def execute(networkMode: NetworkMode): Unit = {
            if (!conversation.isMemberOfConversation) return
            if (userRequester eq IConnectStore.UserRequester.POPOVER) {
              val otherUser = conversation.getOtherParticipant
              getContainer.toggleBlockUser(otherUser, otherUser.getConnectionStatus ne User.ConnectionStatus.BLOCKED)
            } else getControllerFactory.getConversationScreenController.showConversationMenu(IConversationScreenController.CONVERSATION_DETAILS, new ConvId(conversation.getId))
          }

          override def onNoNetwork(): Unit = {
            ViewUtils.showAlertDialog(getActivity, R.string.alert_dialog__no_network__header, R.string.leave_conversation_failed__message, R.string.alert_dialog__confirmation, null, true)
          }
        })
      }
    })
  }

  override def participantsUpdated(participants: UsersList): Unit = {
    participantsAdapter.notifyDataSetChanged()
  }

  override def otherUserUpdated(otherUser: User): Unit = {
    if (otherUser == null || getView == null) return
    participantsAdapter.notifyDataSetChanged()
    imageAssetImageView.setVisibility(View.VISIBLE)
    imageAssetImageView.connectImageAsset(otherUser.getPicture)
    otherUser.getConnectionStatus match {
      case User.ConnectionStatus.BLOCKED =>
        footerMenu.setVisibility(View.GONE)
        unblockButton.setVisibility(View.VISIBLE)
        unblockButton.setOnClickListener(new View.OnClickListener() {
          override def onClick(v: View): Unit = {
            otherUser.unblock
          }
        })
      case _ =>
        unblockButton.setVisibility(View.GONE)
        unblockButton.setOnClickListener(null)
        footerMenu.setVisibility(View.VISIBLE)
    }
  }

  override def onAccentColorHasChanged(sender: Any, color: Int): Unit = {
    unblockButton.setAccentColor(color)
  }

  override def onConnectUserUpdated(user: User, usertype: IConnectStore.UserRequester): Unit = {
    if ((usertype ne userRequester) || user == null) return
    imageAssetImageView.setVisibility(View.VISIBLE)
    imageAssetImageView.connectImageAsset(user.getPicture)
    footerMenu.setVisibility(View.VISIBLE)
    topBorder.setVisibility(View.INVISIBLE)
    inject(classOf[ConversationController]).withCurrentConv(new Callback[ConversationData]() {
      override def callback(conv: ConversationData): Unit = {
        if (conv.convType eq IConversation.Type.ONE_TO_ONE) if (user.isMe) {
          footerMenu.setLeftActionText(getString(R.string.glyph__people))
          footerMenu.setLeftActionLabelText(getString(R.string.popover__action__profile))
          footerMenu.setRightActionText("")
          footerMenu.setRightActionLabelText("")
        } else {
          footerMenu.setLeftActionText(getString(R.string.glyph__add_people))
          footerMenu.setLeftActionLabelText(getString(R.string.conversation__action__create_group))
          footerMenu.setRightActionText(getString(R.string.glyph__block))
          footerMenu.setRightActionLabelText(getString(R.string.popover__action__block))
        } else if (user.isMe) {
          footerMenu.setLeftActionText(getString(R.string.glyph__people))
          footerMenu.setLeftActionLabelText(getString(R.string.popover__action__profile))
          footerMenu.setRightActionText(getString(R.string.glyph__minus))
          footerMenu.setRightActionLabelText("")
        } else {
          footerMenu.setLeftActionText(getString(R.string.glyph__conversation))
          footerMenu.setLeftActionLabelText(getString(R.string.popover__action__open))
          footerMenu.setRightActionText(getString(R.string.glyph__minus))
          footerMenu.setRightActionLabelText(getString(R.string.popover__action__remove))
        }

        footerMenu.setCallback(new FooterMenuCallback() {
          override def onLeftActionClicked(): Unit = {
            if (user.isMe || (conv.convType ne IConversation.Type.ONE_TO_ONE)) {
              getControllerFactory.getConversationScreenController.hideParticipants(true, false)
              getControllerFactory.getPickUserController.hidePickUserWithoutAnimations(getContainer.getCurrentPickerDestination)
              inject(classOf[ConversationController]).selectConv(new ConvId(user.getConversation.getId), ConversationChangeRequester.CONVERSATION_LIST)
            } else getControllerFactory.getConversationScreenController.addPeopleToConversation()
          }

          override def onRightActionClicked(): Unit = {
            if (conv.convType eq IConversation.Type.ONE_TO_ONE) if (!user.isMe) getContainer.toggleBlockUser(user, user.getConnectionStatus ne User.ConnectionStatus.BLOCKED)
            else getStoreFactory.networkStore.doIfHasInternetOrNotifyUser(new NetworkAction() {
              override def execute(networkMode: NetworkMode): Unit = {
                if (user.isMe) showLeaveConfirmation(conv.id)
                else getContainer.showRemoveConfirmation(user)
              }

              override def onNoNetwork(): Unit = {
                if (user.isMe) ViewUtils.showAlertDialog(getActivity, R.string.alert_dialog__no_network__header, R.string.leave_conversation_failed__message, R.string.alert_dialog__confirmation, null, true)
                else ViewUtils.showAlertDialog(getActivity, R.string.alert_dialog__no_network__header, R.string.remove_from_conversation__no_network__message, R.string.alert_dialog__confirmation, null, true)
              }
            })
          }
        })
      }
    })
  }

  override def onInviteRequestSent(conversation: IConversation): Unit = {
  }

  private def showLeaveConfirmation(convId: ConvId) = {
    val conversationController = inject(classOf[ConversationController])
    val callback = new TwoButtonConfirmationCallback() {
      override def positiveButtonClicked(checkboxIsSelected: Boolean): Unit = {
        if (getStoreFactory == null || getControllerFactory == null || getStoreFactory.isTornDown || getControllerFactory.isTornDown) return
        conversationController.leave(convId)
        if (LayoutSpec.isTablet(getActivity)) getControllerFactory.getConversationScreenController.hideParticipants(false, true)
      }

      override def negativeButtonClicked(): Unit = {}

      override def onHideAnimationEnd(confirmed: Boolean, canceled: Boolean, checkboxIsSelected: Boolean): Unit = {}
    }

    val header = getString(R.string.confirmation_menu__meta_remove)
    val text = getString(R.string.confirmation_menu__meta_remove_text)
    val confirm = getString(R.string.confirmation_menu__confirm_leave)
    val cancel = getString(R.string.confirmation_menu__cancel)
    val checkboxLabel = getString(R.string.confirmation_menu__delete_conversation__checkbox__label)
    val request = new ConfirmationRequest.Builder().withHeader(header).withMessage(text).withPositiveButton(confirm).withNegativeButton(cancel).withConfirmationCallback(callback).withCheckboxLabel(checkboxLabel).withWireTheme(getActivity.asInstanceOf[BaseActivity].injectJava(classOf[ThemeController]).getThemeDependentOptionsTheme).withCheckboxSelectedByDefault.build
    getControllerFactory.getConfirmationController.requestConfirmation(request, IConfirmationController.PARTICIPANTS)
    val ctrl = inject(classOf[SoundController])
    if (ctrl != null) ctrl.playAlert()
  }

}

object ParticipantBodyFragment {
  val TAG: String = classOf[ParticipantBodyFragment].getName
  private val ARG_USER_REQUESTER = "ARG_USER_REQUESTER"

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  //  Lifecycle
  //
  //////////////////////////////////////////////////////////////////////////////////////////
  def newInstance(userRequester: IConnectStore.UserRequester): ParticipantBodyFragment = {
    val participantBodyFragment = new ParticipantBodyFragment
    val args = new Bundle
    args.putSerializable(ARG_USER_REQUESTER, userRequester)
    participantBodyFragment.setArguments(args)
    participantBodyFragment
  }

  //  Notifications
  //  Conversation Manager Notifications
  //  Event listeners
  //  AccentColorObserver
  trait Container {
    def onClickedEmptyBackground(): Unit

    def toggleBlockUser(otherUser: User, block: Boolean): Unit

    def showRemoveConfirmation(user: User): Unit

    def getCurrentPickerDestination: IPickUserController.Destination
  }

}
