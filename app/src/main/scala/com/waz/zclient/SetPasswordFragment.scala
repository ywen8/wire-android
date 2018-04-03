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

import android.graphics.Color
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag._
import com.waz.content.UserPreferences
import com.waz.content.UserPreferences.PendingPassword
import com.waz.model.AccountData.Password
import com.waz.model.EmailAddress
import com.waz.service.{AccountManager, ZMessaging}
import com.waz.threading.Threading.Implicits.Ui
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.SetPasswordFragment._
import com.waz.zclient.newreg.views.PhoneConfirmationButton
import com.waz.zclient.newreg.views.PhoneConfirmationButton.State.{CONFIRM, NONE}
import com.waz.zclient.pages.main.MainPhoneFragment
import com.waz.zclient.pages.main.profile.validator.{EmailValidator, PasswordValidator}
import com.waz.zclient.pages.main.profile.views.GuidedEditText
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils.showToast
import com.waz.zclient.utils._
import com.waz.zclient.views.LoadingIndicatorView

import scala.concurrent.Future

//Do not rely on having ZMS!
class SetPasswordFragment extends FragmentHelper with OnBackPressedListener {

  import SetPasswordFragment._

  lazy val am  = inject[Signal[AccountManager]]
  lazy val spinnerController = inject[SpinnerController]

  val password = Signal(Option.empty[Password])

  lazy val passwordValidator = PasswordValidator.instance(getContext)

  lazy val isValid = password.map {
    case Some(p) => passwordValidator.validate(p.str)
    case _ => false
  }

  //TODO show email somewhere on screen
  lazy val email = getStringArg(EmailArg).map(EmailAddress)

  lazy val confirmationButton = returning(view[PhoneConfirmationButton](R.id.confirmation_button)) { vh =>
    vh.onClick { _ =>
      spinnerController.showSpinner(LoadingIndicatorView.Spinner)
      for {
        am      <- am.head
        Some(p) <- password.head
        resp    <- am.setPassword(p)
        _       <- resp match {
          case Right(_) => am.storage.userPrefs(PendingPassword) := false
          case Left(_) => Future.successful({})
        }
      } yield {
        spinnerController.hideSpinner()
        resp match {
          case Right(_) =>
            activity.startFirstFragment()
          case Left(err) =>
            //getContainer.showError(EntryError(error.getCode, error.getLabel, SignInMethod(Register, Email)))
            showToast(s"Something went wrong: $err") //TODO show error to user
        }
      }
    }

    isValid.map( if(_) CONFIRM else NONE).onUi ( st => vh.foreach(_.setState(st)))
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.fragment_main_start_request_password_with_email, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    Option(findById[GuidedEditText](getView, R.id.password_field)).foreach { field =>
      field.setValidator(passwordValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__password)
      field.getEditText.addTextListener(txt => password ! Some(Password(txt)))
    }

    confirmationButton.foreach(_.setAccentColor(Color.WHITE))

    returning(findById[TypefaceTextView](R.id.skip_button)) { v =>
      v.onClick(activity.replaceMainFragment(new MainPhoneFragment, MainPhoneFragment.Tag))
    }
  }

  override def onBackPressed() = true // can't go back...

  def activity = getActivity.asInstanceOf[MainActivity]
}

object SetPasswordFragment {
  val Tag = ZLog.ImplicitTag.implicitLogTag
  val EmailArg = "EMAIL_ARG"

  def apply(email: EmailAddress): SetPasswordFragment = {
    val f = new SetPasswordFragment()
    f.setArguments(returning(new Bundle()) { b =>
      b.putString(EmailArg, email.str)
    })
    f
  }
}
