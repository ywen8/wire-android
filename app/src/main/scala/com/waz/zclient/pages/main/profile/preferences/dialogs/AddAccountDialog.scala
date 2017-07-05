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
package com.waz.zclient.pages.main.profile.preferences.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import com.waz.api.impl.EmailCredentials
import com.waz.model.EmailAddress
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.zclient.ui.text.TypefaceEditText
import com.waz.zclient.{FragmentHelper, R}

import scala.concurrent.Future

class AddAccountDialog extends DialogFragment with FragmentHelper {

  override def onCreateDialog(savedInstanceState: Bundle) = {
    val builder = new AlertDialog.Builder(getActivity)
    val inflater = getActivity.getLayoutInflater

    builder.setView(inflater.inflate(R.layout.add_account_dialog, null))
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, id: Int): Unit = {
          implicit val ec = Threading.Ui
          getDialog.findViewById(R.id.input_email).setVisibility(View.INVISIBLE)
          val email = getDialog.findViewById(R.id.input_email).asInstanceOf[TypefaceEditText].getText.toString
          val password = getDialog.findViewById(R.id.input_password).asInstanceOf[TypefaceEditText].getText.toString
          val creds = EmailCredentials(EmailAddress(email), Some(password))
          ZMessaging.currentAccounts.login(creds).flatMap{
            case Right(a) =>
              dismiss()
              ZMessaging.currentAccounts.switchAccount(a.id)
            case _ =>
              getDialog.findViewById(R.id.input_email).setVisibility(View.VISIBLE)
              Future.successful[Unit](())
          }
        }
      })
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        def onClick(dialog: DialogInterface, id: Int): Unit = {
          dismiss()
        }
      })
      .create
  }
}

object AddAccountDialog {
  val Tag = AddAccountDialog.getClass.getSimpleName
}
