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

import static com.waz.zclient.controllers.userpreferences.IUserPreferencesController.DO_NOT_SHOW_SHARE_CONTACTS_DIALOG;
import static com.waz.zclient.controllers.userpreferences.UserPreferencesController.USER_PREF_ACTION_PREFIX;

public class PreferenceReceiver extends BroadcastReceiver {

    public static final String AUTO_ANSWER_CALL_INTENT = "com.waz.zclient.intent.action.AUTO_ANSWER_CALL";
    public static final String AUTO_ANSWER_CALL_INTENT_EXTRA_KEY = "AUTO_ANSWER_CALL_EXTRA_KEY";

    public static final String ENABLE_GCM_INTENT = "com.waz.zclient.intent.action.ENABLE_GCM";
    public static final String DISABLE_GCM_INTENT = "com.waz.zclient.intent.action.DISABLE_GCM";

    public static final String SILENT_MODE = "com.waz.zclient.intent.action.SILENT_MODE";
    public static final String NO_CONTACT_SHARING = "com.waz.zclient.intent.action.NO_CONTACT_SHARING";

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
                    .putBoolean(context.getString(R.string.pref_dev_push_enabled_key), true)
                    .apply();
                break;
            case DISABLE_GCM_INTENT:
                preferences.edit()
                    .putBoolean(context.getString(R.string.pref_dev_push_enabled_key), false)
                    .apply();
                break;
            case SILENT_MODE:
                preferences.edit()
                    .putString(context.getString(R.string.pref_options_ringtones_ringtone_key), "")
                    .apply();
                preferences.edit()
                    .putString(context.getString(R.string.pref_options_ringtones_ping_key), "")
                    .apply();
                preferences.edit()
                    .putString(context.getString(R.string.pref_options_ringtones_text_key), "")
                    .apply();
                break;
            case NO_CONTACT_SHARING:
                preferences.edit()
                    .putBoolean(USER_PREF_ACTION_PREFIX + DO_NOT_SHOW_SHARE_CONTACTS_DIALOG, true)
                    .apply();
                break;
        }
    }
}
