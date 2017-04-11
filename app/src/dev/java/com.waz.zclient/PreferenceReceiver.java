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
package com.waz.zclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.waz.zclient.controllers.userpreferences.UserPreferencesController;
import com.waz.zclient.core.R;

public class PreferenceReceiver extends BroadcastReceiver {

    public static final String AUTO_ANSWER_CALL_INTENT = "com.waz.zclient.intent.action.AUTO_ANSWER_CALL";
    public static final String AUTO_ANSWER_CALL_INTENT_EXTRA_KEY = "AUTO_ANSWER_CALL_EXTRA_KEY";

    public static final String ENABLE_GCM_INTENT = "com.waz.zclient.intent.action.ENABLE_GCM";
    public static final String DISABLE_GCM_INTENT = "com.waz.zclient.intent.action.DISABLE_GCM";

    public static final String CALLING_V2_INTENT = "com.waz.zclient.intent.action.CALLING_V2";
    public static final String CALLING_V3_INTENT = "com.waz.zclient.intent.action.CALLING_V3";
    public static final String CALLING_BE_SWITCH = "com.waz.zclient.intent.action.CALLING_BE_SWITCH";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(UserPreferencesController.USER_PREFS_TAG, Context.MODE_PRIVATE);
        switch (intent.getAction()) {
            case AUTO_ANSWER_CALL_INTENT:
                preferences.edit()
                    .putBoolean(
                        context.getString(R.string.pref_dev_auto_answer_call_key),
                        intent.getBooleanExtra(AUTO_ANSWER_CALL_INTENT_EXTRA_KEY, false))
                    .apply();
                break;
            case ENABLE_GCM_INTENT:
                preferences.edit()
                    .putBoolean(context.getString(R.string.pref_dev_gcm_enabled_key), true)
                    .apply();
                break;
            case DISABLE_GCM_INTENT:
                preferences.edit()
                    .putBoolean(context.getString(R.string.pref_dev_gcm_enabled_key), false)
                    .apply();
                break;
            case CALLING_V2_INTENT:
                preferences.edit()
                    .putString(context.getString(R.string.pref_dev_calling_v3_key), "0")
                    .apply();
                break;
            case CALLING_BE_SWITCH:
                preferences.edit()
                    .putString(context.getString(R.string.pref_dev_calling_v3_key), "1")
                    .apply();
                break;
            case CALLING_V3_INTENT:
                preferences.edit()
                    .putString(context.getString(R.string.pref_dev_calling_v3_key), "2")
                    .apply();
                break;
        }
    }
}
