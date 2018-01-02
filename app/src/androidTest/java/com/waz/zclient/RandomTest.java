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

import android.test.InstrumentationTestCase;
import com.waz.zclient.ui.utils.ResourceUtils;

public class RandomTest extends InstrumentationTestCase {

    public void testRandomInteger() {
        int testnumber = 7;
        int runs = 1000;

        int[] distribution = new int[testnumber];


        for (int i = 0; i < runs; i++) {
            int rand = ResourceUtils.randInt(0, testnumber - 1);
            if (rand >= testnumber) {
                fail("Integer bigger than biggest number: " + rand);
            }
            if (rand < 0) {
                fail("Integer smaller than 0");
            }
            distribution[rand]++;
        }
    }
}
