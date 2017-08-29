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
package com.wire.testinggallery;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String COMMAND = "command";
    private static final String PACKAGE_NAME = "package";
    private static final String COMMAND_SHARE_TEXT = "share_text";
    private static final String COMMAND_SHARE_IMAGE = "share_image";
    private static final String COMMAND_SHARE_VIDEO = "share_video";
    private static final String COMMAND_SHARE_AUDIO = "share_audio";
    private static final String COMMAND_SHARE_FILE = "share_file";
    private static final String COMMAND_CHECK_NOTIFICATION_ACCESS = "check_notification_access";
    private static final String DEFAULT_TEST_TEXT = "QA AUTOMATION TEST";
    private static final String DEFAULT_PACKAGE_NAME = "com.wire.candidate";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        registerReceiver(broadcastReceiver, new IntentFilter("com.wire.testinggallery.main.receiver"));
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent shareIntent;
            String command = intent.getStringExtra(COMMAND);
            String packageName = intent.getStringExtra(PACKAGE_NAME) == null ?
                DEFAULT_PACKAGE_NAME : intent.getStringExtra(PACKAGE_NAME);
            if (command.startsWith("share")) {
                if (command.equals(COMMAND_SHARE_TEXT)) {
                    shareIntent = ShareCompat.IntentBuilder.from(MainActivity.this)
                        .setType("text/plain")
                        .setText(DEFAULT_TEST_TEXT)
                        .getIntent();
                } else {
                    Uri uri = getLatestAssetUriByCommand(command);
                    shareIntent = ShareCompat.IntentBuilder.from(MainActivity.this)
                        .setType(getContentResolver().getType(uri))
                        .setStream(uri)
                        .getIntent();
                }
                shareIntent.setPackage(packageName);
                startActivity(shareIntent);
            } else {
                switch (command) {
                    case COMMAND_CHECK_NOTIFICATION_ACCESS:
                        if (Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
                            setResultData("VERIFIED");
                        } else {
                            setResultData("UNVERIFIED");
                        }
                        break;
                    default:
                        throw new RuntimeException(String.format("Cannot identify your command [%s]", command));
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private Uri getLatestAssetUriByCommand(String command) {
        DocumentResolver resolver = new DocumentResolver(getContentResolver());
        switch (command) {
            case COMMAND_SHARE_FILE:
                return resolver.getDocumentUri();
            case COMMAND_SHARE_IMAGE:
                return resolver.getImageUri();
            case COMMAND_SHARE_VIDEO:
                return resolver.getVideoUri();
            case COMMAND_SHARE_AUDIO:
                return resolver.getAudioUri();
            default:
                throw new RuntimeException(String.format("Cannot identify the command : %s", command));
        }
    }

}
