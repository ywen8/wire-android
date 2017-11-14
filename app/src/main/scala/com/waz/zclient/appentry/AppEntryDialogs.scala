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

import android.content.{Context, DialogInterface}
import android.support.v7.app.AlertDialog

import scala.concurrent.{Future, Promise}

object AppEntryDialogs {
  def showTermsAndConditions(context: Context): Future[Boolean] = {
    val dialogResult = Promise[Boolean]()
    val dialog = new AlertDialog.Builder(context)
      .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = dialogResult.success(true)
      })
      .setNegativeButton("CANCEL", new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = dialogResult.success(false)
      })
      .setNeutralButton("VIEW", new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = {
          //TODO: ??
        }
      })
      .setTitle("Terms of Service")
      .setMessage("You must accept to continue")
    dialog.show()
    dialogResult.future
  }

  def showNotificationsWarning(context: Context): Future[Boolean] = {
    val dialogResult = Promise[Boolean]()
    val dialog = new AlertDialog.Builder(context)
      .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = dialogResult.success(true)
      })
      .setNegativeButton("CANCEL", new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = dialogResult.success(false)
      })
      .setNeutralButton("VIEW", new DialogInterface.OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = {
          //TODO: ??
        }
      })
      .setTitle("Notifications")
      .setMessage("Notifications may include alerts, sounds, and icon badges. These can be configured in Settings.")
    dialog.show()
    dialogResult.future
  }
}
