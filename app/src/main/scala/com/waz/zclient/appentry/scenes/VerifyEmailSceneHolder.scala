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

import android.app.Activity
import android.content.Context
import android.support.transition.Scene
import android.view.ViewGroup
import android.widget.Toast
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.EventContext
import com.waz.zclient._
import com.waz.zclient.common.views.NumberCodeInput
import com.waz.zclient.controllers.SignInController.{Email, Register, SignInMethod}
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.utils._

import scala.concurrent.Future

case class VerifyEmailSceneHolder(container: ViewGroup)(implicit val context: Context, eventContext: EventContext, injector: Injector) extends SceneHolder with Injectable {

  val appEntryController = inject[AppEntryController]

  val scene = Scene.getSceneForLayout(container, R.layout.verify_email_scene, context)
  val root = scene.getSceneRoot

  lazy val codeField = root.findViewById[NumberCodeInput](R.id.input_field)
  lazy val resend = root.findViewById[TypefaceTextView](R.id.resend_email)
  lazy val changeEmail = root.findViewById[TypefaceTextView](R.id.change_email)
  lazy val subtitle = root.findViewById[TypefaceTextView](R.id.subtitle)

  def onCreate(): Unit = {
    import Threading.Implicits.Ui

    ZMessaging.currentAccounts.getActiveAccount.map {
      _.flatMap(_.pendingEmail) match {
        case Some(email) =>
          subtitle.setText(ContextUtils.getString(R.string.teams_verify_email_subtitle, email.str))
        case _ =>
          subtitle.setText("")
      }
    }
    codeField.codeInput.setText(appEntryController.code)
    codeField.codeInput.addTextListener(appEntryController.code = _)
    codeField.codeInput.requestFocus()
    KeyboardUtils.showKeyboard(context.asInstanceOf[Activity])
    codeField.setOnCodeSet({ code =>
      for {
        _ <- setButtonsVisible(false)
        resp <- appEntryController.setEmailVerificationCode(code)
        _ <- setButtonsVisible(true)
      } yield resp match {
        case Right(error) =>
          val errorMessage = ContextUtils.getString(EntryError(error.code, error.label, SignInMethod(Register, Email)).bodyResource)
          Some(errorMessage)
        case _ =>
          appEntryController.code = ""
          None

      }
    })

    resend.onClick(appEntryController.resendTeamEmailVerificationCode() map {
      case Left(_) => Toast.makeText(context, "Email sent", Toast.LENGTH_SHORT).show()
      case Right(error) =>
        val errorMessage = ContextUtils.getString(EntryError(error.code, error.label, SignInMethod(Register, Email)).bodyResource)
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    })
    changeEmail.onClick(appEntryController.createTeamBack())
  }

  def setButtonsVisible(visible: Boolean) = Future.successful {
    resend.setVisible(visible)
    changeEmail.setVisible(visible)
  }
}
