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
package com.waz.zclient.core.stores.connect;

import android.os.Parcel;
import android.os.Parcelable;

import com.waz.api.ConversationsList;
import com.waz.api.IConversation;
import com.waz.api.ImageAsset;
import com.waz.api.InputStateIndicator;
import com.waz.api.MembersList;
import com.waz.api.Message;
import com.waz.api.MessageContent;
import com.waz.api.MessagesList;
import com.waz.api.UpdateListener;
import com.waz.api.User;
import com.waz.api.Verification;
import com.waz.api.EphemeralExpiration;

import java.util.ArrayList;
import java.util.List;


/**
 * Dummy conversation object to represent inbox link in conversation list
 */
public class InboxLinkConversation implements IConversation, UpdateListener {
    public static final String TAG = InboxLinkConversation.class.getName();

    public static final Parcelable.Creator<InboxLinkConversation> CREATOR
            = new Parcelable.Creator<InboxLinkConversation>() {
        public InboxLinkConversation createFromParcel(Parcel in) {
            return new InboxLinkConversation(in);
        }

        public InboxLinkConversation[] newArray(int size) {
            return new InboxLinkConversation[size];
        }
    };

    int size;
    private IConversation indicatorConversation;
    private List<UpdateListener> updateListeners = new ArrayList<>();
    private ConversationsList.SearchableConversationsList incomingConversations;

    public InboxLinkConversation(Parcel in) {
        size = in.readInt();
    }

    public InboxLinkConversation(ConversationsList.SearchableConversationsList incomingConversations) {
        init(incomingConversations);
    }

    public void init(ConversationsList.SearchableConversationsList incomingConversations) {
        this.size = incomingConversations.size();
        this.incomingConversations = incomingConversations;
        incomingConversations.addUpdateListener(this);
        updated();
    }

    public int getSize() {
        return size;
    }

    @Override
    public Type getType() {
        return Type.INCOMING_CONNECTION;
    }

    @Override
    public MessagesList getMessages() {
        return null;
    }

    @Override
    public MembersList getUsers() {
        return null;
    }

    @Override
    public String getId() {
        return TAG;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public boolean isMe() {
        return false;
    }

    @Override
    public boolean isOtto() {
        return false;
    }

    @Override
    public boolean isMemberOfConversation() {
        return false;
    }

    @Override
    public User getOtherParticipant() {
        return null;
    }

    @Override
    public int getUnreadCount() {
        return 0;
    }

    @Override
    public int getFailedCount() {
        return 0;
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public boolean isMuted() {
        return false;
    }

    @Override
    public void setArchived(boolean b) {

    }

    @Override
    public void setMuted(boolean b) {

    }

    @Override
    public ImageAsset getBackground() {
        return null;
    }

    @Override
    public void sendMessage(MessageContent msg) {

    }


    @Override
    public void setConversationName(String s) {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean hasMissedCall() {
        return false;
    }

    @Override
    public void addMembers(Iterable<? extends User> users) {

    }

    @Override
    public void removeMember(User user) {

    }

    @Override
    public void leave() {

    }

    @Override
    public void knock() {

    }

    @Override
    public Message getIncomingKnock() {
        return null;
    }

    @Override
    public InputStateIndicator getInputStateIndicator() {
        return null;
    }

    @Override
    public void clear() {
        if (incomingConversations != null) {
            incomingConversations.removeUpdateListener(this);
            indicatorConversation.removeUpdateListener(this);
        }
    }

    @Override
    public boolean isSelected() {
        return indicatorConversation.isSelected();
    }

    @Override
    public Verification getVerified() {
        return Verification.UNKNOWN;
    }

    @Override
    public void addUpdateListener(UpdateListener updateListener) {
        updateListeners.add(updateListener);
    }

    @Override
    public void removeUpdateListener(UpdateListener updateListener) {
        updateListeners.remove(updateListener);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(0);
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void updated() {
        if (incomingConversations.size() > 0) {
            indicatorConversation = incomingConversations.get(0);
            indicatorConversation.addUpdateListener(this);
            for (UpdateListener updateListener : updateListeners) {
                updateListener.updated();
            }
        }
    }

    public boolean isEphemeral() {
        return false;
    }

    public EphemeralExpiration getEphemeralExpiration() {
        return null;
    }

    public void setEphemeralExpiration(EphemeralExpiration expiration) {

    }
}
