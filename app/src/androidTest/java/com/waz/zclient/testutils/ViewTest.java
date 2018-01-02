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
package com.waz.zclient.testutils;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.view.ViewGroup;
import com.waz.threading.Threading$;
import com.waz.zclient.R;
import com.waz.zclient.TestActivity;
import com.waz.zclient.mock.MockControllerFactory;
import com.waz.zclient.mock.MockStoreFactory;
import com.waz.zclient.utils.ViewUtils;
import org.junit.Before;
import org.junit.Rule;

public class ViewTest<A extends TestActivity> {

    @Rule
    public ActivityTestRule<A> activityTestRule;

    protected Instrumentation instrumentation;

    protected A activity;
    private Class<A> activityType;
    private ViewGroup rootView;
    private Handler handler;

    public ViewTest(Class<A> activityType) {
        this.activityType = activityType;
        activityTestRule = new ActivityTestRule<>(activityType, false, false);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        handler = new Handler();
    }

    @Before
    public void setup() throws InterruptedException {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        activityTestRule.launchActivity(new Intent(instrumentation.getContext(), activityType));
        Threading$.MODULE$.AssertsEnabled_$eq(false);

        activity = activityTestRule.getActivity();
        activity.setMockStoreFactory(new MockStoreFactory());
        activity.setMockControllerFactory(new MockControllerFactory());
        rootView = ViewUtils.getView(activity, R.id.test_content);
    }

    protected void setView(final View view) {
        rootView.post(new Runnable() {
            @Override
            public void run() {
                rootView.removeAllViews();
                rootView.addView(view);
            }
        });
    }

}
