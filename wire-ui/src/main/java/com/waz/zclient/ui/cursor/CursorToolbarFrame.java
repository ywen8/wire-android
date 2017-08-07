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
package com.waz.zclient.ui.cursor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.waz.zclient.ui.R;
import com.waz.zclient.utils.ViewUtils;


public class CursorToolbarFrame extends FrameLayout {

    private final int heightExpanded;
    private final int heightShrinked;

    public void expand() {
        ViewUtils.setHeight(this, heightExpanded);
        requestLayout();
    }

    public void shrink() {
        ViewUtils.setHeight(this, heightShrinked);
        requestLayout();
    }
    public CursorToolbarFrame(Context context) {
        this(context, null);
    }

    public CursorToolbarFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CursorToolbarFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        heightExpanded = getResources().getDimensionPixelSize(R.dimen.new_cursor_height);
        heightShrinked = getResources().getDimensionPixelSize(R.dimen.cursor_height_shrinked);
    }
}
