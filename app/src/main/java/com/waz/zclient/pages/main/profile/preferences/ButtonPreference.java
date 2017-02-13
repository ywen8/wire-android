/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.waz.zclient.pages.main.profile.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import com.waz.zclient.ui.views.ZetaButton;

public class ButtonPreference extends Preference {

    private ZetaButton zetaButton;
    private static final int UNINITIALIZED = Color.TRANSPARENT;
    private int accentColor = UNINITIALIZED;

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonPreference(Context context) {
        super(context);
    }

    @Override
    @SuppressLint("com.waz.ViewUtils")
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        zetaButton = (ZetaButton) holder.findViewById(android.R.id.title);
        zetaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performClick();
            }
        });
        zetaButton.setIsFilled(false);
        if (accentColor != UNINITIALIZED) {
            zetaButton.setAccentColor(accentColor, true);
        }
    }

    public void setAccentColor(int color) {
        accentColor = color;
        if (zetaButton != null) {
            zetaButton.setAccentColor(color, true);
        }
    }

}
