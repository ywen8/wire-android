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
import android.text.InputType
import android.view.ViewGroup
import com.waz.threading.Threading
import com.waz.utils.events.EventContext
import com.waz.zclient._
import com.waz.zclient.appentry.AppEntryDialogs
import com.waz.zclient.common.views.InputBox
import com.waz.zclient.common.views.InputBox.PasswordValidator
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.utils._
import scala.concurrent.Future

case class SetPasswordSceneHolder(container: ViewGroup)(implicit val context: Context, eventContext: EventContext, injector: Injector) extends SceneHolder with Injectable {

  private val appEntryController = inject[AppEntryController]

  override val scene: Scene = Scene.getSceneForLayout(container, R.layout.set_password_scene, context)
  override val root: ViewGroup = scene.getSceneRoot

  lazy val inputField = root.findViewById[InputBox](R.id.input_field)

  def onCreate(): Unit = {
    import Threading.Implicits.Ui

    inputField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
    inputField.setValidator(PasswordValidator)
    inputField.editText.setText(appEntryController.password)
    inputField.editText.addTextListener(appEntryController.password = _)
    inputField.editText.requestFocus()
    KeyboardUtils.showKeyboard(context.asInstanceOf[Activity])
    inputField.setOnClick( text =>
      appEntryController.isAB.flatMap {
        case true =>
          AppEntryDialogs.showTermsAndConditions(context).flatMap {
            case true => appEntryController.setPassword(text).map {
              case Right(error) => Some(error.message)
              case _ => None
            }
            case false =>
              Future.successful(None)
          }
        case false =>
          appEntryController.setPassword(text).map {
            case Right(error) => Some(error.message)
            case _ => None
          }
      })
  }
}
