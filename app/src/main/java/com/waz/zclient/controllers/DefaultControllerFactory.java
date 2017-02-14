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
package com.waz.zclient.controllers;

import android.content.Context;
import com.waz.zclient.BuildConfig;
import com.waz.zclient.R;
import com.waz.zclient.controllers.loadtimelogger.LoadTimeLoggerController;
import com.waz.zclient.controllers.tracking.DisabledTrackingController;
import com.waz.zclient.controllers.tracking.LoggingTrackingController;
import com.waz.zclient.controllers.tracking.TrackingController;
import com.waz.zclient.controllers.userpreferences.UserPreferencesController;

public class DefaultControllerFactory extends Base$$ControllerFactory {

    public DefaultControllerFactory(Context context) {
        super(context);
    }

    @Override
    protected void initLoadTimeLoggerController() {
        if (loadTimeLoggerController != null) {
            return;
        }
        if (BuildConfig.IS_LOADTIME_LOGGER_ENABLED) {
            loadTimeLoggerController = new LoadTimeLoggerController();
        } else {
            loadTimeLoggerController = new LoadTimeLoggerController.DisabledLoadTimeLoggerController();
        }
    }

    @Override
    protected void initTrackingController() {
        final boolean trackingEnabled = context.getSharedPreferences(UserPreferencesController.USER_PREFS_TAG, Context.MODE_PRIVATE)
                                               .getBoolean(context.getString(R.string.pref_advanced_analytics_enabled_key), true);

        if (trackingEnabled) {
            if (trackingController == null || !(trackingController instanceof TrackingController)) {
                if (BuildConfig.DISABLE_TRACKING_KEEP_LOGGING) {
                    trackingController = new DisabledTrackingController();
                } else if (BuildConfig.DEBUG) {
                    trackingController = new LoggingTrackingController();
                } else {
                    trackingController = new TrackingController();
                }
            }
        } else {
            if (trackingController == null || !(trackingController instanceof DisabledTrackingController)) {
                if (trackingController != null) {
                    trackingController.tearDown();
                    trackingController = null;
                }
                trackingController = new DisabledTrackingController();
            }
        }
    }
}
