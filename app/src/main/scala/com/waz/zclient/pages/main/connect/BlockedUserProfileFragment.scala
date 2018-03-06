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
  *//**
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
package com.waz.zclient.pages.main.connect

import android.os.Bundle
import android.view.animation.Animation
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{FrameLayout, ImageView, LinearLayout}
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.model.{ConvId, UserData, UserId}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.common.controllers.ThemeController
import com.waz.zclient.common.controllers.global.AccentColorController
import com.waz.zclient.common.views.ImageAssetDrawable
import com.waz.zclient.common.views.ImageController.{ImageSource, WireImage}
import com.waz.zclient.core.stores.connect.IConnectStore
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.connect.BlockedUserProfileFragment._
import com.waz.zclient.pages.main.participants.ProfileAnimation
import com.waz.zclient.pages.main.participants.dialog.DialogLaunchMode
import com.waz.zclient.ui.animation.fragment.FadeAnimation
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.utils.{ContextUtils, StringUtils}
import com.waz.zclient.views.menus.{FooterMenu, FooterMenuCallback}
import com.waz.zclient.{FragmentHelper, R}
import com.waz.zclient.utils.RichView

object BlockedUserProfileFragment {
  val Tag: String = ZLog.ImplicitTag.implicitLogTag
  val ARGUMENT_USER_ID = "ARGUMENT_USER_ID"
  val ARGUMENT_USER_REQUESTER = "ARGUMENT_USER_REQUESTER"
  val STATE_IS_SHOWING_FOOTER_MENU = "STATE_IS_SHOWING_FOOTER_MENU"

  def newInstance(userId: String, userRequester: IConnectStore.UserRequester): BlockedUserProfileFragment = {
    val newFragment = new BlockedUserProfileFragment
    val args = new Bundle
    args.putString(ARGUMENT_USER_REQUESTER, userRequester.toString)
    args.putString(ARGUMENT_USER_ID, userId)
    newFragment.setArguments(args)
    newFragment
  }

  trait Container extends UserProfileContainer {
    def onUnblockedUser(restoredConversationWithUser: ConvId): Unit
  }

}

class BlockedUserProfileFragment extends BaseFragment[BlockedUserProfileFragment.Container] with FragmentHelper {

  private implicit lazy val ctx = getContext
  private lazy val zms = inject[Signal[ZMessaging]]
  private lazy val accentColor = inject[AccentColorController].accentColor

  private val userId = Signal[UserId]()
  private lazy val user = for {
    zms <- zms
    uId <- userId
    user <- zms.usersStorage.signal(uId)
  } yield user

  private lazy val pictureSignal: Signal[ImageSource] = user.map(_.picture).collect { case Some(pic) => WireImage(pic) }
  private lazy val profileDrawable = new ImageAssetDrawable(pictureSignal, ImageAssetDrawable.ScaleType.CenterInside, ImageAssetDrawable.RequestBuilder.Round)

  private var userRequester = Option.empty[IConnectStore.UserRequester]
  private var isShowingFooterMenu = false
  private var goToConversationWithUser = false

  private lazy val unblockButton = view[ZetaButton](R.id.zb__connect_request__unblock_button)
  private lazy val cancelButton = view[ZetaButton](R.id.zb__connect_request__ignore_button)
  private lazy val smallUnblockButton = view[ZetaButton](R.id.zb__connect_request__accept_button)
  private lazy val unblockMenu = view[LinearLayout](R.id.ll__connect_request__accept_menu)
  private lazy val footerMenu = view[FooterMenu](R.id.fm__footer)
  private lazy val profileImageView = view[ImageView](R.id.blocked_user_picture)
  private lazy val backgroundContainer = view[FrameLayout](R.id.fl__blocked_user__background_container)
  private lazy val userNameView = view[TypefaceTextView](R.id.user_name)
  private lazy val userUsernameView = view[TypefaceTextView](R.id.user_handle)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    if (savedInstanceState != null) {
      userId ! UserId(savedInstanceState.getString(BlockedUserProfileFragment.ARGUMENT_USER_ID))
      userRequester = Option(IConnectStore.UserRequester.valueOf(savedInstanceState.getString(ARGUMENT_USER_REQUESTER)))
      isShowingFooterMenu = savedInstanceState.getBoolean(BlockedUserProfileFragment.STATE_IS_SHOWING_FOOTER_MENU)
    } else {
      userRequester = Option(IConnectStore.UserRequester.valueOf(getArguments.getString(ARGUMENT_USER_REQUESTER)))
      userId ! UserId(getArguments.getString(BlockedUserProfileFragment.ARGUMENT_USER_ID))
      isShowingFooterMenu = true
    }

    accentColor.onUi { color =>
      unblockButton.setIsFilled(true)
      unblockButton.setAccentColor(color.getColor)
      cancelButton.setIsFilled(false)
      cancelButton.setAccentColor(color.getColor)
      smallUnblockButton.setIsFilled(true)
      smallUnblockButton.setAccentColor(color.getColor)
    }

    user.onUi { userData =>
      userNameView.setText(userData.getDisplayName)
      userUsernameView.setText(userData.handle.map(h => StringUtils.formatHandle(h.string)).getOrElse(""))
      unblockMenu.setVisibility(View.GONE)
      if (userRequester.contains(IConnectStore.UserRequester.PARTICIPANTS)){
        setGroupConversationFooterMenu(userData)
        userNameView.setPaddingRelative(0, 0, 0, 0)
      } else {
        setRegularFooterMenu(userData)
        userNameView.setPaddingRelative(0, ContextUtils.getDimenPx(R.dimen.wire__padding__regular), 0, 0)
      }
    }
  }

  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation = {
    var animation = super.onCreateAnimation(transit, enter, nextAnim)
    if ((getControllerFactory.getConversationScreenController.getPopoverLaunchMode ne DialogLaunchMode.AVATAR) && (getControllerFactory.getConversationScreenController.getPopoverLaunchMode ne DialogLaunchMode.COMMON_USER)) {
      val centerX = ContextUtils.getOrientationIndependentDisplayWidth(getActivity) / 2
      val centerY = ContextUtils.getOrientationIndependentDisplayHeight(getActivity) / 2
      var duration = 0
      var delay = 0
      // Fade out animation when starting conversation directly with this user when unblocking
      if (!goToConversationWithUser || enter) if (nextAnim != 0) {
        if (enter) {
          duration = getResources.getInteger(R.integer.open_profile__animation_duration)
          delay = getResources.getInteger(R.integer.open_profile__delay)
        }
        else duration = getResources.getInteger(R.integer.close_profile__animation_duration)
        animation = new ProfileAnimation(enter, duration, delay, centerX, centerY)
      }
      else {
        goToConversationWithUser = false
        duration = getResources.getInteger(R.integer.framework_animation_duration_medium)
        animation = new FadeAnimation(duration, 1, 0)
      }
    }
    animation
  }

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_blocked_user_profile, viewContainer, false)


  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)

    profileImageView.setImageDrawable(profileDrawable)
    // Split Unblock / Cancel menu when opened from group conversation
    cancelButton.setText(getString(R.string.confirmation_menu__cancel))
    smallUnblockButton.setText(getString(R.string.connect_request__unblock__button__text))
    val unblockButtonColor = inject(classOf[ThemeController]).optionsDarkTheme.getTextColorPrimary
    unblockButton.setTextColor(unblockButtonColor)
    smallUnblockButton.setTextColor(unblockButtonColor)
    // Hide some views irrelevant for blocking
    footerMenu.setVisibility(View.GONE)
    unblockButton.setVisibility(View.GONE)

    backgroundContainer.setClickable(true)
  }

  override def onSaveInstanceState(outState: Bundle): Unit = {
    outState.putString(BlockedUserProfileFragment.ARGUMENT_USER_ID, userId.currentValue.map(_.str).orNull)
    userRequester.foreach(req => outState.putString(ARGUMENT_USER_REQUESTER, req.toString))
    outState.putBoolean(BlockedUserProfileFragment.STATE_IS_SHOWING_FOOTER_MENU, isShowingFooterMenu)
    super.onSaveInstanceState(outState)
  }

  private def setRegularFooterMenu(user: UserData) = {
    unblockButton.foreach(_.onClick(unblockUser(user)))
    unblockButton.setVisibility(View.VISIBLE)
    footerMenu.setVisibility(View.GONE)
    unblockMenu.setVisibility(View.GONE)
  }

  private def setGroupConversationFooterMenu(user: UserData) = {
    unblockButton.setVisibility(View.GONE)
    toggleUnblockAndFooterMenu(isShowingFooterMenu)
    footerMenu.setLeftActionLabelText(getString(R.string.connect_request__footer__blocked_label))
    footerMenu.setLeftActionText(getString(R.string.glyph__block))
    footerMenu.setRightActionText(getString(R.string.glyph__minus))
    footerMenu.setCallback(new FooterMenuCallback() {
      override def onLeftActionClicked(): Unit = toggleUnblockAndFooterMenu(false)
      override def onRightActionClicked(): Unit = getContainer.showRemoveConfirmation(user.id)
    })
    cancelButton.setEnabled(true)
    cancelButton.foreach(_.onClick(toggleUnblockAndFooterMenu(true)))
    smallUnblockButton.setEnabled(true)
    smallUnblockButton.foreach(_.onClick(unblockUser(user)))
  }

  private def toggleUnblockAndFooterMenu(showFooterMenu: Boolean) = {
    if (showFooterMenu) {
      footerMenu.setVisibility(View.VISIBLE)
      unblockMenu.setVisibility(View.GONE)
    } else {
      footerMenu.setVisibility(View.GONE)
      unblockMenu.setVisibility(View.VISIBLE)
    }
    isShowingFooterMenu = showFooterMenu
  }

  private def unblockUser(user: UserData) = {
    zms.head.map(_.connection.unblockConnection(user.id))(Threading.Background)
    goToConversationWithUser = true
    getContainer.onUnblockedUser(ConvId(user.id.str))
  }
}
