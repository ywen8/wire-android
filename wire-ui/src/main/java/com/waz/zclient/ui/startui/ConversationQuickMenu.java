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
package com.waz.zclient.ui.startui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.waz.zclient.ui.R;
import com.waz.zclient.ui.views.ZetaButton;
import com.waz.zclient.utils.ViewUtils;

public class ConversationQuickMenu extends LinearLayout {
    ConversationQuickMenuCallback callback;
    private ZetaButton conversationButton;
    private View videoCallButton;
    private View callButton;
    private View cameraButton;

    public ConversationQuickMenu(Context context) {
        this(context, null);
    }

    public ConversationQuickMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationQuickMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(ConversationQuickMenuCallback callback) {
        this.callback = callback;

        conversationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversationQuickMenu.this.callback.onConversationButtonClicked();
            }
        });

        cameraButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversationQuickMenu.this.callback.onCameraButtonClicked();
            }
        });

        callButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversationQuickMenu.this.callback.onCallButtonClicked();
            }
        });

        videoCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ConversationQuickMenu.this.callback.onVideoCallButtonClicked();
            }
        });
    }

    public void setAccentColor(int color) {
        conversationButton.setAccentColor(color);

        ShapeDrawable ovalBackground = new ShapeDrawable(new OvalShape());
        ovalBackground.getPaint().setColor(color);
        ovalBackground.getPaint().setStyle(Paint.Style.FILL);
        ovalBackground.getPaint().setAntiAlias(true);

        cameraButton.setBackground(ovalBackground);
        callButton.setBackground(ovalBackground);
        videoCallButton.setBackground(ovalBackground);
    }

    public void setConversationButtonText(String text) {
        conversationButton.setText(text);
    }

    public void showVideoCallButton(boolean show) {
        if (show) {
            videoCallButton.setVisibility(VISIBLE);
        } else {
            videoCallButton.setVisibility(GONE);
        }
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.quick_menu, this, true);
        conversationButton = ViewUtils.getView(this, R.id.zb__conversation_quick_menu__conversation_button);
        cameraButton =  ViewUtils.getView(this, R.id.gtv__conversation_quick_menu__camera_button);
        callButton =  ViewUtils.getView(this, R.id.gtv__conversation_quick_menu__call_button);
        videoCallButton =  ViewUtils.getView(this, R.id.gtv__conversation_quick_menu__video_call_button);
    }
}
