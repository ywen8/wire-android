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

  //TODO: to test
  val accountState = Signal[CreateAccountState](SetTeamName)

  implicit val ec = Threading.Background

  def setTeamName(name: String): Future[Either[Unit, ErrorResponse]] = {
    if (Random.nextBoolean()) {
      CancellableFuture.delayed(Random.nextInt(4).seconds)(accountState ! SetEmail).future.map { _ => Left(()) }
    } else {
      CancellableFuture.delayed(Random.nextInt(4).seconds)(Right(ErrorResponse.internalError("Shit happened. Try again."))).future
    }
  }

  def setEmail(email: String): Future[Either[Unit, ErrorResponse]] = {
    if (Random.nextBoolean()) {
      CancellableFuture.delayed(Random.nextInt(4).seconds)(accountState ! SetTeamName)(Threading.Background).future.map { _ => Left(()) }
    } else {
      CancellableFuture.delayed(Random.nextInt(4).seconds)(Right(ErrorResponse.internalError("Shit happened. Try again."))).future
    }
  }


}

object CreateAccountController {
  trait CreateAccountState

  object SetTeamName extends CreateAccountState
  object SetEmail extends CreateAccountState
  object VerifyEmail extends CreateAccountState
  object SetName extends CreateAccountState
  object SetPassword extends CreateAccountState
}
