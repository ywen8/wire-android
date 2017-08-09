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
package com.waz.zclient.core.controllers.tracking.attributes;


import com.waz.api.IConversation;

public enum ConversationType {
    GROUP_CONVERSATION("group"),
    ONE_TO_ONE_CONVERSATION("1:1")
    ;

    public final String name;

    ConversationType(String name) {
        this.name = name;
    }

    public static ConversationType getValue(IConversation conversation) {
        return getValue(conversation.getType());
    }

    public static ConversationType getValue(IConversation.Type type) {
        return type == IConversation.Type.GROUP ? GROUP_CONVERSATION : ONE_TO_ONE_CONVERSATION;
    }
}
