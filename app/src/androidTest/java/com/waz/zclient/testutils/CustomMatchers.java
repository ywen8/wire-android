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

import android.content.res.Resources;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.waz.zclient.pages.main.profile.views.GuidedEditText;
import com.waz.zclient.ui.text.TypefaceEditText;
import com.waz.zclient.ui.views.e2ee.OtrSwitch;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CustomMatchers {
    /**
     * Typing with regular {@link android.support.test.espresso.matcher.ViewMatchers#withId(int)} matchers won't work on a
     * GuidedEditText (GET) because the GET hides the EditText which Espresso needs. This matcher searches for an edit
     * text under a grandparent with the given id that has the type GET
     * @param id the id of the GuidedEditText which we want to type (or perform other actions) into
     * @return an Espresso compatible Matcher<View> for use in onView();
     */
    public static Matcher<View> guidedEditTextWithId(final int id) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            @Override
            public void describeTo(Description description) {
                String idDescription = Integer.toString(id);
                if (resources != null) {
                    try {
                        idDescription = resources.getResourceName(id);
                    } catch (Resources.NotFoundException e) {
                        // No big deal, will just use the int value.
                        idDescription = String.format("%s (resource name not found)", id);
                    }
                }
                description.appendText("with id: " + idDescription);
            }

            @Override
            public boolean matchesSafely(View view) {
                resources = view.getResources();
                GuidedEditText grandParent = getGrandParent(view);
                return grandParent != null &&
                       id == grandParent.getId() &&
                       view instanceof TypefaceEditText;
            }

            private GuidedEditText getGrandParent(View view) {
                ViewParent parent = view.getParent();
                if (!(parent instanceof FrameLayout)) {
                    return null;
                }
                ViewParent grandParent = parent.getParent();
                if (!(grandParent instanceof GuidedEditText)) {
                    return null;
                }
                return (GuidedEditText) grandParent;
            }
        };
    }

    public static Matcher<View> otrSwitchWithId(final int id) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            @Override
            public void describeTo(Description description) {
                String idDescription = Integer.toString(id);
                if (resources != null) {
                    try {
                        idDescription = resources.getResourceName(id);
                    } catch (Resources.NotFoundException e) {
                        // No big deal, will just use the int value.
                        idDescription = String.format("%s (resource name not found)", id);
                    }
                }
                description.appendText("with id: " + idDescription);
            }

            @Override
            public boolean matchesSafely(View view) {
                resources = view.getResources();
                ViewParent parent = view.getParent();
                return parent != null &&
                       parent instanceof OtrSwitch &&
                       id == ((OtrSwitch) parent).getId() &&
                       view instanceof SwitchCompat;
            }
        };
    }

}
