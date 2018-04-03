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

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.TextView
import com.waz.ZLog.ImplicitTag._
import com.waz.content.UserPreferences.PendingEmail
import com.waz.service.{AccountsService, ZMessaging}
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.pages.main.MainPhoneFragment
import com.waz.zclient.ui.utils.{KeyboardUtils, TextViewUtils}
import com.waz.zclient.{FragmentHelper, MainActivity, R}

import scala.concurrent.Future

object VerifyEmailFragment {
  val Tag: String = classOf[VerifyEmailFragment].getName
  def apply(): VerifyEmailFragment = new VerifyEmailFragment
}

class VerifyEmailFragment extends FragmentHelper with View.OnClickListener {

  import com.waz.threading.Threading.Implicits.Ui

  lazy val pendingEmail = inject[Signal[ZMessaging]].flatMap(_.userPrefs(PendingEmail).signal)
  lazy val accounts = inject[AccountsService]

  lazy val resendTextView = view[TextView](R.id.ttv__pending_email__resend)

  lazy val checkEmailTextView = returning(view[TextView](R.id.ttv__sign_up__check_email)) { vh =>
    pendingEmail.onUi {
      case Some(e) =>
        vh.foreach(_.setText(getResources.getString(R.string.profile__email__verify__instructions, e.str)))
        vh.foreach(TextViewUtils.boldText)
      case _ => //
    }
  }

  lazy val didntGetEmailTextView = view[TextView](R.id.ttv__sign_up__didnt_get)
  lazy val backButton = view[View](R.id.ll__activation_button__back)

  override def onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_pending_email_email_confirmation, viewGroup, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)
    checkEmailTextView
    resendTextView
    didntGetEmailTextView
    backButton

    pendingEmail.onChanged.filter(_.isEmpty).onUi { _ =>
      activity.replaceMainFragment(new MainPhoneFragment, MainPhoneFragment.Tag)
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    backButton.foreach(_.setOnClickListener(this))
    resendTextView.foreach(_.setOnClickListener(this))
  }

  override def onPause(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    backButton.foreach(_.setOnClickListener(null))
    resendTextView.foreach(_.setOnClickListener(null))
    super.onPause()
  }

  private def back() = activity.replaceMainFragment(AddEmailAndPasswordFragment(skippable = false), AddEmailAndPasswordFragment.Tag)

  def onClick(v: View): Unit = {
    v.getId match {
      case R.id.ll__activation_button__back =>
        back()
      case R.id.ttv__pending_email__resend =>
        didntGetEmailTextView.foreach(_.animate.alpha(0).start())
        resendTextView.foreach(_.animate.alpha(0).withEndAction(new Runnable() {
          def run(): Unit = {
            resendTextView.foreach(_.setEnabled(false))
          }
        }).start())
        pendingEmail.head.flatMap {
          case Some(e) => accounts.requestVerificationEmail(e)
          case _ => Future.successful({})
        }
    }
  }

  def activity = getActivity.asInstanceOf[MainActivity]

  override def onBackPressed(): Boolean = {
    back()
    true
  }
}
