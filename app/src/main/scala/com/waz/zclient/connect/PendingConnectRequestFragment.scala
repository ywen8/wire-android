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
import com.waz.zclient.core.stores.connect.IConnectStore.UserRequester
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
  private lazy val footerMenuVisibility = userConnection.collect {
    case ConnectionStatus.IGNORED | ConnectionStatus.PENDING_FROM_USER => View.VISIBLE
    case ConnectionStatus.PENDING_FROM_OTHER if userRequester == UserRequester.PARTICIPANTS => View.VISIBLE
  }
  private lazy val footerMenuRightActionText = userConnection.collect {
    case ConnectionStatus.PENDING_FROM_OTHER if userRequester == UserRequester.PARTICIPANTS =>
      getString(R.string.glyph__minus)
  }
  private lazy val footerMenuRightActionLabelText = userConnection.collect {
    case ConnectionStatus.IGNORED => ""
  }
  private lazy val footerMenuLeftActionText = userConnection.collect {
    case ConnectionStatus.IGNORED => getString(R.string.glyph__plus)
  }
  private lazy val footerMenuLeftActionLabelText = userConnection.collect {
    case ConnectionStatus.IGNORED => getString(R.string.send_connect_request__connect_button__text)
  }
  private lazy val userNameView = returning(view[TypefaceTextView](R.id.user_name)) { vh =>
    userDisplayName.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val userHandleView = returning(view[TypefaceTextView](R.id.user_handle)) { vh =>
    userHandle.onUi(t => vh.foreach(_.setText(t)))
  }
  private lazy val footerMenu = returning(view[FooterMenu](R.id.fm__footer)) { vh =>
    footerMenuVisibility.onUi { visibility => vh.foreach(_.setVisibility(visibility)) }
    footerMenuRightActionText.onUi { text => vh.foreach(_.setRightActionText(text)) }
    footerMenuRightActionLabelText.onUi { text => vh.foreach(_.setRightActionLabelText(text)) }
    footerMenuLeftActionText.onUi { text => vh.foreach(_.setLeftActionText(text)) }
    footerMenuLeftActionLabelText.onUi { text => vh.foreach(_.setLeftActionLabelText(text)) }
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

    footerMenu.setCallback(new FooterMenuCallback {
      override def onLeftActionClicked(): Unit = userConnection.head foreach {
        case ConnectionStatus.IGNORED |
             ConnectionStatus.PENDING_FROM_OTHER if userRequester == IConnectStore.UserRequester.PARTICIPANTS =>
          getContainer.onAcceptedConnectRequest(userId)
        case ConnectionStatus.PENDING_FROM_USER =>
          zms.head.map(_.connection.cancelConnection(userId)).foreach { _ =>
            getActivity.onBackPressed()
          }
        case _ =>
      }
      override def onRightActionClicked(): Unit = userConnection.head foreach {
        case ConnectionStatus.PENDING_FROM_OTHER if userRequester == IConnectStore.UserRequester.PARTICIPANTS =>
          getContainer.showRemoveConfirmation(userId)
        case _ =>
      }
    })

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
