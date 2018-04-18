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
package com.waz.zclient.ui.text;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.waz.zclient.ui.utils.TypefaceUtils;

import com.waz.zclient.R;


public class TypefaceTextView extends TextView {

    private String transform;

    public TypefaceTextView(Context context) {
        this(context, null);
    }

    public TypefaceTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TypefaceTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            return;
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TypefaceTextView,
                0, 0);

        String font = a.getString(R.styleable.TypefaceTextView_w_font);
        if (!TextUtils.isEmpty(font)) {
            setTypeface(font);
        }

        transform = a.getString(R.styleable.TypefaceTextView_transform);
        if (!TextUtils.isEmpty(transform) && getText() != null) {
            setTransformedText(getText().toString(), transform);
        }

        a.recycle();
        setSoundEffectsEnabled(false);
    }

    public void setTypeface(String font) {
        if (!isInEditMode()) {
            setTypeface(TypefaceUtils.getTypeface(font));
        }
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public void setTransformedText(String text, String transform) {
        TextTransform transformer = TextTransform.get(transform);
        this.setText(transformer.transform(text));
    }

    public void setTransformedText(String text) {
        TextTransform transformer = TextTransform.get(this.transform);
        this.setText(transformer.transform(text));
    }

}
