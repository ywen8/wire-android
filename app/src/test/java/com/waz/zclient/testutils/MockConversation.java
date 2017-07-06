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
package com.waz.zclient.testutils;

import android.annotation.SuppressLint;
import android.os.Parcel;
import com.waz.api.EphemeralExpiration;
import com.waz.api.IConversation;
import com.waz.api.ImageAsset;
import com.waz.api.InputStateIndicator;
import com.waz.api.MembersList;
import com.waz.api.Message;
import com.waz.api.MessageContent;
import com.waz.api.User;
import com.waz.api.Verification;

@SuppressLint("ParcelCreator")
public class MockConversation extends MockObservable implements IConversation {

    public MockConversation(int id) {
        super(id);
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public MembersList getUsers() {
        return null;
    }

    @Override
    public String getId() {
        return Integer.toString(id);
    }

    @Override
    public String getName() {
        return null;
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
    public void setArchived(boolean archived) {

    }

    @Override
    public void setMuted(boolean muted) {

    }

    @Override
    public ImageAsset getBackground() {
        return null;
    }

    @Override
    public void sendMessage(MessageContent msg) {

    }

    @Override
    public void setConversationName(String name) {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public Verification getVerified() {
        return null;
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

    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public boolean isEphemeral() {
        return false;
    }

    @Override
    public EphemeralExpiration getEphemeralExpiration() {
        return null;
    }

    @Override
    public void setEphemeralExpiration(EphemeralExpiration expiration) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
