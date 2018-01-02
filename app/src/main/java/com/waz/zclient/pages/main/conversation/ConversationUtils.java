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
package com.waz.zclient.pages.main.conversation;

import android.content.Context;
import android.content.res.TypedArray;
import com.waz.api.IConversation;
import com.waz.zclient.R;

public class ConversationUtils {
    private ConversationUtils() {
    }

    public static int getListUnreadIndicatorRadiusPx(Context context, int count) {
        if (count == 0) {
            return 0;
        }
        // check the limits
        int[] limits = context.getResources().getIntArray(R.array.list_unread_indicator_radiuses_limits);
        int limitId = 0;
        for (int i = 0; i < limits.length; i++) {
            if (count < limits[i]) {
                break;
            }
            limitId = i;
        }

        // get radius
        int radius = 0;
        TypedArray ta = null;
        try {
            ta = context.getResources().obtainTypedArray(R.array.list_unread_indicator_radiuses);
            if (ta != null) {
                radius = ta.getDimensionPixelSize(limitId, 0);
            }
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
        return radius;
    }

    public static int getMaxIndicatorRadiusPx(Context context) {
        // Return biggest indicator radius
        int[] limits = context.getResources().getIntArray(R.array.list_unread_indicator_radiuses_limits);
        int limitId = limits.length - 1;

        // get radius
        int radius = 0;
        TypedArray ta = null;
        try {
            ta = context.getResources().obtainTypedArray(R.array.list_unread_indicator_radiuses);
            if (ta != null) {
                radius = ta.getDimensionPixelSize(limitId, 0);
            }
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
        return radius;
    }

    public static boolean isConversationEqual(IConversation conversation1, IConversation conversation2) {
        if (conversation1 == null) {
            return conversation2 == null;
        } else if (conversation2 == null) {
            return false;
        }
        return conversation1.getId().equals(conversation2.getId());
    }

}
