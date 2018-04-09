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
import android.widget.TextView
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag._
import com.waz.api.impl.ErrorResponse
import com.waz.content.UserPreferences.PendingPassword
import com.waz.model.AccountData.Password
import com.waz.model.EmailAddress
import com.waz.service.AccountManager
import com.waz.service.AccountManager.ClientRegistrationState.LimitReached
import com.waz.threading.Threading.Implicits.Ui
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.common.controllers.BrowserController
import com.waz.zclient.common.controllers.global.KeyboardController
import com.waz.zclient.newreg.views.PhoneConfirmationButton
import com.waz.zclient.newreg.views.PhoneConfirmationButton.State.{CONFIRM, NONE}
import com.waz.zclient.pages.main.profile.validator.PasswordValidator
import com.waz.zclient.pages.main.profile.views.GuidedEditText
import com.waz.zclient.utils.ContextUtils.showToast
import com.waz.zclient.utils._
import com.waz.zclient.views.LoadingIndicatorView
import com.waz.znet.Response.Status

import scala.concurrent.Future

//Do not rely on having ZMS!
class SetOrRequestPasswordFragment extends FragmentHelper with OnBackPressedListener {

  import SetOrRequestPasswordFragment._

  lazy val am  = inject[Signal[AccountManager]]
  lazy val spinnerController = inject[SpinnerController]

  val password = Signal(Option.empty[Password])

  lazy val passwordValidator = PasswordValidator.instance(getContext)

  lazy val isValid = password.map {
    case Some(p) => passwordValidator.validate(p.str)
    case _ => false
  }

  lazy val hasPw = getBooleanArg(HasPasswordArg)

  lazy val confirmationButton = returning(view[PhoneConfirmationButton](R.id.confirmation_button)) { vh =>
    vh.onClick { _ =>
      spinnerController.showSpinner(LoadingIndicatorView.Spinner)
      for {
        am <- am.head
        Some(pw) <- password.head //pw should be defined
      } yield
      if (hasPw) am.getOrRegisterClient(Some(pw)).map {
        case Right(state) =>
          (am.storage.userPrefs(PendingPassword) := false).map { _ =>
            inject[KeyboardController].hideKeyboardIfVisible()
            state match {
              case LimitReached => activity.replaceMainFragment(OtrDeviceLimitFragment.newInstance, OtrDeviceLimitFragment.Tag, addToBackStack = false)
              case _ => activity.startFirstFragment()
            }
          }
        case Left(err) => showError(err)
      } else
        am.setPassword(pw).flatMap { //TODO perform login request after setting password in case the user has reset their password in the meantime
          case Right(_)  => (am.storage.userPrefs(PendingPassword) := false).map(_ => Right({}))
          case Left(err) => Future.successful(Left(err))
        }.map {
          case Right(_) => activity.startFirstFragment()
          case Left(err) => showError(err)
        }
    }

    isValid.map( if(_) CONFIRM else NONE).onUi ( st => vh.foreach(_.setState(st)))
  }

  private def showError(err: ErrorResponse) = { //this method is needed to avoid a compiler error - don't inline
    spinnerController.hideSpinner()
    //getContainer.showError(EntryError(error.getCode, error.getLabel, SignInMethod(Register, Email)))
    showToast(err match { // TODO show proper dialog...
      case _@ErrorResponse(Status.Forbidden, _, "invalid-credentials") => "Password incorrect - please try again"
      case _ => s"Something went wrong, please try again: $err"
    })
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.fragment_main_start_set_password, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {

    val email = getStringArg(EmailArg).map(EmailAddress).getOrElse(EmailAddress("")) //email should always be defined

    val header = getString(if (hasPw) R.string.provide_password else R.string.add_password)
    val info   = getString(if (hasPw) R.string.provide_password_explanation else R.string.add_email_address_explanation, email.str)

    findById[TextView](getView, R.id.info_text_header).setText(header)
    findById[TextView](getView, R.id.info_text).setText(info)

    Option(findById[GuidedEditText](getView, R.id.password_field)).foreach { field =>
      field.setValidator(passwordValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__password)
      field.getEditText.addTextListener(txt => password ! Some(Password(txt)))
    }

    confirmationButton.foreach(_.setAccentColor(Color.WHITE))

    Option(findById[View](R.id.ttv_signin_forgot_password)).foreach { forgotPw =>
      forgotPw.onClick(inject[BrowserController].openUrl(getString(R.string.url_password_reset)))
      forgotPw.setVisible(hasPw)
    }
  }

  override def onBackPressed() = true // can't go back...

  def activity = getActivity.asInstanceOf[MainActivity]
}

object SetOrRequestPasswordFragment {
  val Tag = ZLog.ImplicitTag.implicitLogTag
  val EmailArg = "EMAIL_ARG"
  val HasPasswordArg = "HAS_PASSWORD_ARG"

  def apply(email: EmailAddress, hasPassword: Boolean = false): SetOrRequestPasswordFragment = {
    val f = new SetOrRequestPasswordFragment()
    f.setArguments(returning(new Bundle()) { b =>
      b.putString(EmailArg, email.str)
      b.putBoolean(HasPasswordArg, hasPassword)
    })
    f
  }
}
