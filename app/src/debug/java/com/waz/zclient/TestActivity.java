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
package com.waz.zclient;

import android.os.Bundle;
import com.waz.zclient.controllers.IControllerFactory;
import com.waz.zclient.core.stores.IStoreFactory;

public class TestActivity extends BaseActivity {
    /**
     * Stub out enough of the Store and Controller factories to get the BaseActivity running (it relies on
     * getControllerFactory()) and then we can set these later using mockito.
     */
    private IStoreFactory mockStoreFactory = new StubStoreFactory();

    private IControllerFactory mockControllerFactory = new StubControllerFactory();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blank);
    }

    public void setMockStoreFactory(IStoreFactory mockStoreFactory) {
        this.mockStoreFactory = mockStoreFactory;
    }

    public void setMockControllerFactory(IControllerFactory mockControllerFactory) {
        this.mockControllerFactory = mockControllerFactory;
    }

    @Override
    public IStoreFactory getStoreFactory() {
        return mockStoreFactory;
    }

    @Override
    public IControllerFactory getControllerFactory() {
        return mockControllerFactory;
    }
}
