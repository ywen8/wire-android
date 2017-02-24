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
package com.waz

import android.app.{Activity, ActivityManager, Application, NotificationManager}
import android.content.{ClipboardManager, ContentResolver, Context, ContextWrapper}
import android.media.AudioManager
import android.os.{PowerManager, Vibrator}
import android.support.v4.app.{FragmentActivity, FragmentManager}
import com.waz.api.{ZMessagingApi, ZMessagingApiFactory}
import com.waz.service.{MediaManagerService, NetworkModeService, PreferenceService, ZMessaging}
import com.waz.utils.events.{EventContext, Signal, Subscription}
import com.waz.zclient.calling.controllers.{CallPermissionsController, CurrentCallController, GlobalCallingController}
import com.waz.zclient.camera.controllers.{AndroidCameraFactory, GlobalCameraController}
import com.waz.zclient.common.controllers.{PermissionActivity, PermissionsController, PermissionsWrapper}
import com.waz.zclient.controllers._
import com.waz.zclient.controllers.drawing.IDrawingController
import com.waz.zclient.controllers.global.{AccentColorController, KeyboardController, SelectionController}
import com.waz.zclient.controllers.navigation.INavigationController
import com.waz.zclient.controllers.singleimage.ISingleImageController
import com.waz.zclient.controllers.theme.IThemeController
import com.waz.zclient.controllers.userpreferences.IUserPreferencesController
import com.waz.zclient.conversation.CollectionController
import com.waz.zclient.media.SoundController
import com.waz.zclient.messages.controllers.{MessageActionsController, NavigationController}
import com.waz.zclient.messages.{LikesController, MessageViewFactory, MessagesController, UsersController}
import com.waz.zclient.notifications.controllers.{CallingNotificationsController, ImageNotificationsController, MessageNotificationsController}
import com.waz.zclient.pages.main.conversation.controller.IConversationScreenController
import com.waz.zclient.pages.main.conversationpager.controller.ISlidingPaneController
import com.waz.zclient.pages.main.pickuser.controller.IPickUserController
import com.waz.zclient.tracking.{CallingTrackingController, GlobalTrackingController, UiTrackingController}
import com.waz.zclient.views.ImageController

package object zclient {

  lazy val AppModule = new Module {
    val ctx = WireApplication.APP_INSTANCE
    bind [Context]          to ctx
    bind [WireContext]      to ctx
    bind [Application]      to ctx
    bind [EventContext]     to ctx.eventContext
    bind [ContentResolver]  to ctx.getContentResolver

    //Android services
    bind [ActivityManager]      to ctx.getSystemService(Context.ACTIVITY_SERVICE).asInstanceOf[ActivityManager]
    bind [PowerManager]         to ctx.getSystemService(Context.POWER_SERVICE).asInstanceOf[PowerManager]
    bind [Vibrator]             to ctx.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    bind [AudioManager]         to ctx.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
    bind [NotificationManager]  to ctx.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    bind [ClipboardManager]     to ctx.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
  }

  lazy val Global = new Module {
    implicit lazy val wContext     = inject[WireContext]
    implicit lazy val eventContext = inject[EventContext]

    def controllerFactory = WireApplication.APP_INSTANCE.controllerFactory

    // SE services
    bind [Signal[Option[ZMessaging]]]     to ZMessaging.currentUi.currentZms
    bind [Signal[ZMessaging]]             to inject[Signal[Option[ZMessaging]]].collect { case Some(z) => z }
    bind [PreferenceService]              to ZMessaging.currentGlobal.prefs
    bind [NetworkModeService]             to ZMessaging.currentGlobal.network
    bind [MediaManagerService]            to ZMessaging.currentGlobal.mediaManager

    // old controllers
    // TODO: remove controller factory, reimplement those controllers
    bind [IControllerFactory]             toProvider controllerFactory
    bind [IPickUserController]            toProvider controllerFactory.getPickUserController
    bind [IThemeController]               toProvider controllerFactory.getThemeController
    bind [IConversationScreenController]  toProvider controllerFactory.getConversationScreenController
    bind [INavigationController]          toProvider controllerFactory.getNavigationController
    bind [IUserPreferencesController]     toProvider controllerFactory.getUserPreferencesController
    bind [IConversationScreenController]  toProvider controllerFactory.getConversationScreenController
    bind [ISingleImageController]         toProvider controllerFactory.getSingleImageController
    bind [ISlidingPaneController]         toProvider controllerFactory.getSlidingPaneController
    bind [IDrawingController]             toProvider controllerFactory.getDrawingController

    // global controllers
    bind [AccentColorController]          to new AccentColorController()
    bind [GlobalCallingController]        to new GlobalCallingController()
    bind [GlobalCameraController]         to new GlobalCameraController(new AndroidCameraFactory)
    bind [SelectionController]            to new SelectionController()
    bind [SoundController]                to new SoundController

    //notifications
    bind [MessageNotificationsController] to new MessageNotificationsController()
    bind [ImageNotificationsController]   to new ImageNotificationsController()
    bind [CallingNotificationsController] to new CallingNotificationsController()

    bind [GlobalTrackingController]       to new GlobalTrackingController()
    bind [CallingTrackingController]      to new CallingTrackingController()
  }

  def ContextModule(implicit ctx: WireContext) = new Module {
    private implicit val eventContext = ctx.eventContext

    bind [Context]      to ctx
    bind [WireContext]  to ctx
    bind [EventContext] to ctx.eventContext
    bind [Activity]     to {
      def getActivity(ctx: Context): Activity = ctx match {
        case a: Activity => a
        case w: ContextWrapper => getActivity(w.getBaseContext)
      }
      getActivity(ctx)
    }
    bind [FragmentManager] to inject[Activity].asInstanceOf[FragmentActivity].getSupportFragmentManager

    bind [ZMessagingApi]      to new ZMessagingApiProvider(ctx).api
    bind [Signal[ZMessaging]] to inject[ZMessagingApi].asInstanceOf[com.waz.api.impl.ZMessagingApi].ui.currentZms.collect{case Some(zms)=> zms }

    bind [KeyboardController]        to new KeyboardController()
    bind [CurrentCallController]     to new CurrentCallController()
    bind [CallPermissionsController] to new CallPermissionsController()
    bind [ImageController]           to new ImageController()
    bind [AssetsController]          to new AssetsController()
    bind [BrowserController]         to new BrowserController()
    bind [MessageViewFactory]        to new MessageViewFactory()
    bind [PermissionActivity]        to ctx.asInstanceOf[PermissionActivity]
    bind [PermissionsController]     to new PermissionsController(new PermissionsWrapper)
    bind [UsersController]           to new UsersController()
    bind [ScreenController]          to new ScreenController()
    bind [NavigationController]      to new NavigationController()
    bind [MessageActionsController]  to new MessageActionsController()
    bind [MessagesController]        to new MessagesController()
    bind [LikesController]           to new LikesController()
    bind [CollectionController]      to new CollectionController()
    bind [SharingController]         to new SharingController()

    bind [UiTrackingController]      to new UiTrackingController()
  }

  class ZMessagingApiProvider(ctx: WireContext) {
    val api = ZMessagingApiFactory.getInstance(ctx)

    api.onCreate(ctx)

    ctx.eventContext.register(new Subscription {
      override def subscribe(): Unit = api.onResume()
      override def unsubscribe(): Unit = api.onPause()
      override def enable(): Unit = ()
      override def disable(): Unit = ()
      override def destroy(): Unit = api.onDestroy()
      override def disablePauseWithContext(): Unit = ()
    })
  }
}
