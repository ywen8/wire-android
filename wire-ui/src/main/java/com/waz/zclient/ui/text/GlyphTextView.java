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
package com.waz.zclient.ui.text;

import android.content.Context;
import android.util.AttributeSet;
import com.waz.zclient.ui.utils.TypefaceUtils;

public class GlyphTextView extends android.support.v7.widget.AppCompatTextView {

    public GlyphTextView(Context context) {
        super(context);
        init();
    }

    public GlyphTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlyphTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            setTypeface(TypefaceUtils.getTypeface(TypefaceUtils.getGlyphsTypefaceName()));
        }
        setSoundEffectsEnabled(false);
    }
}
