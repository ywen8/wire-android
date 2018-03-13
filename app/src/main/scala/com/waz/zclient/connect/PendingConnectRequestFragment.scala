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
  private lazy val userConnection = userToConnect.map(_.connection)
  private lazy val userToConnectPicture: Signal[ImageSource] =
    userToConnect.map(_.picture).collect { case Some(p) => WireImage(p) }
  private lazy val userDisplayName = userToConnect.map(_.getDisplayName)
  private lazy val userHandle =
    userToConnect.map(user => StringUtils.formatHandle(user.handle.map(_.string).getOrElse("")))

  private lazy val userNameView = returning(view[TypefaceTextView](R.id.user_name)) { vh =>
    userDisplayName.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val userHandleView = returning(view[TypefaceTextView](R.id.user_handle)) { vh =>
    userHandle.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val imageViewProfile = view[ImageView](R.id.pending_connect)

  private lazy val footerMenu = returning(view[FooterMenu](R.id.fm__footer)) { vh =>

    def setFooterForOutgoingConnectRequest(footerMenu: FooterMenu, userId: UserId): Unit = {
      footerMenu.setVisibility(View.VISIBLE)
      footerMenu.setRightActionText("")
      footerMenu.setCallback(new FooterMenuCallback() {
        override def onLeftActionClicked(): Unit = {
          zms.head.map(_.connection.cancelConnection(userId)).foreach { _ =>
            getActivity.onBackPressed()
          }
        }
        override def onRightActionClicked(): Unit = ()
      })
    }

    def setFooterForIncomingConnectRequest(footerMenu: FooterMenu): Unit = {
      footerMenu.setVisibility(View.VISIBLE)
      footerMenu.setRightActionText(getString(R.string.glyph__minus))
      footerMenu.setCallback(new FooterMenuCallback() {
        override def onLeftActionClicked(): Unit = getContainer.onAcceptedConnectRequest(userId)
        override def onRightActionClicked(): Unit = getContainer.showRemoveConfirmation(userId)
      })
      footerMenu.setLeftActionText(getString(R.string.glyph__plus))
      footerMenu.setLeftActionLabelText(getString(R.string.send_connect_request__connect_button__text))
    }

    def setFooterForIgnoredConnectRequest(footerMenu: FooterMenu): Unit = {
      footerMenu.setVisibility(View.VISIBLE)
      footerMenu.setRightActionText("")
      footerMenu.setRightActionLabelText("")
      footerMenu.setCallback(new FooterMenuCallback() {
        override def onLeftActionClicked(): Unit = getContainer.onAcceptedConnectRequest(userId)
        override def onRightActionClicked(): Unit = ()
      })
      footerMenu.setLeftActionText(getString(R.string.glyph__plus))
      footerMenu.setLeftActionLabelText(getString(R.string.send_connect_request__connect_button__text))
    }

    userConnection.onUi { connection =>
      vh.foreach { v =>
        connection match {
          case ConnectionStatus.PENDING_FROM_OTHER if userRequester == IConnectStore.UserRequester.PARTICIPANTS =>
            setFooterForIncomingConnectRequest(v)
          case ConnectionStatus.IGNORED =>
            setFooterForIgnoredConnectRequest(v)
          case ConnectionStatus.PENDING_FROM_USER =>
            setFooterForOutgoingConnectRequest(v, userId)
          case _ =>
        }
      }
    }
  }

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
    userHandleView
    footerMenu

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

  }

}

object PendingConnectRequestFragment {
  val Tag: String = classOf[PendingConnectRequestFragment].getName
  val ArgUserId = "ARGUMENT_USER_ID"
  val ArgUserRequester = "ARGUMENT_USER_REQUESTER"

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
