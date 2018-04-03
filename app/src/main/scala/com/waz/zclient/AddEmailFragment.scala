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
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag._
import com.waz.content.UserPreferences.{PendingEmail, PendingPassword}
import com.waz.model.EmailAddress
import com.waz.service.AccountManager
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.newreg.views.PhoneConfirmationButton
import com.waz.zclient.newreg.views.PhoneConfirmationButton.State.{CONFIRM, NONE}
import com.waz.zclient.pages.main.MainPhoneFragment
import com.waz.zclient.pages.main.profile.validator.EmailValidator
import com.waz.zclient.pages.main.profile.views.GuidedEditText
import com.waz.zclient.utils.ContextUtils.showToast
import com.waz.zclient.utils._
import com.waz.zclient.views.LoadingIndicatorView
import com.waz.threading.Threading
import scala.concurrent.Future

//Do not rely on having ZMS!
class AddEmailFragment extends FragmentHelper {
  import AddEmailFragment._
  import Threading.Implicits.Ui

  lazy val am  = inject[Signal[AccountManager]]
  lazy val spinnerController = inject[SpinnerController]

  lazy val emailValidator = EmailValidator.newInstance()
  lazy val email = Signal(Option.empty[EmailAddress])


  lazy val isValid = email.map {
    case Some(p) => emailValidator.validate(p.str)
    case _ => false
  }

  lazy val skipButton = returning(view[View](R.id.skip_button)) { vh =>
    vh.onClick(_ => activity.replaceMainFragment(new MainPhoneFragment(), MainPhoneFragment.Tag))
  }

  lazy val confirmationButton = returning(view[PhoneConfirmationButton](R.id.confirmation_button)) { vh =>
    vh.onClick { _ =>
      spinnerController.showSpinner(LoadingIndicatorView.Spinner)
      for {
        am      <- am.head
        Some(e) <- email.head
        resp    <- am.setEmail(e)
        _       <- resp match {
          case Right(_) => for {
            _ <- am.storage.userPrefs(PendingPassword) := true
            _ <- am.storage.userPrefs(PendingEmail) := Some(e)
          } yield {}
          case Left(_) => Future.successful({})
        }
      } yield {
        spinnerController.hideSpinner()
        resp match {
          case Right(_) => activity.replaceMainFragment(VerifyEmailFragment(e), VerifyEmailFragment.Tag)
          case Left(err) =>
            //getContainer.showError(EntryError(error.getCode, error.getLabel, SignInMethod(Register, Email)))
            showToast(s"Something went wrong: $err") //TODO show error to user
        }
      }
    }

    isValid.map( if(_) CONFIRM else NONE).onUi ( st => vh.foreach(_.setState(st)))
  }


  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.fragment_main_start_add_email, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    super.onViewCreated(view, savedInstanceState)

    Option(findById[GuidedEditText](view, R.id.email_field)).foreach { field =>
      field.setValidator(emailValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__email)
      field.getEditText.addTextListener(txt => email ! Some(EmailAddress(txt)))
    }

    skipButton.foreach(_.setVisible(getBooleanArg(SkippableArg, default = false)))
  }

  def activity = getActivity.asInstanceOf[MainActivity]
}

object AddEmailFragment {
  val Tag: String = ZLog.ImplicitTag.implicitLogTag
  val SkippableArg = "SKIPPABLE"

  def apply(skippable: Boolean): AddEmailFragment = {
    val f = new AddEmailFragment()
    f.setArguments(returning(new Bundle()) { b =>
      b.putBoolean(SkippableArg, skippable)
    })
    f
  }
}
