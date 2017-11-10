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

import com.waz.api.impl.ErrorResponse
import com.waz.utils.events.Signal
import CreateAccountController._
import com.waz.threading.{CancellableFuture, Threading}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random


class CreateAccountController {
  val accountState = Signal[CreateAccountState](NoAccountState(FirstScreen))

  implicit val ec = Threading.Background

  //TODO: to test
  private def fakeInput(nextState: CreateAccountState): Future[Either[Unit, ErrorResponse]] = {
    CancellableFuture.delayed(Random.nextInt(4).seconds){
      if (Random.nextBoolean()) Left(accountState ! nextState)
      else Right(ErrorResponse.internalError("Shit happened. Try again."))
    }.future
  }

  def createTeam(): Unit = {
    accountState ! NoAccountState(RegisterTeamScreen)
  }

  def setTeamName(name: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetEmail)

  def setEmail(email: String): Future[Either[Unit, ErrorResponse]] = fakeInput(VerifyEmail)

  def setEmailVerificationCode(code: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetName)

  def setName(name: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetPassword)

  def setPassword(password: String): Future[Either[Unit, ErrorResponse]] = fakeInput(SetUsername)

  def setUsername(username: String): Future[Either[Unit, ErrorResponse]] = fakeInput(NoAccountState(FirstScreen))
}

object CreateAccountController {
  trait CreateAccountState

  trait AppEntryPage
  object FirstScreen extends AppEntryPage
  object RegisterTeamScreen extends AppEntryPage
  object LoginScreen extends AppEntryPage
  case class NoAccountState(page: AppEntryPage) extends CreateAccountState

  object SetEmail extends CreateAccountState
  object VerifyEmail extends CreateAccountState
  object SetName extends CreateAccountState
  object SetPassword extends CreateAccountState
  object SetUsername extends CreateAccountState
}
