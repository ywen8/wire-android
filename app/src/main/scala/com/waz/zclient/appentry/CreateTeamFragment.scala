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
import android.support.transition._
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.zclient.CreateAccountController._
import com.waz.zclient.appentry.CreateTeamFragment._
import com.waz.zclient.appentry.scenes.{SetEmailSceneController, TeamNameSceneController}
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.{CreateAccountController, FragmentHelper, R}

class CreateTeamFragment extends BaseFragment[Container] with FragmentHelper {

  lazy val createAccountController = inject[CreateAccountController]

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.app_entry_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {

    createAccountController.accountState.onUi { state =>
      val entryScene = state match {
        case SetTeamName => TeamNameSceneController(getView.asInstanceOf[ViewGroup])(getContext, this, injector)
        case SetEmail => SetEmailSceneController(getView.asInstanceOf[ViewGroup])(getContext, this, injector)
      }
      TransitionManager.go(entryScene.scene, new AutoTransition2())
      entryScene.onCreate()
    }

  }
}

object CreateTeamFragment {
  val TAG: String = classOf[CreateTeamFragment].getName

  def newInstance = new CreateTeamFragment

  trait Container
}

class AutoTransition2 extends TransitionSet {
  setOrdering(TransitionSet.ORDERING_TOGETHER)
  addTransition(new Fade(Fade.OUT)).addTransition(new ChangeBounds).addTransition(new Fade(Fade.IN))
}
