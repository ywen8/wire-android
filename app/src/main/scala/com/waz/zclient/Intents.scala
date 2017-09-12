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

import android.app.PendingIntent
import android.content.{Context, Intent}
import com.waz.model.{AccountId, ConvId}

object Intents {

  private lazy val FromNotificationExtra = "from_notification"
  private lazy val FromSharingExtra      = "from_sharing"
  private lazy val StartCallExtra        = "start_call"
  private lazy val AccountIdExtra        = "account_id"
  private lazy val ConvIdExtra           = "conv_id"

  def CallIntent(accountId: AccountId, convId: ConvId, requestCode: Int)(implicit context: Context) =
    Intent(context, accountId, Some(convId), requestCode, startCall = true)

  def QuickReplyIntent(accountId: AccountId, convId: ConvId, requestCode: Int)(implicit context: Context) =
    Intent(context, accountId, Some(convId), requestCode, classOf[PopupActivity])

  def OpenConvIntent(accountId: AccountId, convId: ConvId, requestCode: Int)(implicit context: Context) =
    Intent(context, accountId, Some(convId), requestCode)

  def OpenAccountIntent(accountId: AccountId, requestCode: Int = System.currentTimeMillis().toInt)(implicit context: Context) =
    Intent(context, accountId)

  def SharingIntent(implicit context: Context) =
    new Intent(context, classOf[MainActivity]).putExtra(FromSharingExtra, true)

  private def Intent(context:     Context,
                     accountId:   AccountId,
                     convId:      Option[ConvId] = None,
                     requestCode: Int            = System.currentTimeMillis().toInt,
                     clazz:       Class[_]       = classOf[MainActivity],
                     startCall:   Boolean        = false) = {
    val intent = new Intent(context, clazz)
      .putExtra(FromNotificationExtra,        true)
      .putExtra(StartCallExtra,         startCall)
      .putExtra(AccountIdExtra,         accountId.str)
    convId.foreach(c => intent.putExtra(ConvIdExtra, c.str))
    PendingIntent.getActivity(context, requestCode, intent, 0)
  }

  implicit class RichIntent(val intent: Intent) extends AnyVal {
    def fromNotification = Option(intent).exists(_.getBooleanExtra(FromNotificationExtra, false))
    def fromSharing      = Option(intent).exists(_.getBooleanExtra(FromSharingExtra, false))
    def startCall        = Option(intent).exists(_.getBooleanExtra(StartCallExtra, false))
    def accountId        = Option(intent).map(_.getStringExtra(AccountIdExtra)).filter(_ != null).map(AccountId)
    def convId           = Option(intent).map(_.getStringExtra(ConvIdExtra)).filter(_ != null).map(ConvId)

    def clearExtras() = Option(intent).foreach { i =>
      i.removeExtra(FromNotificationExtra)
      i.removeExtra(FromSharingExtra)
      i.removeExtra(StartCallExtra)
      i.removeExtra(AccountIdExtra)
      i.removeExtra(ConvIdExtra)
    }

    def log =
      s"""NofiticationIntent:
          |FromNotification: $fromNotification
          |FromSharing:      $fromSharing
          |Start call:       $startCall
          |Account id:       $accountId
          |Conv id:          $convId
        """.stripMargin
  }

  object NotificationIntent {
    def unapply(i: Intent): Option[(AccountId, Option[ConvId], Boolean)] =
      if (i.fromNotification && i.accountId.isDefined) Some(i.accountId.get, i.convId, i.startCall)
      else None
  }

  object SharingIntent {
    def unapply(i: Intent): Boolean = i.fromSharing
  }


}
