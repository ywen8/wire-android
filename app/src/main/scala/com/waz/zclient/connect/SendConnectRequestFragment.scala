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
package com.waz.zclient.connect

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.animation.Animation
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.ImageView
import com.waz.ZLog.ImplicitTag._
import com.waz.model.UserId
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.common.controllers.UserAccountsController
import com.waz.zclient.common.controllers.global.AccentColorController
import com.waz.zclient.common.views.ImageAssetDrawable
import com.waz.zclient.common.views.ImageAssetDrawable.{RequestBuilder, ScaleType}
import com.waz.zclient.common.views.ImageController.{ImageSource, WireImage}
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.core.stores.connect.IConnectStore
import com.waz.zclient.messages.UsersController
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.connect.UserProfileContainer
import com.waz.zclient.pages.main.participants.ProfileAnimation
import com.waz.zclient.pages.main.participants.dialog.DialogLaunchMode
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.{StringUtils, ViewUtils}
import com.waz.zclient.views.images.ImageAssetImageView
import com.waz.zclient.views.menus.{FooterMenu, FooterMenuCallback}
import com.waz.zclient.{FragmentHelper, R}
import timber.log.Timber

/**
  * Created by admin on 3/6/18.
  */

object SendConnectRequestFragment {
  val Tag: String = classOf[SendConnectRequestFragment].getName
  val ArgumentUserId = "ARGUMENT_USER_ID"
  val ArgumentUserRequester = "ARGUMENT_USER_REQUESTER"

  def newInstance(userId: String, userRequester: IConnectStore.UserRequester): SendConnectRequestFragment = {
    val newFragment = new SendConnectRequestFragment
    val args = new Bundle
    args.putString(ArgumentUserId, userId)
    args.putString(ArgumentUserRequester, userRequester.toString)
    newFragment.setArguments(args)
    newFragment
  }

  trait Container extends UserProfileContainer {
    def onConnectRequestWasSentToUser(): Unit
    override def showRemoveConfirmation(userId: UserId): Unit
  }

}

class SendConnectRequestFragment extends BaseFragment[SendConnectRequestFragment.Container]
  with FragmentHelper {

  import Threading.Implicits.Ui
  import SendConnectRequestFragment._

  implicit def context: Context = getActivity

  private lazy val userToConnectId = UserId(getArguments.getString(ArgumentUserId))
  private lazy val userRequester = IConnectStore.UserRequester.valueOf(getArguments.getString(ArgumentUserRequester))

  private lazy val usersController = inject[UsersController]
  private lazy val userAccountsController = inject[UserAccountsController]
  private lazy val conversationController = inject[ConversationController]
  private lazy val zms = inject[Signal[ZMessaging]]

  private lazy val accentColor = inject[AccentColorController].accentColor
  private lazy val userToConnect = usersController.user(userToConnectId)
  private lazy val userDisplayName = userToConnect.map(user => { Timber.d("We have user: " + user); user.getDisplayName })
  private lazy val userName = userToConnect.map(user => StringUtils.formatHandle(user.handle.map(_.string).getOrElse("")))
  private lazy val userPicture: Signal[ImageSource] = userToConnect.map(_.picture).collect { case Some(p) => WireImage(p) }
  private lazy val hasRemoveConvMemberPermForCurrentConv = for {
    convId <- conversationController.currentConvId
    permission <- userAccountsController.hasRemoveConversationMemberPermission(convId)
  } yield permission

  private lazy val connectButton = returning(view[ZetaButton](R.id.zb__send_connect_request__connect_button)) { vh =>
    accentColor.onUi { color => vh.foreach(_.setAccentColor(color.getColor)) }
  }
  private lazy val footerMenu = returning(view[FooterMenu](R.id.fm__footer)) { vh =>
    hasRemoveConvMemberPermForCurrentConv.onUi { hasPermission =>
      val removeConvMemberFeatureEnabled = hasPermission &&
        userRequester == IConnectStore.UserRequester.PARTICIPANTS

      val text =
        if (removeConvMemberFeatureEnabled) getString(R.string.glyph__minus)
        else null
      vh.foreach(_.setRightActionText(text))

      val callback: FooterMenuCallback = new FooterMenuCallback() {
        override def onLeftActionClicked(): Unit = {
          showConnectButtonInsteadOfFooterMenu()
        }
        override def onRightActionClicked(): Unit = {
          if (removeConvMemberFeatureEnabled)
            getContainer.showRemoveConfirmation(userToConnectId)
        }
      }
      vh.foreach(_.setCallback(callback))
    }
  }
  private lazy val imageViewProfile = view[ImageView](R.id.iv__send_connect)
  private lazy val userNameView = returning(view[TypefaceTextView](R.id.user_name)) { vh =>
    userDisplayName.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val userHandleView = returning(view[TypefaceTextView](R.id.user_handle)) { vh =>
    userName.onUi(t => vh.foreach(_.setText(t)))
  }

  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation = {
    var animation = super.onCreateAnimation(transit, enter, nextAnim)
    val popoverLaunchMode = getControllerFactory.getConversationScreenController.getPopoverLaunchMode
    if (popoverLaunchMode != DialogLaunchMode.AVATAR && popoverLaunchMode != DialogLaunchMode.COMMON_USER) {
      val centerX = getOrientationIndependentDisplayWidth(getActivity) / 2
      val centerY = getOrientationIndependentDisplayHeight(getActivity) / 2
      var duration = 0
      var delay = 0
      if (nextAnim != 0) {
        if (enter) {
          duration = getInt(R.integer.open_profile__animation_duration)
          delay = getInt(R.integer.open_profile__delay)
        } else {
          duration = getInt(R.integer.close_profile__animation_duration)
        }
        animation = new ProfileAnimation(enter, duration, delay, centerX, centerY)
      }
    }
    animation
  }

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_send_connect_request, viewContainer, false)
  }


  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    userNameView
    userHandleView

//    imageViewProfile.foreach(_.setDisplayType(ImageAssetImageView.DisplayType.CIRCLE))
//    imageViewProfile.foreach(_.setSaturation(0))

    val assetDrawable = new ImageAssetDrawable(
      userPicture,
      scaleType = ScaleType.CenterInside,
      request = RequestBuilder.Round
    )
    imageViewProfile.foreach(_.setImageDrawable(assetDrawable))

    val backgroundContainer = findById[View](R.id.background_container)
    backgroundContainer.setClickable(true)

    val popoverLaunchMode = getControllerFactory.getConversationScreenController.getPopoverLaunchMode
    if (userRequester == IConnectStore.UserRequester.PARTICIPANTS &&
      popoverLaunchMode != DialogLaunchMode.AVATAR &&
      popoverLaunchMode != DialogLaunchMode.COMMON_USER) {
      backgroundContainer.setBackgroundColor(Color.TRANSPARENT)
    }

    connectButton.setText(getString(R.string.send_connect_request__connect_button__text))
    footerMenu.setVisibility(View.GONE)

    if (userRequester == IConnectStore.UserRequester.PARTICIPANTS) {
      footerMenu.setVisibility(View.VISIBLE)
      connectButton.setVisibility(View.GONE)
    } else {
      footerMenu.setVisibility(View.GONE)
      connectButton.setVisibility(View.VISIBLE)
    }

    connectButton.onClick { _ =>
      for {
        uSelf <- usersController.selfUser.head
        uToConnect <- userToConnect.head
        message = getString(R.string.connect__message, uToConnect.name, uSelf.name)
        maybeConversation <- zms.head.flatMap(_.connection.connectToUser(userToConnectId, message, uToConnect.displayName))
      } yield maybeConversation.foreach { _ =>
        KeyboardUtils.hideKeyboard(getActivity)
        getContainer.onConnectRequestWasSentToUser()
      }
    }
  }

  override def onStop(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    super.onStop()
  }

  private def showConnectButtonInsteadOfFooterMenu(): Unit = {
    if (connectButton.getVisibility != View.VISIBLE) {
      footerMenu.foreach(_.setVisibility(View.GONE))
      connectButton.foreach { btn =>
        btn.setAlpha(0)
        btn.setVisibility(View.VISIBLE)
        ViewUtils.fadeInView(btn, getInt(R.integer.framework_animation_duration_long))
      }
    }
  }
}
