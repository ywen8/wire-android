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
package com.waz.zclient.controllers.sharing;

import com.waz.api.IConversation;
import com.waz.utils.wrappers.URI;

import java.util.List;

public class SharingController implements ISharingController {

    private SharedContentType sharedContentType;

    private String sharedText;
    private List<URI> sharedFileUris;
    private String conversationId;

    @Override
    public void tearDown() { }

    @Override
    public void setSharedContentType(SharedContentType type) {
        if (type == null) {
            return;

        }
        sharedContentType = type;
    }

    @Override
    public void setSharedText(String text) {
        sharedText = text;
    }

    @Override
    public String getSharedText() {
        return sharedText;
    }

    @Override
    public void setSharedUris(List<URI> imageUris) {
        sharedFileUris = imageUris;
    }

    @Override
    public List<URI> getSharedFileUris() {
        return sharedFileUris;
    }

    @Override
    public void setSharingConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    @Override
    public void maybeResetSharedText(IConversation currentConversation) {
        if (currentConversation == null) {
            return;
        }

        if (!currentConversation.getId().equals(conversationId)) {
            return;
        }

        conversationId = null;
        sharedText = null;
    }

    @Override
    public void maybeResetSharedUris(IConversation currentConversation) {
        if (currentConversation == null) {
            return;
        }

        if (!currentConversation.getId().equals(conversationId)) {
            return;
        }

        if (sharedContentType != SharedContentType.FILE) {
            return;
        }

        conversationId = null;
        sharedFileUris = null;
    }

    @Override
    public boolean isSharedConversation(IConversation conversation) {
        if (conversationId == null ||
            conversation == null) {
            return false;
        }
        return conversationId.equals(conversation.getId());
    }

}
