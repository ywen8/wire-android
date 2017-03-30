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
package com.waz.zclient.core.stores.participants;


import com.waz.api.IConversation;
import com.waz.api.User;
import com.waz.api.UsersList;

import java.util.HashSet;
import java.util.Set;

public abstract class ParticipantsStore implements IParticipantsStore {
    private static final String TAG = ParticipantsStore.class.getName();
    protected UsersList participants;
    protected IConversation conversation;
    protected User otherUser;

    // observers attached to a IParticipantsStore
    protected Set<ParticipantsStoreObserver> participantsStoreObservers = new HashSet<>();

    @Override
    public void addParticipantsStoreObserver(ParticipantsStoreObserver participantsStoreObserver) {
        participantsStoreObservers.add(participantsStoreObserver);

        if (conversation != null) {
            notifyConversationChanged(conversation);
            switch (conversation.getType()) {
                case GROUP:
                    if (participants == null) {
                        participants = conversation.getUsers();
                    }
                    notifyParticipantsUpdated(participants);
                    break;
                case SELF:
                    break;
                case ONE_TO_ONE:
                    if (otherUser == null) {
                        otherUser = conversation.getOtherParticipant();
                    }
                    notifyOtherUserUpdated(otherUser);
                    break;
                case WAIT_FOR_CONNECTION:
                    break;
            }

        }
    }

    @Override
    public void removeParticipantsStoreObserver(ParticipantsStoreObserver participantsStoreObserver) {
        participantsStoreObservers.remove(participantsStoreObserver);
    }

    protected void notifyConversationChanged(IConversation conversation) {
        for (ParticipantsStoreObserver observer : participantsStoreObservers) {
            observer.conversationUpdated(conversation);
        }
    }

    protected void notifyParticipantsUpdated(UsersList participants) {
        for (ParticipantsStoreObserver observer : participantsStoreObservers) {
            observer.participantsUpdated(participants);
        }
    }

    protected void notifyOtherUserUpdated(User otherUser) {
        for (ParticipantsStoreObserver observer : participantsStoreObservers) {
            observer.otherUserUpdated(otherUser);
        }
    }
}
