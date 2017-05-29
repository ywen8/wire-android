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
package com.waz.zclient.controllers

import android.content.Context
import com.waz.ZLog._
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.{Injectable, Injector}

class TeamsAndUserController(implicit injector: Injector, context: Context, ec: EventContext) extends Injectable {
  import Threading.Implicits.Ui
  private implicit val tag: LogTag = logTagFor[TeamsAndUserController]

  val zms = inject[Signal[ZMessaging]]
  val teams = for {
    z <- zms
    teams <- Signal.future(z.teams.getSelfTeams).orElse(Signal(Set[TeamData]()))
    teamSigs <- Signal.sequence(teams.map(_.id).map(z.teamsStorage.signal).toSeq:_*)
  } yield teamSigs

  val self = for {
    z <- zms
    self <- z.usersStorage.signal(z.selfUserId)
  } yield self

  val currentTeamOrUser = Signal[Either[UserData, TeamData]]()
  self.head.map{s => currentTeamOrUser ! Left(s)} //TODO: initial value

  def getCurrentUserOrTeamName: String ={
    currentTeamOrUser.currentValue.map {
      case Left(userData) => userData.displayName
      case Right(teamData) => teamData.name
      case _ => ""
    }.getOrElse("")
  }
}
