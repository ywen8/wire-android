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
package com.waz.zclient

import android.content.Intent
import android.text.TextUtils
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.zclient.utils.{BackendPicker, Callback, IntentUtils}

import scala.concurrent.Future

class LaunchActivity extends BaseActivity {

  import com.waz.threading.Threading.Implicits.Ui

  override def getBaseTheme = R.style.Theme_Dark

  override def onBaseActivityStart() = {
    persistInviteToken()
    new BackendPicker(getApplicationContext).withBackend (this, new Callback[Void]() {
      def callback(aVoid: Void) = {
        LaunchActivity.super.onBaseActivityStart()
        ZMessaging.accountsService.flatMap(_.getActiveAccountManager).flatMap {
          case Some(manager) => manager.clientId.orElse(Signal.const(None)).head.map {
            case Some(id) => start(classOf[MainActivity])
            case _        => start(classOf[AppEntryActivity])
          }
          case _ => Future.successful(start(classOf[AppEntryActivity]))
        }
      }
    })
  }

  override protected def onNewIntent(intent: Intent) = {
    super.onNewIntent(intent)
    setIntent(intent)
    persistInviteToken()
  }

  private def persistInviteToken() = {
    val token = IntentUtils.getInviteToken(getIntent)
    if (!TextUtils.isEmpty(token))
      getControllerFactory.getUserPreferencesController.setGenericInvitationToken (token)
  }

  private def start[T](cls: Class[T]) = {
    startActivity(new Intent(this, cls))
    finish()
  }
}

