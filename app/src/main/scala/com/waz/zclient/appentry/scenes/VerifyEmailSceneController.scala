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
import android.view.ViewGroup
import com.waz.threading.Threading
import com.waz.utils.events.EventContext
import com.waz.zclient.common.views.NumberCodeInput
import com.waz.zclient.{CreateAccountController, Injectable, Injector, R}

case class VerifyEmailSceneController(container: ViewGroup)(implicit val context: Context, eventContext: EventContext, injector: Injector) extends SceneController with Injectable {

  val createTeamController = inject[CreateAccountController]

  val scene = Scene.getSceneForLayout(container, R.layout.verify_email_scene, context)
  val root = scene.getSceneRoot

  lazy val codeField = root.findViewById[NumberCodeInput](R.id.input_field)

  def onCreate(): Unit = {
    codeField.codeInput.requestFocus()
    codeField.setOnCodeSet({ code =>
      createTeamController.setEmailVerificationCode(code).map {
        case Right(error) => Some(error.message)
        case _ => None
      } (Threading.Ui)
    })
  }
}
