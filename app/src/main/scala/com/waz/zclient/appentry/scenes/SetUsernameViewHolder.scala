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
import android.view.View
import com.waz.api.impl.ErrorResponse
import com.waz.threading.Threading
import com.waz.utils.events.EventContext
import com.waz.zclient._
import com.waz.zclient.common.views.InputBox
import com.waz.zclient.common.views.InputBox.UsernameValidator
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.utils._

case class SetUsernameViewHolder(root: View)(implicit val context: Context, eventContext: EventContext, injector: Injector) extends ViewHolder with Injectable {

  private val appEntryController = inject[AppEntryController]

  lazy val inputField = root.findViewById[InputBox](R.id.input_field)

  def onCreate(): Unit = {
    inputField.setValidator(UsernameValidator)
    inputField.editText.setText(appEntryController.teamUserUsername)
    inputField.editText.addTextListener(appEntryController.teamUserUsername = _)
    inputField.editText.requestFocus()
    KeyboardUtils.showKeyboard(context.asInstanceOf[Activity])
    //TODO: do the checks on change?
    inputField.setOnClick( text => appEntryController.setUsername(text).map {
      case Right(ErrorResponse(409, _, "handle-exists")) =>
        Some(ContextUtils.getString(R.string.pref__account_action__dialog__change_username__error_already_taken))
      case Right(_) =>
        Some(ContextUtils.getString(R.string.pref__account_action__dialog__change_username__error_unknown))
      case _ => None
    } (Threading.Ui))
  }
}
