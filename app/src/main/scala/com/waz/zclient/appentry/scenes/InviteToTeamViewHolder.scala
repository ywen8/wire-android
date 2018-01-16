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
package com.waz.zclient.appentry.scenes

import android.content.Context
import android.support.v7.widget.RecyclerView.AdapterDataObserver
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.text.InputType
import android.view.View
import android.view.View.OnLayoutChangeListener
import com.waz.ZLog
import com.waz.api.impl.ErrorResponse
import com.waz.api.impl.ErrorResponse.{Forbidden, InternalErrorCode, ConnectionErrorCode}
import com.waz.model.EmailAddress
import com.waz.threading.Threading
import com.waz.utils.events.EventContext
import com.waz.utils.returning
import com.waz.utils.wrappers.AndroidURIUtil
import com.waz.zclient.appentry.InvitesAdapter
import com.waz.zclient.appentry.controllers.InvitationsController
import com.waz.zclient.common.controllers.BrowserController
import com.waz.zclient.common.views.InputBox
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.{Injectable, Injector, R}
import com.waz.zclient.utils.{RichTextView, RichView}

import scala.concurrent.Future

case class InviteToTeamViewHolder(root: View)(implicit val context: Context, eventContext: EventContext, injector: Injector) extends ViewHolder with Injectable {

  private lazy val invitesController = inject[InvitationsController]
  private lazy val browser = inject[BrowserController]

  private lazy val adapter = new InvitesAdapter()
  private lazy val inputField = root.findViewById[InputBox](R.id.input_field)
  private lazy val sentInvitesList = root.findViewById[RecyclerView](R.id.sent_invites_list)
  private lazy val learnMoreButton = root.findViewById[TypefaceTextView](R.id.about_button)

  override def onCreate(): Unit = {

    val layoutManager = returning(new LinearLayoutManager(context)) {
      _.setStackFromEnd(true)
    }
    sentInvitesList.setAdapter(adapter)
    sentInvitesList.setLayoutManager(layoutManager)
    adapter.registerAdapterDataObserver(new AdapterDataObserver {
      override def onChanged(): Unit = layoutManager.scrollToPosition(adapter.getItemCount - 1)
    })

    sentInvitesList.addOnLayoutChangeListener(new OnLayoutChangeListener {
      override def onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or: Int, ob: Int): Unit =
        layoutManager.scrollToPosition(adapter.getItemCount - 1)
    })

    inputField.setShouldDisableOnClick(false)
    inputField.setShouldClearTextOnClick(true)
    inputField.setValidator(InputBox.SimpleValidator)
    inputField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
    inputField.setButtonGlyph(R.string.glyph__send)
    inputField.editText.setText(invitesController.inputEmail)
    inputField.editText.addTextListener{ text =>
      learnMoreButton.setVisibility(View.INVISIBLE)
      invitesController.inputEmail = text
    }

    inputField.setOnClick { text =>
      val email = EmailAddress.parse(text)
      email match {
        case Some(e) => invitesController.sendInvite(e).map {
          case Left(ErrorResponse(Forbidden, _, "too-many-team-invitations")) => Some(context.getString(R.string.teams_invitations_error_too_many))
          case Left(ErrorResponse(400, _, "invalid-email")) => Some(context.getString(R.string.teams_invitations_error_invalid_email))
          case Left(ErrorResponse(400, _, "bad-request")) => Some(context.getString(R.string.teams_invitations_error_invalid_email))
          case Left(ErrorResponse(409, _, "email-exists")) =>
            learnMoreButton.setVisible(true)
            Some(context.getString(R.string.teams_invitations_error_email_exists))
          case Left(ErrorResponse(InternalErrorCode, _, "already-sent")) => Some(context.getString(R.string.teams_invitations_error_already_sent))
          case Left(ErrorResponse(ConnectionErrorCode, _, _)) => Some(context.getString(R.string.teams_invitations_error_no_internet))
          case Left(error) =>
            import ZLog.ImplicitTag._
            ZLog.verbose(s"$error")
            Some(context.getString(R.string.teams_invitations_error_generic))
          case _ => None
        }(Threading.Ui)
        case _ =>
          Future.successful(Some(context.getString(R.string.teams_invitations_error_invalid_email)))
      }
    }
    learnMoreButton.onClick {
      browser.openUrl(AndroidURIUtil.parse(context.getString(R.string.invalid_email_help)))
    }
  }
}

object InviteToTeamViewHolder {
  val Tag = ZLog.ImplicitTag.implicitLogTag
  trait Container
}
