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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.waz.ZLog.ImplicitTag._
import com.waz.content.UserPreferences.DarkTheme
import com.waz.service.ZMessaging
import com.waz.threading.{SerialDispatchQueue, Threading}
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.ui.theme.{OptionsDarkTheme, OptionsLightTheme, OptionsTheme}
import com.waz.zclient.utils.IntentUtils
import com.waz.zclient.{ActivityHelper, Injectable, Injector, R}

class ThemeController(implicit injector: Injector, context: Context, ec: EventContext) extends Injectable {
  private val zms = inject[Signal[ZMessaging]]

  import Threading.Implicits.Background

  val optionsDarkTheme:  OptionsTheme = new OptionsDarkTheme(context)
  val optionsLightTheme: OptionsTheme = new OptionsLightTheme(context)

  val darkThemePref = zms.map(_.userPrefs.preference(DarkTheme))

  val darkThemeSet = darkThemePref.flatMap(_.signal).disableAutowiring()

  def setDarkTheme(active: Boolean) =
    darkThemePref.head.flatMap(_ := active)

  def toggleDarkTheme() =
    darkThemePref.head.flatMap(_.mutate(!_))

  def isDarkTheme: Boolean = darkThemeSet.currentValue.contains(true)

  def getTheme: Int = if (isDarkTheme) R.style.Theme_Dark else R.style.Theme_Light

  def getThemeDependentOptionsTheme: OptionsTheme = if (isDarkTheme) optionsDarkTheme else optionsLightTheme
}

/**
  * Trait for activities that need to restart if the theme changes - so far, this should only be used by the
  * main activity
  */
trait ThemeObservingActivity extends Activity with ActivityHelper {

  private implicit val dispatcher = new SerialDispatchQueue()

  private lazy val themeController = inject[ThemeController]
  import themeController._

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    val currentlyDarkTheme = darkThemeSet.currentValue.contains(true)
    darkThemeSet.onUi { set =>
      if (set != currentlyDarkTheme) restartActivity()
    }

  }

  private def restartActivity() = {
    finish()
    startActivity(IntentUtils.getAppLaunchIntent(this))
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

}
