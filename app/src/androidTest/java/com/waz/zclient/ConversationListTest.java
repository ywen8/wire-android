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

import android.content.Context;
import android.test.InstrumentationTestCase;
import com.waz.zclient.pages.main.conversation.ConversationUtils;
import com.waz.zclient.utils.ViewUtils;

public class ConversationListTest extends InstrumentationTestCase {

    Context context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext().getApplicationContext();
    }

    public void testGetListUnreadIndicatorRadiusesCount0() {
        int expectedRadius = ViewUtils.toPx(context, 0);
        int count = 0;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount1() {
        int expectedRadius = ViewUtils.toPx(context, 2);
        int count = 1;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount2() {
        int expectedRadius = ViewUtils.toPx(context, 4);
        int count = 2;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount3() {
        int expectedRadius = ViewUtils.toPx(context, 4);
        int count = 3;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount8() {
        int expectedRadius = ViewUtils.toPx(context, 4);
        int count = 8;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount9() {
        int expectedRadius = ViewUtils.toPx(context, 4);
        int count = 9;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount10() {
        int expectedRadius = ViewUtils.toPx(context, 6);
        int count = 10;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount11() {
        int expectedRadius = ViewUtils.toPx(context, 6);
        int count = 11;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount12() {
        int expectedRadius = ViewUtils.toPx(context, 6);
        int count = 12;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }

    public void testGetListUnreadIndicatorRadiusesCount13() {
        int expectedRadius = ViewUtils.toPx(context, 6);
        int count = 13;
        assertEquals(expectedRadius, ConversationUtils.getListUnreadIndicatorRadiusPx(context, count));
    }
}
