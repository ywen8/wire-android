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
package com.waz.zclient.utils;

import android.content.Context;
import com.waz.api.OtrClient;
import com.waz.zclient.R;

public final class OtrUtils {

    private static final String BOLD_PREFIX = "[[";
    private static final String BOLD_SUFFIX = "]]";
    private static final String SEPARATOR = " ";

    private OtrUtils() {

    }

    public static String getFormattedFingerprint(String fingerprint) {
        return getFormattedString(fingerprint, 2);
    }

    public static String getFormattedString(String string, int chunkSize) {
        int currentChunkSize = 0;
        boolean bold = true;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            if (currentChunkSize == 0 && bold) {
                sb.append(BOLD_PREFIX);
            }
            sb.append(string.charAt(i));
            currentChunkSize++;

            if (currentChunkSize == chunkSize || i == string.length() - 1) {
                if (bold) {
                    sb.append(BOLD_SUFFIX);
                }
                bold = !bold;
                if (i < string.length() - 1) {
                    sb.append(SEPARATOR);
                }
                currentChunkSize = 0;
            }
        }
        return sb.toString();
    }

    public static String getDeviceClassName(Context context, OtrClient otrClient) {
        if (otrClient == null || otrClient.getType() == null) {
            return context.getString(R.string.otr__participant__device_class__unknown);
        }
        switch (otrClient.getType()) {
            case DESKTOP:
                return context.getString(R.string.otr__participant__device_class__desktop);
            case PHONE:
                return context.getString(R.string.otr__participant__device_class__phone);
            case TABLET:
                return context.getString(R.string.otr__participant__device_class__tablet);
            default:
                return context.getString(R.string.otr__participant__device_class__unknown);
        }
    }
}
