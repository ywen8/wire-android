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
package com.waz.zclient

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.TextView
import com.waz.model.EmailAddress
import com.waz.service.{AccountManager, AccountsService}
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.ui.utils.{KeyboardUtils, TextViewUtils}

object VerifyEmailFragment {
  val Tag: String = classOf[VerifyEmailFragment].getName

  val EmailArg = "EMAIL_ARG"

  def apply(email: EmailAddress): VerifyEmailFragment = {
    val f = new VerifyEmailFragment
    f.setArguments(returning(new Bundle()) { b =>
      b.putString(EmailArg, email.str)
    })
    f
  }
}

class VerifyEmailFragment extends FragmentHelper {

  import VerifyEmailFragment._

  lazy val am = inject[Signal[AccountManager]]

  lazy val email = getStringArg(EmailArg).map(EmailAddress)
  lazy val accounts = inject[AccountsService]

  lazy val resendTextView = returning(view[TextView](R.id.ttv__pending_email__resend)) { vh =>
    vh.onClick { _ =>
      didntGetEmailTextView.foreach(_.animate.alpha(0).start())
      vh.foreach(_.animate.alpha(0).withEndAction(new Runnable() {
        def run(): Unit = {
          vh.foreach(_.setEnabled(false))
        }
      }).start())
      email.foreach(accounts.requestVerificationEmail)
    }
  }

  lazy val didntGetEmailTextView = returning(view[TextView](R.id.ttv__sign_up__didnt_get)) { vh =>
    vh.onClick(_ => back())
  }

  lazy val backButton = view[View](R.id.ll__activation_button__back)

  override def onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_main_start_verify_email, viewGroup, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)
    resendTextView
    didntGetEmailTextView
    backButton

    Option(findById[TextView](view, R.id.ttv__sign_up__check_email)).foreach { v =>
      email.foreach { e =>
        v.setText(getResources.getString(R.string.profile__email__verify__instructions, e.str))
        TextViewUtils.boldText(v)
      }
    }

    //TODO we now need to poll in the account manager when waiting for verification...
//    email.onChanged.filter(_.isEmpty).onUi { _ =>
//      activity.replaceMainFragment(new MainPhoneFragment, MainPhoneFragment.Tag)
//    }
  }

  override def onPause(): Unit = {
    KeyboardUtils.hideKeyboard(getActivity)
    super.onPause()
  }

  private def back() = activity.replaceMainFragment(AddEmailFragment(skippable = false), AddEmailFragment.Tag)

  def activity = getActivity.asInstanceOf[MainActivity]

  override def onBackPressed(): Boolean = {
    back()
    true
  }
}
