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
package com.waz.zclient.pages.main.profile.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.TypedValue;
import com.waz.api.Location;
import com.waz.api.OtrClient;
import com.waz.zclient.R;
import com.waz.zclient.ui.utils.TextViewUtils;
import com.waz.zclient.utils.StringUtils;
import com.waz.zclient.utils.ZTimeFormatter;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

public class DevicesPreferencesUtil {

    private static final String BOLD_PREFIX = "[[";
    private static final String BOLD_SUFFIX = "]]";
    private static final char SEPARATOR = ' ';
    private static final char NEW_LINE = '\n';
    private static final char UNKNOWN_LOCATION = '?';

    public static CharSequence getTitle(Context context, OtrClient otrClient) {
        return TextViewUtils.getBoldText(context,
                                         context.getString(R.string.pref_devices_device_title,
                                                           StringUtils.capitalise(otrClient.getModel())));
    }

    public static CharSequence getSummary(Context context,
                                          OtrClient otrClient,
                                          boolean includeActivationSummary) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {android.R.attr.textColorPrimary});
        int highlightColor = a.getColor(0, 0);
        a.recycle();
        StringBuilder sb = new StringBuilder();
        sb.append(context.getString(R.string.pref_devices_device_id, otrClient.getDisplayId()));
        int highlightEnd = sb.length();
        if (includeActivationSummary) {
            sb.append(NEW_LINE)
              .append(NEW_LINE)
              .append(getActivationSummary(context, otrClient));
        }
        return TextViewUtils.getBoldHighlightText(context,
                                                  sb.toString(),
                                                  highlightColor,
                                                  0,
                                                  highlightEnd);
    }

    private static String getActivationSummary(Context context, OtrClient otrClient) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        String time = ZTimeFormatter.getSeparatorTime(context,
                                                      now,
                                                      LocalDateTime.ofInstant(otrClient.getRegTime(),
                                                                              ZoneId.systemDefault()),
                                                      DateFormat.is24HourFormat(context),
                                                      ZoneId.systemDefault(),
                                                      false);
        Location location = otrClient.getRegLocation();
        String regLocation = location == null ? "" : location.getDisplayName();
        return context.getString(R.string.pref_devices_device_activation_summary,
                                 time,
                                 StringUtils.isBlank(regLocation) ? UNKNOWN_LOCATION : regLocation);
    }

    public static CharSequence getFormattedFingerprint(Context context, String fingerprint) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {android.R.attr.textColorPrimary});
        int highlightColor = a.getColor(0, 0);
        a.recycle();
        final String formattedFingerprint = getFormattedFingerprint(fingerprint);
        return TextViewUtils.getBoldHighlightText(context,
                                                  formattedFingerprint,
                                                  highlightColor,
                                                  0,
                                                  formattedFingerprint.length());
    }

    private static String getFormattedFingerprint(String fingerprint) {
        return getFormattedString(fingerprint, 2);
    }

    private static String getFormattedString(String string, int chunkSize) {
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
                if (i == string.length() - 1) {
                    sb.append(NEW_LINE);
                } else {
                    sb.append(SEPARATOR);
                }
                currentChunkSize = 0;
            }
        }
        return sb.toString();
    }

}
