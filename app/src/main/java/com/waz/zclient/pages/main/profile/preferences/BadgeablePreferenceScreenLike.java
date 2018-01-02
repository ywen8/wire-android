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
package com.waz.zclient.pages.main.profile.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.waz.zclient.R;
import com.waz.zclient.ui.views.FilledCircularBackgroundDrawable;
import net.xpece.android.support.preference.Preference;

public class BadgeablePreferenceScreenLike extends Preference {

    private int badgeCount;
    private int accentColor;

    public BadgeablePreferenceScreenLike(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public BadgeablePreferenceScreenLike(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference_Material_BadgeablePreferenceScreenLike);
    }

    public BadgeablePreferenceScreenLike(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.badgeablePreferenceScreenLikeStyle);
    }

    public BadgeablePreferenceScreenLike(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BadgeablePreferenceScreenLike, defStyleAttr, defStyleRes);
        badgeCount = a.getInt(R.styleable.BadgeablePreferenceScreenLike_badgeCount, 0);
        a.recycle();
    }

    public void setBadgeCount(int badgeCount) {
        this.badgeCount = badgeCount;
        notifyChanged();
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
        notifyChanged();
    }

    @Override
    protected void onClick() {
        getPreferenceManager().getOnNavigateToScreenListener().onNavigateToScreen(getPreferenceScreen());
    }

    @Override
    @SuppressLint("com.waz.ViewUtils")
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textView = (TextView) holder.findViewById(R.id.tv__pref_devices_badge_count);
        FrameLayout iconFrameLayout = (FrameLayout) holder.findViewById(R.id.icon_frame);

        LinearLayout.LayoutParams layoutParams;
        if (getIcon() != null) {
            layoutParams = (LinearLayout.LayoutParams) iconFrameLayout.getLayoutParams();
            layoutParams.setMarginStart(getContext().getResources().getDimensionPixelSize(R.dimen.wire__padding__big) - holder.itemView.getPaddingLeft());
            iconFrameLayout.setLayoutParams(layoutParams);
        }

        if (badgeCount == 0) {
            textView.setVisibility(View.GONE);
        } else {
            layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
            layoutParams.setMarginEnd(getContext().getResources().getDimensionPixelSize(R.dimen.wire__padding__big) - holder.itemView.getPaddingRight());
            textView.setLayoutParams(layoutParams);

            textView.setVisibility(View.VISIBLE);
            textView.setBackground(new FilledCircularBackgroundDrawable(accentColor));
            textView.setText(String.valueOf(badgeCount));
        }
    }

    public PreferenceScreen getPreferenceScreen() {
        PreferenceScreen preferenceScreen = new PreferenceScreen(getContext(), null);
        preferenceScreen.getExtras().putAll(getExtras());
        preferenceScreen.setTitle(getTitle());
        preferenceScreen.setSummary(getSummary());
        preferenceScreen.setIcon(getIcon());
        preferenceScreen.setKey(getKey());
        return preferenceScreen;
    }
}
