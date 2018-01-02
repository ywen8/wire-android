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

import com.waz.api.UpdateListener;
import com.waz.api.User;
import com.waz.zclient.core.stores.singleparticipants.SingleParticipantStore;
import com.waz.zclient.core.stores.singleparticipants.SingleParticipantStoreObserver;

public class ScalaSingleParticipantStore extends SingleParticipantStore implements UpdateListener {
    private User currentUser;

    @Override
    public void addSingleParticipantObserver(SingleParticipantStoreObserver singleParticipantObserver) {
        super.addSingleParticipantObserver(singleParticipantObserver);
        if (currentUser != null) {
            singleParticipantObserver.onUserUpdated(currentUser);
        }
    }

    @Override
    public void tearDown() {
        unregisterListener();
        currentUser = null;
    }

    @Override
    public void setUser(User user) {
        unregisterListener();
        currentUser = user;
        user.addUpdateListener(this);
        notifyUserHasBeenUpdated(currentUser);
    }

    @Override
    public User getUser() {
        return currentUser;
    }

    private void unregisterListener() {
        if (currentUser != null) {
            currentUser.removeUpdateListener(this);
        }
    }

    @Override
    public void updated() {
        notifyUserHasBeenUpdated(currentUser);
    }

}
