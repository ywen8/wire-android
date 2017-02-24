/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
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

import java.io.File

import android.graphics.Typeface
import android.support.multidex.MultiDexApplication
import com.jakewharton.threetenabp.AndroidThreeTen
import com.localytics.android.{Localytics, LocalyticsActivityLifecycleCallbacks}
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog.{error, verbose}
import com.waz.api.LogLevel
import com.waz.api.impl.AccentColors
import com.waz.utils.events.EventContext
import com.waz.zclient.api.scala.ScalaStoreFactory
import com.waz.zclient.controllers._
import com.waz.zclient.core.stores.IStoreFactory
import com.waz.zclient.notifications.controllers.{CallingNotificationsController, ImageNotificationsController, MessageNotificationsController}
import com.waz.zclient.tracking.{CallingTrackingController, GlobalTrackingController}
import com.waz.zclient.ui.text.{TypefaceFactory, TypefaceLoader}
import com.waz.zclient.utils.{BackendPicker, BuildConfigUtils, WireLoggerTree}
import timber.log.Timber

class WireApplication extends MultiDexApplication with WireContext with Injectable {
  import WireApplication._

  WireApplication.APP_INSTANCE = this

  override def eventContext: EventContext = EventContext.Global

  var controllerFactory: IControllerFactory = _
  var storeFactory: IStoreFactory = _

  override def onCreate(): Unit = {
    super.onCreate()
    controllerFactory = new DefaultControllerFactory(getApplicationContext)

    if (new BackendPicker(this).hasBackendConfig) ensureInitialized()

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree)
      LogLevel.setMinimumLogLevel(LogLevel.VERBOSE)
    }
    else {
      Timber.plant(new WireLoggerTree)
      LogLevel.setMinimumLogLevel(BuildConfigUtils.getLogLevelSE(this))
    }

    AndroidThreeTen.init(this)
    TypefaceFactory.getInstance.init(new TypefaceLoader() {

      private var typefaceMap = Map.empty[String, Typeface]

      def getTypeface(name: String): Typeface = Option(name) match {

        case Some(n) if n.nonEmpty => typefaceMap.getOrElse(n, try {
          val typeface =
            if (name == getString(R.string.wire__glyphs) || name == getString(R.string.wire__typeface__redacted)) Typeface.createFromAsset(getAssets, FONT_FOLDER + File.separator + name)
            else if (name == getString(R.string.wire__typeface__thin)) Typeface.create("sans-serif-thin", Typeface.NORMAL)
            else if (name == getString(R.string.wire__typeface__light)) Typeface.create("sans-serif-light", Typeface.NORMAL)
            else if (name == getString(R.string.wire__typeface__regular)) Typeface.create("sans-serif", Typeface.NORMAL)
            else if (name == getString(R.string.wire__typeface__medium)) Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else if (name == getString(R.string.wire__typeface__bold)) Typeface.create("sans-serif", Typeface.BOLD)
            else {
              error(s"Couldn't load typeface: $name")
              Typeface.DEFAULT
            }
          typefaceMap += name -> typeface
          typeface
        }
        catch {
          case t: Throwable =>
            error(s"Couldn't load typeface: $name", t)
            null
        })
        case _ => null.asInstanceOf[Typeface]
      }
    })

    Thread.setDefaultUncaughtExceptionHandler(new WireUncaughtExceptionHandler(controllerFactory, Thread.getDefaultUncaughtExceptionHandler))
    // refresh
    AccentColors.setColors(AccentColors.loadArray(getApplicationContext, R.array.original_accents_color))

    // Register LocalyticsActivityLifecycleCallbacks
    registerActivityLifecycleCallbacks(new LocalyticsActivityLifecycleCallbacks(this))
    Localytics.setPushDisabled(false)
  }

  def ensureInitialized() = {
    verbose("ensureInitialized")
    if (storeFactory == null) {
      storeFactory = new ScalaStoreFactory(getApplicationContext)
      //TODO initialization of ZMessaging happens here - make this more explicit?
      storeFactory.getZMessagingApiStore.getAvs.setLogLevel(BuildConfigUtils.getLogLevelAVS(this))
    }

    inject[MessageNotificationsController]
    inject[ImageNotificationsController]
    inject[CallingNotificationsController]

    //TODO [AN-4942] - is this early enough for app launch events?
    inject[GlobalTrackingController]
    inject[CallingTrackingController]
  }

  override def onTerminate(): Unit = {
    controllerFactory.tearDown()
    storeFactory.tearDown()
    storeFactory = null
    controllerFactory = null
    super.onTerminate()
  }
}

object WireApplication {
  var APP_INSTANCE: WireApplication = _
  val FONT_FOLDER: String = "fonts"
}
