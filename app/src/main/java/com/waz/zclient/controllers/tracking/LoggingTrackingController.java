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
package com.waz.zclient.controllers.tracking;

import android.content.Intent;
import android.os.Bundle;
import com.waz.zclient.controllers.tracking.screens.ApplicationScreen;
import com.waz.zclient.controllers.tracking.screens.RegistrationScreen;
import com.waz.zclient.core.controllers.tracking.events.Event;
import timber.log.Timber;

import java.util.List;

@SuppressWarnings("PMD.UselessOverridingMethod")
public class LoggingTrackingController extends TrackingController {

    private static final String TAG = LoggingTrackingController.class.getName();

    @Override
    public void appLaunched(Intent intent) {
        super.appLaunched(intent);
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Override
    public void tagEvent(Event event) {
        super.tagEvent(event);

        if (trackingData == null ||
            (!trackingData.isInitialized() &&
            event.mustWaitForTrackingData())) {
            return;
        }

        Timber.i("Tag event=[name='%s',\nattributes='%s',\nrangedAttributes='%s']",
                 event.getName(),
                 event.getAttributes().toString(),
                 event.getRangedAttributes().toString());
        Timber.i("Custom dimensions:");
        List<String> dimensions = getCustomDimensions();
        Timber.i("Day: %s", dimensions.get(0));
        Timber.i("Hour: %s", dimensions.get(1));
        Timber.i("Contacts: %s", dimensions.get(2));
        Timber.i("Groups: %s", dimensions.get(3));
        Timber.i("Color: %s", dimensions.get(4));
        Timber.i("AB Testing: %s", dimensions.get(5));
    }

    @Override
    public void loadFromSavedInstance(Bundle savedInstanceState) {
        super.loadFromSavedInstance(savedInstanceState);
    }

    @Override
    public void saveToSavedInstance(Bundle outState) {
        super.saveToSavedInstance(outState);
    }

    @Override
    public void onRegistrationScreen(RegistrationScreen screen) {
        super.onRegistrationScreen(screen);
        Timber.i("Tag registration screen=[name='%s']", screen.toString());
    }

    @Override
    public void onApplicationScreen(ApplicationScreen screen) {
        super.onApplicationScreen(screen);
        Timber.i("Tag application screen=[\nname='%s']", screen.toString());
    }
}
