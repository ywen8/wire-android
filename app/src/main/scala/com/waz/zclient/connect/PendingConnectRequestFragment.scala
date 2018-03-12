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
import com.waz.api.User.ConnectionStatus
import com.waz.model.UserId
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.common.views.ImageAssetDrawable
import com.waz.zclient.common.views.ImageAssetDrawable.{RequestBuilder, ScaleType}
import com.waz.zclient.common.views.ImageController.{ImageSource, WireImage}
import com.waz.zclient.core.stores.connect.IConnectStore
import com.waz.zclient.messages.UsersController
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.connect.UserProfileContainer
import com.waz.zclient.pages.main.participants.ProfileAnimation
import com.waz.zclient.pages.main.participants.dialog.DialogLaunchMode
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.StringUtils
import com.waz.zclient.views.menus.{FooterMenu, FooterMenuCallback}
import com.waz.zclient.{FragmentHelper, R}

/**
  * Created by admin on 3/7/18.
  */

class PendingConnectRequestFragment extends BaseFragment[PendingConnectRequestFragment.Container]
  with FragmentHelper {

  import PendingConnectRequestFragment._
  import Threading.Implicits.Ui

  implicit def context: Context = getActivity

  private lazy val usersController  = inject[UsersController]
  private lazy val zms              = inject[Signal[ZMessaging]]

  private lazy val userId         = UserId(getArguments.getString(ArgUserId))
  private lazy val userRequester  = IConnectStore.UserRequester.valueOf(getArguments.getString(ArgUserRequester))

  private lazy val userToConnect  = usersController.user(userId)
  private lazy val userToConnectPicture: Signal[ImageSource] =
    userToConnect.map(_.picture).collect { case Some(p) => WireImage(p) }
  private lazy val userDisplayName = userToConnect.map(_.getDisplayName)
  private lazy val userHandle =
    userToConnect.map(user => StringUtils.formatHandle(user.handle.map(_.string).getOrElse("")))


  private var isShowingFooterMenu: Boolean = false

  private lazy val footerMenu = view[FooterMenu](R.id.fm__footer)
  private lazy val userNameView = returning(view[TypefaceTextView](R.id.user_name)) { vh =>
    userDisplayName.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val userHandleView = returning(view[TypefaceTextView](R.id.user_handle)) { vh =>
    userHandle.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val imageViewProfile = view[ImageView](R.id.pending_connect)

  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation = {
    def defaultAnimation = super.onCreateAnimation(transit, enter, nextAnim)
    def shownInConvList = {
      val launchMode = getControllerFactory.getConversationScreenController.getPopoverLaunchMode
      launchMode != DialogLaunchMode.AVATAR && launchMode != DialogLaunchMode.COMMON_USER
    }
    def isConvRequester = {
      val userRequester = IConnectStore.UserRequester.valueOf(getArguments.getString(ArgUserRequester))
      userRequester == IConnectStore.UserRequester.CONVERSATION
    }

    if (shownInConvList || isConvRequester || nextAnim != 0) defaultAnimation
    else {
      val centerX = getOrientationIndependentDisplayWidth(getActivity) / 2
      val centerY = getOrientationIndependentDisplayHeight(getActivity) / 2
      val duration =
        if (enter) getInt(R.integer.open_profile__animation_duration)
        else getInt(R.integer.close_profile__animation_duration)
      val delay =
        if (enter) getInt(R.integer.open_profile__delay)
        else 0

      new ProfileAnimation(enter, duration, delay, centerX, centerY)
    }
  }

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_connect_request_pending, viewContainer, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    if (savedInstanceState != null) {
      isShowingFooterMenu = savedInstanceState.getBoolean(StateIsShowingFooterMenu)
    } else {
      isShowingFooterMenu = false
    }

    userHandleView

    val assetDrawable = new ImageAssetDrawable(
      userToConnectPicture,
      scaleType = ScaleType.CenterInside,
      request = RequestBuilder.Round
    )
    imageViewProfile.foreach(_.setImageDrawable(assetDrawable))

    val backgroundContainer = findById[View](R.id.ll__pending_connect__background_container)
    val popoverLaunchMode = getControllerFactory.getConversationScreenController.getPopoverLaunchMode
    if (popoverLaunchMode == DialogLaunchMode.AVATAR || popoverLaunchMode == DialogLaunchMode.COMMON_USER) {
      backgroundContainer.setClickable(true)
    } else {
      backgroundContainer.setBackgroundColor(Color.TRANSPARENT)
    }

    userNameView.foreach { v =>
      val paddingTop =
        if (userRequester == IConnectStore.UserRequester.PARTICIPANTS) 0
        else getDimenPx(R.dimen.wire__padding__regular)

      v.setPaddingRelative(0, paddingTop, 0, 0)
    }

    userToConnect.onUi {user =>
      user.connection match {
        case ConnectionStatus.PENDING_FROM_OTHER =>
          setFooterForIncomingConnectRequest(user.id)
        case ConnectionStatus.IGNORED =>
          setFooterForIgnoredConnectRequest(user.id)
        case ConnectionStatus.PENDING_FROM_USER =>
          setFooterForOutgoingConnectRequest(user.id)
        case _ =>
      }
    }

  }

  override def onSaveInstanceState(outState: Bundle): Unit = {
    // Save if footer menu was visible -> used to toggle accept & footer menu in incoming connect request opened from group participants
    outState.putBoolean(StateIsShowingFooterMenu, isShowingFooterMenu)
    super.onSaveInstanceState(outState)
  }

  private def setFooterForOutgoingConnectRequest(userId: UserId): Unit = footerMenu.foreach { v =>
    v.setVisibility(View.VISIBLE)
    isShowingFooterMenu = true
    v.setRightActionText("")
    v.setCallback(new FooterMenuCallback() {
      override def onLeftActionClicked(): Unit = {
        zms.head.map(_.connection.cancelConnection(userId)).foreach { _ =>
          getActivity.onBackPressed()
        }
      }

      override def onRightActionClicked(): Unit = {
      }
    })
  }

  private def setFooterForIncomingConnectRequest(userId: UserId): Unit =
    if (userRequester == IConnectStore.UserRequester.PARTICIPANTS) {
      footerMenu.foreach { v =>
        v.setVisibility(View.VISIBLE)
        v.setRightActionText(getString(R.string.glyph__minus))
        v.setCallback(new FooterMenuCallback() {
          override def onLeftActionClicked(): Unit = {
            getContainer.onAcceptedConnectRequest(userId)
          }

          override def onRightActionClicked(): Unit = {
            getContainer.showRemoveConfirmation(userId)
          }
        })
        v.setLeftActionText(getString(R.string.glyph__plus))
        v.setLeftActionLabelText(getString(R.string.send_connect_request__connect_button__text))
      }
    }

  private def setFooterForIgnoredConnectRequest(userId: UserId): Unit = footerMenu.foreach { v =>
    v.setVisibility(View.VISIBLE)
    v.setRightActionText("")
    v.setRightActionLabelText("")
    v.setCallback(new FooterMenuCallback() {
      override def onLeftActionClicked(): Unit = {
        getContainer.onAcceptedConnectRequest(userId)
      }

      override def onRightActionClicked(): Unit = {
      }
    })
    v.setLeftActionText(getString(R.string.glyph__plus))
    v.setLeftActionLabelText(getString(R.string.send_connect_request__connect_button__text))
  }

}

object PendingConnectRequestFragment {
  val Tag: String = classOf[PendingConnectRequestFragment].getName
  val ArgUserId = "ARGUMENT_USER_ID"
  val ArgUserRequester = "ARGUMENT_USER_REQUESTER"
  val StateIsShowingFooterMenu = "STATE_IS_SHOWING_FOOTER_MENU"

  def newInstance(userId: UserId, userRequester: IConnectStore.UserRequester): PendingConnectRequestFragment =
    returning(new PendingConnectRequestFragment)(fragment =>
      fragment.setArguments(
        returning(new Bundle) { args =>
          args.putString(ArgUserId, userId.str)
          args.putString(ArgUserRequester, userRequester.toString)
        }
      )
    )

  trait Container extends UserProfileContainer {
    def onAcceptedConnectRequest(userId: UserId): Unit
  }

}
