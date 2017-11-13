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

import com.waz.ZLog
import com.waz.ZLog.ImplicitTag._
import com.waz.api.impl.ErrorResponse
import com.waz.api.{ClientRegistrationState, ImageAsset, KindOfAccess}
import com.waz.client.RegistrationClientImpl.ActivateResult
import com.waz.client.RegistrationClientImpl.ActivateResult.{Failure, PasswordExists}
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.threading.{CancellableFuture, Threading}
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.AppEntryController._
import com.waz.zclient.controllers.SignInController
import com.waz.zclient.controllers.SignInController._
import com.waz.zclient.newreg.fragments.SignUpPhotoFragment
import com.waz.zclient.newreg.fragments.SignUpPhotoFragment.RegistrationType
import com.waz.zclient.tracking.{AddPhotoOnRegistrationEvent, GlobalTrackingController}
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

class AppEntryController(implicit inj: Injector, eventContext: EventContext) extends Injectable {

  implicit val ec = Threading.Background

  lazy val optZms = inject[Signal[Option[ZMessaging]]]
  lazy val tracking = inject[GlobalTrackingController]
  val currentAccount = ZMessaging.currentAccounts.activeAccount
  val currentUser = optZms.flatMap{ _.fold(Signal.const(Option.empty[UserData]))(z => z.usersStorage.optSignal(z.selfUserId)) }
  val invitationToken = Signal(Option.empty[String])
  val firstPage = Signal[FirstStage](FirstScreen)

  val invitationDetails = for {
    Some(token) <- invitationToken
    req <- Signal.future(ZMessaging.currentAccounts.retrieveInvitationDetails(PersonalInvitationToken(token)))
  } yield req

  invitationToken.onUi{
    case Some (token) =>
      val invToken = PersonalInvitationToken(token)
      for {
        invDetails <- ZMessaging.currentAccounts.retrieveInvitationDetails(invToken)
        _ <- ZMessaging.currentAccounts.generateAccountFromInvitation(invDetails, invToken)
        _ = invitationToken ! None
      } yield ()
    case _ =>
  }

  val entryStage = for {
    account <- currentAccount
    user <- currentUser
    firstPageState <- firstPage
    fake <- fakeCreateAccountStage
    state <- Signal.const(stateForAccountAndUser(account, user, firstPageState, fake)).collect{ case s if s != Waiting => s }
  } yield state

  val autoConnectInvite = for {
    Some(token)   <- invitationToken
    EnterAppStage <- entryStage
  } yield token

  entryStage.onUi { stage =>
    ZLog.verbose(s"Current stage: $stage")
  }

  def stateForAccountAndUser(account: Option[AccountData], user: Option[UserData], firstPageState: FirstStage, fakeCreateAccountStage: Option[AppEntryStage]): AppEntryStage = {
    ZLog.verbose(s"Current account and user: $account $user")
    fakeCreateAccountStage.fold{
      (account, user) match {
        case (None, _) =>
          NoAccountState(firstPageState)
        case (Some(accountData), _) if accountData.clientRegState == ClientRegistrationState.PASSWORD_MISSING && accountData.email.orElse(accountData.pendingEmail).isDefined =>
          InsertPasswordStage
        case (Some(accountData), _) if accountData.clientRegState == ClientRegistrationState.LIMIT_REACHED =>
          DeviceLimitStage
        case (Some(accountData), _) if accountData.pendingPhone.isDefined && !accountData.verified =>
          VerifyPhoneStage
        case (Some(accountData), _) if accountData.pendingEmail.isDefined && accountData.password.isDefined && !accountData.verified =>
          VerifyEmailStage
        case (Some(accountData), _) if accountData.regWaiting =>
          AddNameStage
        case (Some(accountData), None) if accountData.cookie.isDefined || accountData.accessToken.isDefined =>
          Waiting
        case (Some(accountData), Some(userData)) if userData.picture.isEmpty =>
          AddPictureStage
        case (Some(accountData), Some(userData)) if userData.handle.isEmpty =>
          AddHandleStage
        case (Some(accountData), Some(userData)) if accountData.firstLogin && accountData.clientRegState == ClientRegistrationState.REGISTERED =>
          FirstEnterAppStage
        case (Some(accountData), Some(userData)) if accountData.clientRegState == ClientRegistrationState.REGISTERED =>
          EnterAppStage
        case _ =>
          NoAccountState(firstPageState)
      }
    } { s => s }
  }


  def loginPhone(phone: String): Future[Either[EntryError, Unit]] = {
    ZMessaging.currentAccounts.loginPhone(PhoneNumber(phone)).map {
      case Left(error) => Left(EntryError(error.code, error.label, SignInMethod(Login, Phone)))
      case Right(_) => Right(())
    }
  }

  def loginEmail(email: String, password: String): Future[Either[EntryError, Unit]] = {
    ZMessaging.currentAccounts.loginEmail(EmailAddress(email), password).map {
      case Left(error) => Left(EntryError(error.code, error.label, SignInMethod(Login, Email)))
      case _ => Right(())
    }
  }

  def registerEmail(name: String, email: String, password: String): Future[Either[EntryError, Unit]] = {
    ZMessaging.currentAccounts.registerEmail(EmailAddress(email), password, name).map {
      case Left(error) => Left(EntryError(error.code, error.label, SignInMethod(Register, Email)))
      case _ => Right(())
    }
  }

  def registerPhone(phone: String): Future[Either[EntryError, Unit]] = {
    ZMessaging.currentAccounts.registerPhone(PhoneNumber(phone)).map {
      case Left(error) => Left(EntryError(error.code, error.label, SignInMethod(Register, Phone)))
      case _ => Right(())
    }
  }

  def verifyPhone(code: String): Future[Either[EntryError, Unit]] = {
    ZMessaging.currentAccounts.activeAccount.head.flatMap {
      case Some(accountData) if accountData.regWaiting =>
        val method = SignInMethod(Register, Phone)
        ZMessaging.currentAccounts.activatePhoneOnRegister(accountData.id, ConfirmationCode(code)).map {
          case Left(error) =>
            val entryError = EntryError(error.code, error.label, method)
            tracking.onEnterCode(Left(entryError), method)
            Left(entryError)
          case _ =>
            tracking.onEnterCode(Right(()), method)
            Right(())
        }
      case Some(accountData) =>
        val method = SignInMethod(Login, Phone)
        ZMessaging.currentAccounts.loginPhone(accountData.id, ConfirmationCode(code)).flatMap {
          case Left(error) =>
            val entryError = EntryError(error.code, error.label, method)
            tracking.onEnterCode(Left(entryError), method)
            Future.successful(Left(entryError))
          case _ =>
            tracking.onEnterCode(Right(()), method)
            ZMessaging.currentAccounts.switchAccount(accountData.id).map(_ => Right(()))
        }
      case _ => Future.successful(Left(GenericRegisterPhoneError))
    }
  }

  def registerName(name: String): Future[Either[EntryError, Unit]] = {
    ZMessaging.currentAccounts.activeAccount.head.flatMap {
      case Some(accountData) if accountData.phone.isDefined && accountData.regWaiting && accountData.code.isDefined =>
        ZMessaging.currentAccounts.registerNameOnPhone(accountData.id, name).flatMap {
          case Left(error) =>
            val entryError = EntryError(error.code, error.label, SignInMethod(Register, Phone))
            tracking.onAddNameOnRegistration(Left(entryError), SignInController.Phone)
            Future.successful(Left(entryError))
          case _ =>
            tracking.onAddNameOnRegistration(Right(()), SignInController.Phone)
            ZMessaging.currentAccounts.switchAccount(accountData.id).map(_ => Right(()))
        }
      case _ => Future.successful(Left(GenericRegisterPhoneError))
    }
  }

  def resendActivationEmail(): Unit = {
    ZMessaging.currentAccounts.getActiveAccount.map {
      case Some(account) if account.pendingEmail.isDefined =>
        ZMessaging.currentAccounts.requestVerificationEmail(account.pendingEmail.get)
      case _ =>
    }
  }

  def resendActivationPhoneCode(shouldCall: Boolean = false): Future[Either[EntryError, Unit]] = {

    def requestCode(accountData: AccountData, kindOfAccess: KindOfAccess): Future[ActivateResult] = {
      if (shouldCall)
        ZMessaging.currentAccounts.requestPhoneConfirmationCall(accountData.pendingPhone.get, kindOfAccess).future
      else
        ZMessaging.currentAccounts.requestPhoneConfirmationCode(accountData.pendingPhone.get, kindOfAccess).future
    }

    ZMessaging.currentAccounts.getActiveAccount.flatMap {
      case Some(account) if account.pendingPhone.isDefined =>
        val kindOfAccess = if (account.regWaiting) KindOfAccess.REGISTRATION else KindOfAccess.LOGIN
        requestCode(account, kindOfAccess).map {
          case Failure(error) =>
            Left(EntryError(error.code, error.label, SignInMethod(Register, Phone)))
          case PasswordExists =>
            val validationType = if (account.regWaiting) Register else Login
            Left(EntryError(ErrorResponse.PasswordExists.code, ErrorResponse.PasswordExists.label, SignInMethod(validationType, Phone)))
          case _ =>
            Right(())
        }
      case _ => Future.successful(Left(GenericRegisterPhoneError))
    }
  }

  def cancelVerification(): Unit = ZMessaging.currentAccounts.logout(false)

  def setPicture(imageAsset: ImageAsset, source: SignUpPhotoFragment.Source, registrationType: RegistrationType): Unit = {
    optZms.head.map {
      case Some(zms) =>
        zms.users.updateSelfPicture(imageAsset).map { _ =>
          val trackingSource = source match {
            case SignUpPhotoFragment.Source.Unsplash => AddPhotoOnRegistrationEvent.Unsplash
            case SignUpPhotoFragment.Source.Gallery => AddPhotoOnRegistrationEvent.Gallery
          }
          val trackingRegType = registrationType match {
            case SignUpPhotoFragment.RegistrationType.Email => Email
            case SignUpPhotoFragment.RegistrationType.Phone => Phone
          }
          tracking.onAddPhotoOnRegistration(trackingRegType, trackingSource)
        }
      case _ =>
    }
  }

  //TODO: just for testing, remove
  private val fakeCreateAccountStage = Signal[Option[AppEntryStage]](None)
  private def fakeInput(nextState: AppEntryStage): Future[Either[Unit, ErrorResponse]] = {
    CancellableFuture.delayed(Random.nextInt(4).seconds){
      if (Random.nextBoolean()) Left(fakeCreateAccountStage ! Some(nextState))
      else Right(ErrorResponse.internalError("Shit happened. Try again."))
    }.future
  }

  def gotToFirstPage(): Unit = {
    fakeCreateAccountStage ! None
    firstPage ! FirstScreen
  }

  def createTeam(): Unit = {
    fakeCreateAccountStage ! Some(NoAccountState(RegisterTeamScreen))
    firstPage ! RegisterTeamScreen
  }

  def isCreatingTeam(): Boolean = fakeCreateAccountStage.currentValue.isDefined

  def cancelCreateTeam(): Unit = {
    fakeCreateAccountStage ! None
    firstPage ! FirstScreen
  }

  def goToLoginScreen(): Unit = {
    fakeCreateAccountStage ! None
    firstPage ! LoginScreen
  }

  def setTeamName(name: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetEmail)

  def setEmail(email: String): Future[Either[Unit, ErrorResponse]] = fakeInput(VerifyEmail)

  def setEmailVerificationCode(code: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetName)

  def setName(name: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetPassword)

  def setPassword(password: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetUsername)

  def setUsername(username: String): Future[Either[Unit, ErrorResponse]] = fakeInput(NoAccountState(FirstScreen))

}

object AppEntryController {
  val GenericInviteToken: String = "getwire"

  trait FirstStage
  object FirstScreen extends FirstStage
  object RegisterTeamScreen extends FirstStage
  object LoginScreen extends FirstStage

  trait AppEntryStage
  object Unknown             extends AppEntryStage
  object Waiting             extends AppEntryStage
  object EnterAppStage       extends AppEntryStage
  object FirstEnterAppStage  extends AppEntryStage
  object DeviceLimitStage    extends AppEntryStage
  object AddNameStage        extends AppEntryStage
  object AddPictureStage     extends AppEntryStage
  object VerifyEmailStage    extends AppEntryStage
  object VerifyPhoneStage    extends AppEntryStage
  object AddHandleStage      extends AppEntryStage
  object InsertPasswordStage extends AppEntryStage

  //TODO: team stages, improve names
  object SetEmail            extends AppEntryStage
  object VerifyEmail         extends AppEntryStage
  object SetName             extends AppEntryStage
  object SetPassword         extends AppEntryStage
  object SetUsername         extends AppEntryStage
  case class NoAccountState(page: FirstStage) extends AppEntryStage
}
