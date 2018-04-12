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
package com.waz.zclient.appentry.fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.waz.api.impl.ErrorResponse
import com.waz.threading.Threading
import com.waz.zclient._
import com.waz.zclient.appentry.CreateTeamFragment
import com.waz.zclient.common.views.InputBox
import com.waz.zclient.common.views.InputBox.UsernameValidator
import com.waz.zclient.ui.utils.KeyboardUtils
import com.waz.zclient.utils._

case class SetUsernameViewHolder() extends CreateTeamFragment {

  lazy val inputField = view[InputBox](R.id.input_field)

  override val layoutId: Int = R.layout.set_username_scene

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    inputField.foreach { inputField =>
      inputField.setValidator(UsernameValidator)
      inputField.editText.setText(createTeamController.teamUserUsername)
      inputField.editText.addTextListener(createTeamController.teamUserUsername = _)
      inputField.startText.setText("@")
      inputField.editText.requestFocus()
      KeyboardUtils.showKeyboard(context.asInstanceOf[Activity])
      //TODO: do the checks on change?
      inputField.setOnClick( text => createTeamController.setUsername(text).map {
        case Left(ErrorResponse(409, _, "handle-exists")) =>
          Some(getString(R.string.pref__account_action__dialog__change_username__error_already_taken))
        case Left(_) =>
          Some(getString(R.string.pref__account_action__dialog__change_username__error_unknown))
        case _ => None
      } (Threading.Ui))
    }
  }
}
