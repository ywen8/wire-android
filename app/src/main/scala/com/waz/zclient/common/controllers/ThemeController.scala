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
package com.waz.zclient.common.controllers

import android.content.Context
import com.waz.ZLog.ImplicitTag._
import com.waz.content.UserPreferences.DarkTheme
import com.waz.service.AccountManager
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.ui.theme.{OptionsDarkTheme, OptionsLightTheme, OptionsTheme}
import com.waz.zclient.{Injectable, Injector, R}

import scala.concurrent.Await
import scala.concurrent.duration._

class ThemeController(implicit injector: Injector, context: Context, ec: EventContext) extends Injectable {
  private val am = inject[Signal[AccountManager]]

  import Threading.Implicits.Background

  val optionsDarkTheme:  OptionsTheme = new OptionsDarkTheme(context)
  val optionsLightTheme: OptionsTheme = new OptionsLightTheme(context)

  val darkThemePref = am.map(_.userPrefs.preference(DarkTheme))

  val darkThemeSet = darkThemePref.flatMap(_.signal).disableAutowiring()

  def setDarkTheme(active: Boolean) =
    darkThemePref.head.flatMap(_ := active)

  def toggleDarkTheme() =
    darkThemePref.head.flatMap(_.mutate(!_))

  def isDarkTheme: Boolean = darkThemeSet.currentValue.contains(true)

  def forceLoadDarkTheme: Int = {
    val set = try {
      Await.result(darkThemeSet.head, 1.seconds)
    } catch {
      case _: Exception => false
    }
    if (set) R.style.Theme_Dark else R.style.Theme_Light
  }

  def getTheme: Int = if (isDarkTheme) R.style.Theme_Dark else R.style.Theme_Light

  def getThemeDependentOptionsTheme: OptionsTheme = if (isDarkTheme) optionsDarkTheme else optionsLightTheme
}
