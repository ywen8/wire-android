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
package com.waz.zclient.appentry.scenes

import android.content.Context
import android.support.transition.Scene
import android.view.View.OnClickListener
import android.view.{View, ViewGroup}
import com.waz.utils.events.EventContext
import com.waz.zclient._
import com.waz.zclient.controllers.SignInController
import com.waz.zclient.controllers.SignInController.{Login, Phone, Register, SignInMethod}
import com.waz.zclient.ui.text.{GlyphTextView, TypefaceTextView}

case class FirstScreenSceneController(container: ViewGroup)(implicit val context: Context, eventContext: EventContext, injector: Injector) extends SceneController with Injectable {

  val appEntryController = inject[AppEntryController]
  val signInController = inject[SignInController]

  val scene = Scene.getSceneForLayout(container, R.layout.app_entry_scene, context)
  val root = scene.getSceneRoot

  lazy val createTeamButton = root.findViewById[GlyphTextView](R.id.create_team_button)
  lazy val createTeamText = root.findViewById[TypefaceTextView](R.id.create_team_text)
  lazy val createAccountButton = root.findViewById[GlyphTextView](R.id.create_account_button)
  lazy val createAccountText = root.findViewById[TypefaceTextView](R.id.create_account_text)
  lazy val loginButton = root.findViewById[TypefaceTextView](R.id.login_button)

  def onCreate(): Unit = {
    Seq(createTeamButton, createTeamText, createAccountButton, createAccountText, loginButton)
      .foreach(_.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          v.getId match {
            case R.id.create_team_button | R.id.create_team_text =>
              appEntryController.createTeam()
            case R.id.create_account_button | R.id.create_account_text =>
              appEntryController.goToLoginScreen()
              signInController.uiSignInState ! SignInMethod(Register, Phone)
            case R.id.login_button =>
              appEntryController.goToLoginScreen()
              signInController.uiSignInState ! SignInMethod(Login, Phone)
            case _ =>
          }
        }
      }))
  }
}
