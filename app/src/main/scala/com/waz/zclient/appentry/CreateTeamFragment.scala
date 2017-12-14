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
package com.waz.zclient.appentry

import android.os.Bundle
import android.view.View.{OnClickListener, OnLayoutChangeListener}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.FrameLayout
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.service.ZMessaging
import com.waz.zclient.appentry.CreateTeamFragment._
import com.waz.zclient.appentry.controllers.AppEntryController
import com.waz.zclient.appentry.controllers.AppEntryController._
import com.waz.zclient.appentry.scenes._
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.ui.text.GlyphTextView
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.utils.{ContextUtils, DefaultTransition}
import com.waz.zclient.{FragmentHelper, OnBackPressedListener, R}

class CreateTeamFragment extends BaseFragment[Container] with FragmentHelper with OnBackPressedListener {

  private lazy val appEntryController = inject[AppEntryController]

  private var previousStage = Option.empty[AppEntryStage]
  private var lastKeyboardHeight = 0

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.app_entry_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {

    val container = findById[FrameLayout](R.id.container)
    val closeButton = findById[GlyphTextView](R.id.close_button)

    appEntryController.entryStage.onUi { state =>
      val inflator = LayoutInflater.from(getActivity)
      val viewHolder = state match {
        case NoAccountState(FirstScreen) => FirstScreenViewHolder(inflator.inflate(R.layout.app_entry_scene, null, false))(getContext, this, injector)
        case NoAccountState(RegisterTeamScreen) => TeamNameViewHolder(inflator.inflate(R.layout.create_team_name_scene, null, false))(getContext, this, injector)
        case SetTeamEmail => SetEmailViewHolder(inflator.inflate(R.layout.set_email_scene, null, false))(getContext, this, injector)
        case VerifyTeamEmail => VerifyEmailViewHolder(inflator.inflate(R.layout.verify_email_scene, null, false))(getContext, this, injector)
        case SetUsersNameTeam => SetNameViewHolder(inflator.inflate(R.layout.set_name_scene, null, false))(getContext, this, injector)
        case SetPasswordTeam => SetPasswordViewHolder(inflator.inflate(R.layout.set_password_scene, null, false))(getContext, this, injector)
        case SetUsernameTeam => SetUsernameViewHolder(inflator.inflate(R.layout.set_username_scene, null, false))(getContext, this, injector)
        case _ => EmptyViewHolder(new View(getContext))(getContext, this, injector)
      }

      val forward = previousStage.fold(true)(_.depth < state.depth)
      val sameDepth = previousStage.fold(false)(_.depth == state.depth)
      val transition = DefaultTransition()

      val previousViews = (0 until container.getChildCount).map(container.getChildAt)
      previousViews.foreach { pv =>
        if (!sameDepth)
          transition.outAnimation(pv, container, forward = forward).withEndAction(new Runnable {
            override def run(): Unit = container.removeView(pv)
          }).start()
        else
          container.removeView(pv)
      }

      container.addView(viewHolder.root)
      if (previousViews.nonEmpty && !sameDepth)
        transition.inAnimation(viewHolder.root, container, forward = forward).start()
      viewHolder.onCreate()

      previousStage = Some(state)

      if (state != NoAccountState(FirstScreen) && viewHolder.root.isInstanceOf[ViewGroup]) {
        setKeyboardAnimation(viewHolder.root.asInstanceOf[ViewGroup])
      }
    }

    ZMessaging.currentAccounts.loggedInAccounts.map(_.nonEmpty).zip(appEntryController.entryStage).onUi {
      case (true, state) if state != SetUsernameTeam && state != TeamSetPicture => closeButton.setVisibility(View.VISIBLE)
      case _ => closeButton.setVisibility(View.GONE)
    }

    closeButton.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = getContainer.abortAddAccount()
    })
  }

  def setKeyboardAnimation(view: ViewGroup): Unit = {
    implicit val ctx = getContext
    view.getLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    view.addOnLayoutChangeListener(new OnLayoutChangeListener {
      override def onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int): Unit = {
        val softKeysHeight = KeyboardUtils.getSoftButtonsBarHeight(getActivity)
        val kHeight = KeyboardUtils.getKeyboardHeight(view)
        val padding = ContextUtils.getDimenPx(R.dimen.app_entry_keyboard_content_padding)
        val screenHeight = ctx.getResources.getDisplayMetrics.heightPixels - ContextUtils.getStatusBarHeight(ctx) - softKeysHeight

        if (v.getTranslationY == 0 && lastKeyboardHeight > 0) {
          v.setTranslationY(screenHeight - lastKeyboardHeight - padding - v.getHeight + 2 * softKeysHeight)
        } else if (v.getTranslationY == 0) {
          v.setTranslationY(screenHeight / 2 - v.getHeight / 2)
        } else if (kHeight - softKeysHeight > 0) {
          lastKeyboardHeight = kHeight
          v.animate()
            .translationY(screenHeight - kHeight - padding - v.getHeight + 2 * softKeysHeight)
            .setDuration(ContextUtils.getInt(R.integer.wire__animation__delay__short))
        } else {
          v.animate()
            .translationY(screenHeight / 2 - v.getHeight / 2)
            .setDuration(ContextUtils.getInt(R.integer.wire__animation__delay__short))
        }
      }
    })
  }

  override def onBackPressed(): Boolean = {
    if (appEntryController.entryStage.currentValue.exists(_.isInstanceOf[NoAccountState]) && ZMessaging.currentAccounts.loggedInAccounts.currentValue.exists(_.nonEmpty))
      false
    else if (appEntryController.entryStage.currentValue.exists(_ != NoAccountState(FirstScreen))) {
      appEntryController.createTeamBack()
      true
    } else
      false
  }
}

object CreateTeamFragment {
  val TAG: String = classOf[CreateTeamFragment].getName

  def newInstance = new CreateTeamFragment

  trait Container {
    def abortAddAccount(): Unit
  }
}
