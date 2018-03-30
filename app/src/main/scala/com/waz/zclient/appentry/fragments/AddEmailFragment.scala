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

import android.graphics.Color
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag._
import com.waz.model.AccountData.Password
import com.waz.model.EmailAddress
import com.waz.service.{AccountManager, ZMessaging}
import com.waz.threading.Threading.Implicits.Ui
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.appentry.fragments.AddEmailFragment._
import com.waz.zclient.newreg.views.PhoneConfirmationButton
import com.waz.zclient.newreg.views.PhoneConfirmationButton.State.{CONFIRM, NONE}
import com.waz.zclient.pages.main.profile.validator.{EmailValidator, PasswordValidator}
import com.waz.zclient.pages.main.profile.views.GuidedEditText
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils.showToast
import com.waz.zclient.utils._
import com.waz.zclient.views.LoadingIndicatorView
import com.waz.zclient._
import com.waz.zclient.pages.main.MainPhoneFragment

class AddEmailFragment extends FragmentHelper with OnBackPressedListener {

  lazy val zms = inject[Signal[ZMessaging]]
  lazy val users = zms.map(_.users)
  lazy val am  = inject[Signal[AccountManager]]
  lazy val spinnerController = inject[SpinnerController]

  val email    = Signal(Option.empty[EmailAddress])
  val password = Signal(Option.empty[Password])

  lazy val emailValidator    = EmailValidator.newInstance()
  lazy val passwordValidator = PasswordValidator.instance(getContext)

  lazy val isValid: Signal[Boolean] = for {
    email    <- email
    password <- password
  } yield (email, password) match {
    case (Some(e), Some(p)) => emailValidator.validate(e.str) && passwordValidator.validate(p.str)
    case _ => false
  }

  lazy val confirmationButton = returning(view[PhoneConfirmationButton](R.id.confirmation_button)) { vh =>
    vh.onClick { _ =>
      spinnerController.showSpinner(LoadingIndicatorView.Spinner)
      for {
        Some(e) <- email.head
        Some(p) <- password.head
        users   <- users.head
        resp    <- users.setEmail(e, p)
      } yield {
        spinnerController.hideSpinner()
        resp match {
          case Right(_) =>
            activity.replaceMainFragment(EmailVerifyEmailFragment(), EmailVerifyEmailFragment.Tag)
          case Left(err) =>
            //getContainer.showError(EntryError(error.getCode, error.getLabel, SignInMethod(Register, Email)))
            showToast(s"Something went wrong: $err") //TODO show error to user
        }
      }
    }

    isValid.map( if(_) CONFIRM else NONE).onUi ( st => vh.foreach(_.setState(st)))
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.add_email_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    //TODO handle back press from verification screen and re-fill this field, also don't send another request if it doesn't change
    Option(findById[GuidedEditText](getView, R.id.email_field)).foreach { field =>
      field.setValidator(emailValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__email)
      field.getEditText.addTextListener(txt => email ! Some(EmailAddress(txt)))
    }

    Option(findById[GuidedEditText](getView, R.id.password_field)).foreach { field =>
      field.setValidator(passwordValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__password)
      field.getEditText.addTextListener(txt => password ! Some(Password(txt)))
    }

    confirmationButton.foreach(_.setAccentColor(Color.WHITE))

    returning(findById[TypefaceTextView](R.id.skip_button)) { v =>
      v.setVisible(getBooleanArg(SkippableArg, default = false))
      v.onClick(activity.replaceMainFragment(new MainPhoneFragment, MainPhoneFragment.Tag))
    }
  }

  override def onBackPressed(): Boolean = true

  def activity = getActivity.asInstanceOf[MainActivity]
}

object AddEmailFragment {
  val Tag = ZLog.ImplicitTag.implicitLogTag

  val SkippableArg = "SKIPPABLE"

  def apply(skippable: Boolean): AddEmailFragment =
    returning(new AddEmailFragment()) {
      _.setArguments(returning(new Bundle) { b =>
        b.putBoolean(SkippableArg, skippable)
      })
    }
}
