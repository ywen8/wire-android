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
package com.waz.zclient.core.api.scala;

import com.waz.api.IConversation;
import com.waz.api.UpdateListener;
import com.waz.zclient.core.stores.participants.ParticipantsStore;

public class ScalaParticipantsStore extends ParticipantsStore {
    public static final String TAG = ScalaParticipantsStore.class.getName();

    @Override
    public void setCurrentConversation(IConversation conversation) {
        unregisterListener();
        if (conversation == null) {
            return;
        }
        this.conversation = conversation;
        this.conversation.addUpdateListener(conversationUpdateListener);
        notifyConversationChanged(conversation);
        switch (conversation.getType()) {
            case GROUP:
                participants = conversation.getUsers();
                participants.addUpdateListener(participantListener);
                participantListener.updated();
                break;
            case SELF:
                break;
            case ONE_TO_ONE:
                otherUser = conversation.getOtherParticipant();
                otherUser.addUpdateListener(otherUserListener);
                otherUserListener.updated();
                break;
            case WAIT_FOR_CONNECTION:
                break;
        }
    }

    @Override
    public void tearDown() {
        unregisterListener();

        participants = null;
        conversation = null;
        otherUser = null;
    }

    private void unregisterListener() {
        if (participants != null) {
            participants.removeUpdateListener(participantListener);
            participants = null;
        }

        if (this.conversation != null) {
            this.conversation.removeUpdateListener(conversationUpdateListener);
        }

        if (otherUser != null) {
            otherUser.removeUpdateListener(otherUserListener);
        }
    }

    private UpdateListener otherUserListener = new UpdateListener() {

        @Override
        public void updated() {
            notifyOtherUserUpdated(otherUser);
        }
    };

    private UpdateListener participantListener = new UpdateListener() {

        @Override
        public void updated() {
            notifyParticipantsUpdated(participants);
        }
    };

    private UpdateListener conversationUpdateListener = new UpdateListener() {
        @Override
        public void updated() {
            notifyConversationChanged(conversation);
        }
    };
}
