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
package com.waz.zclient.ui.calling;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.waz.zclient.R;
import com.waz.zclient.utils.ViewUtils;

public class CallControlCameraToggleButtonView extends FrameLayout {
    private TextView cameraIcon;
    private int flipAnimationDuration;

    private boolean flipped = false;

    public CallControlCameraToggleButtonView(Context context) {
        this(context, null);
    }

    public CallControlCameraToggleButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CallControlCameraToggleButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setFlipped(boolean flipped) {
        if (flipped == this.flipped) {
            return;
        }
        this.flipped = flipped;
        cameraIcon.animate()
                  .rotationYBy(180f)
                  .rotationXBy(180f)
                  .setDuration(flipAnimationDuration)
                  .start();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.calling__controls__camera_toggle_button, this, true);

        cameraIcon =  ViewUtils.getView(this, R.id.gtv__calling_controls__ongoing_camera);
        cameraIcon.setTextColor(ContextCompat.getColorStateList(getContext(),
                                                                R.color.selector__icon_button__text_color__dark));

        flipAnimationDuration = getResources().getInteger(R.integer.animation_duration_medium);
    }
}
