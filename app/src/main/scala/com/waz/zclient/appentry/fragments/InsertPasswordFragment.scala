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
import com.waz.api.EmailCredentials
import com.waz.model.AccountData.Password
import com.waz.model.EmailAddress
import com.waz.threading.Threading
import com.waz.utils.returning
import com.waz.zclient.appentry.{AppEntryActivity, EntryError}
import com.waz.zclient.appentry.controllers.AppEntryController
import com.waz.zclient.appentry.fragments.InsertPasswordFragment._
import com.waz.zclient.appentry.fragments.SignInFragment.{Email, Login, SignInMethod}
import com.waz.zclient.newreg.views.PhoneConfirmationButton
import com.waz.zclient.newreg.views.PhoneConfirmationButton.State._
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.pages.main.profile.validator.{EmailValidator, PasswordValidator}
import com.waz.zclient.pages.main.profile.views.GuidedEditText
import com.waz.zclient.{FragmentHelper, R}

class InsertPasswordFragment extends BaseFragment[Container] with FragmentHelper with View.OnClickListener {

  lazy val appEntryController = inject[AppEntryController]

  lazy val emailValidator = EmailValidator.newInstance()
  lazy val passwordValidator = PasswordValidator.instance(getContext)

  lazy val confirmationButton = Option(findById[PhoneConfirmationButton](R.id.confirmation_button))

  private lazy val emailField = view[GuidedEditText](R.id.email_field)
  private lazy val passwordField = view[GuidedEditText](R.id.password_field)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.insert_password_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {

    emailField.foreach { field =>
      field.setValidator(emailValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__email)
      getStringArg(EmailArg).foreach { field.setText }
      //field.setEditable(false)
    }

    passwordField.foreach { field =>
      field.setValidator(passwordValidator)
      field.setResource(R.layout.guided_edit_text_sign_in__password)
      //signInController.password { field.setText }
      //field.getEditText.addTextListener(signInController.password ! _)
    }

    confirmationButton.foreach { button =>
      button.setOnClickListener(this)
      button.setAccentColor(Color.WHITE)
    }

    setConfirmationButtonActive(true)
    //signInController.isValid.onUi { setConfirmationButtonActive }
    Option(findById[View](R.id.cancel_button)).foreach(_.setOnClickListener(this))
    Option(findById[View](R.id.ttv_signin_forgot_password)).foreach(_.setOnClickListener(this))
  }

  private def setConfirmationButtonActive(active: Boolean): Unit =
    confirmationButton.foreach(_.setState(if(active) CONFIRM else NONE))

  override def onClick(v: View) = {
    v.getId match {
      case R.id.cancel_button =>
        onBackPressed()
      case R.id.confirmation_button =>
        getContainer.enableProgress(true)
        val passwordText = passwordField.map(_.getText).getOrElse("")
        val emailText = emailField.map(_.getText).getOrElse("")
        appEntryController.login(EmailCredentials(EmailAddress(emailText), Password(passwordText))).map {
          case Left(error) =>
            getContainer.enableProgress(false)
            getContainer.showError(EntryError(error.code, error.label, SignInMethod(Login, Email)))
          case Right((userId, _)) =>
            activity.showFragment(FirstLaunchAfterLoginFragment(userId), FirstLaunchAfterLoginFragment.Tag)
            getContainer.enableProgress(false)
        } (Threading.Ui)
      case R.id.ttv_signin_forgot_password =>
        getContainer.onOpenUrl(getString(R.string.url_password_reset))
      case _ =>
    }
  }

  override def onBackPressed() = {
//    signInController.uiSignInState ! SignInMethod(Login, Phone)
    getFragmentManager.popBackStack()
    true
  }

  def activity = getActivity.asInstanceOf[AppEntryActivity]
}

object InsertPasswordFragment {
  val Tag = ZLog.ImplicitTag.implicitLogTag

  val EmailArg = "email_arg"

  def apply() = new InsertPasswordFragment()

  def apply(email: String): InsertPasswordFragment =
    returning(new InsertPasswordFragment()) { f =>
      val bundle = new Bundle()
      bundle.putString(EmailArg, email)
      f.setArguments(bundle)
    }

  trait Container {
    def enableProgress(enabled: Boolean): Unit
    def onOpenUrl(url: String): Unit
    def showError(entryError: EntryError, okCallback: => Unit = {}): Unit
  }
}
