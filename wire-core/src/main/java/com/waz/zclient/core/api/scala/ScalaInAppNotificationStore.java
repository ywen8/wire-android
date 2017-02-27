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
package com.waz.zclient.core.api.scala;

import com.waz.api.ErrorsList;
import com.waz.api.IncomingMessagesList;
import com.waz.api.Message;
import com.waz.api.ZMessagingApi;
import com.waz.zclient.core.stores.inappnotification.InAppNotificationStore;

public class ScalaInAppNotificationStore extends InAppNotificationStore implements IncomingMessagesList.MessageListener,
                                                                                   IncomingMessagesList.KnockListener,
                                                                                   ErrorsList.ErrorListener {

    private IncomingMessagesList incomingMessages;
    private ErrorsList syncErrors;

    public ScalaInAppNotificationStore(ZMessagingApi zMessagingApi) {
        incomingMessages = zMessagingApi.getIncomingMessages();
        incomingMessages.addMessageListener(this);
        incomingMessages.addKnockListener(this);

        syncErrors = zMessagingApi.getErrors();
        syncErrors.addErrorListener(this);
    }

    @Override
    public void tearDown() {
        incomingMessages.removeMessageListener(this);
        incomingMessages.removeKnockListener(this);
        incomingMessages = null;

        syncErrors.removeErrorListener(this);
        syncErrors = null;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Notifications
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void dismissError(String errorId) {
        for (int i = 0, length = syncErrors.size(); i < length; i++) {
            ErrorsList.ErrorDescription error = syncErrors.get(i);
            if (error.getId().equals(errorId)) {
                error.dismiss();
            }
        }
    }

    @Override
    public ErrorsList.ErrorDescription getError(String errorId) {
        for (int i = 0, length = syncErrors.size(); i < length; i++) {
            ErrorsList.ErrorDescription error = syncErrors.get(i);
            if (error.getId().equals(errorId)) {
                return error;
            }
        }
        return null;
    }

    @Override
    public void onIncomingMessage(Message message) {
        notifyIncomingMessageObservers(message);
    }

    @Override
    public void onError(ErrorsList.ErrorDescription error) {
        notifySyncErrorObservers(error);
    }

    @Override
    public void onKnock(final Message message) {
        notifyIncomingKnock(message);
    }

}
