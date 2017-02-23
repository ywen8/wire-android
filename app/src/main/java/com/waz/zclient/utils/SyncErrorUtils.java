/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.waz.zclient.utils;

import android.content.Context;
import com.waz.api.ErrorsList;
import com.waz.zclient.R;

public class SyncErrorUtils {

    public static String getGroupErrorMessage(Context context, ErrorsList.ErrorDescription error) {
        if (context == null || error == null) {
            return "";
        }
        switch (error.getType()) {
            case CANNOT_ADD_UNCONNECTED_USER_TO_CONVERSATION:
                int userCount = getIteratorSize(error.getUsers());
                if (userCount == 1) {
                    return context.getResources().getString(R.string.in_app_notification__sync_error__add_user__body,
                                                    error.getUsers().iterator().next().getName());
                } else {
                    return context.getString(R.string.in_app_notification__sync_error__add_multiple_user__body);
                }
            case CANNOT_CREATE_GROUP_CONVERSATION_WITH_UNCONNECTED_USER:
                return context.getResources().getString(R.string.in_app_notification__sync_error__create_group_convo__body,
                                                error.getConversation().getName());
            default:
                return context.getString(R.string.in_app_notification__sync_error__unknown__body);
        }
    }

    private static int getIteratorSize(Iterable iterable) {
        if (iterable == null) {
            return 0;
        }
        int size = 0;
        for (Object ignored : iterable) {
            size++;
        }
        return size;
    }

}
