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
package com.waz.zclient.core.stores.api;

import android.content.Context;
import com.waz.api.InitListener;
import com.waz.api.Self;
import com.waz.api.UpdateListener;
import com.waz.api.ZMessagingApi;
import com.waz.api.ZMessagingApiFactory;

import java.util.HashSet;
import java.util.Set;

public class ZMessagingApiStore implements IZMessagingApiStore,
                                           InitListener,
                                           UpdateListener {

    private Set<ZMessagingApiStoreObserver> observerSet;
    private Context context;
    private ZMessagingApi zMessagingApi;
    private Self self;

    public ZMessagingApiStore(Context context) {
        this.context = context;
        this.observerSet = new HashSet<>();
    }

    @Override
    public void addApiObserver(ZMessagingApiStoreObserver observer) {
        observerSet.add(observer);

        if (isInitialized()) {
            observer.onInitialized(self);
            if (!self.isUpToDate()) {
                observer.onForceClientUpdate();
            }
        } else {
            // force initialization
            getApi();
        }
    }

    @Override
    public void removeApiObserver(ZMessagingApiStoreObserver observer) {
        observerSet.remove(observer);
    }

    @Override
    public boolean isInitialized() {
        return self != null;
    }

    @Override
    public ZMessagingApi getApi() {
        if (zMessagingApi == null) {
            zMessagingApi = ZMessagingApiFactory.getInstance(context);
            zMessagingApi.onCreate(context);
            zMessagingApi.onInit(this);
        }
        return zMessagingApi;
    }

    @Override
    public void logout() {
        zMessagingApi.logout();
    }

    @Override
    public void delete() {
        if (self == null) {
            return;
        }
        self.deleteAccount();
    }

    @Override
    public void tearDown() {
        observerSet.clear();
        if (zMessagingApi != null) {
            zMessagingApi.onDestroy();
            zMessagingApi = null;
        }
        self.removeUpdateListener(this);
        self = null;
        context = null;
    }

    @Override
    public void onInitialized(Self self) {
        this.self = self;
        self.addUpdateListener(this);
        notifyOnInitialized();
    }

    private void notifyOnInitialized() {
        for (ZMessagingApiStoreObserver observer : observerSet) {
            observer.onInitialized(self);
        }
    }

    private void notifyLoggedOut() {
        for (ZMessagingApiStoreObserver observer : observerSet) {
            observer.onLogout();
        }
    }

    private void notifyForceUpdate() {
        for (ZMessagingApiStoreObserver observer : observerSet) {
            observer.onForceClientUpdate();
        }
    }


    @Override
    public void updated() {
        if (!self.isLoggedIn()) {
            notifyLoggedOut();
        }

        if (!self.isUpToDate()) {
            notifyForceUpdate();
        }
    }
}
