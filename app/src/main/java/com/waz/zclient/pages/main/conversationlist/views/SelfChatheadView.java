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
package com.waz.zclient.pages.main.conversationlist.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.waz.api.CoreList;
import com.waz.api.OtrClient;
import com.waz.api.Self;
import com.waz.api.UpdateListener;
import com.waz.zclient.ui.views.CircleView;
import com.waz.zclient.views.images.ImageAssetImageView;


public class SelfChatheadView extends FrameLayout implements UpdateListener {
    public static final String TAG = SelfChatheadView.class.getName();

    private CircleView indicatorDot;
    private Self self;
    private ImageAssetImageView imageAssetImageView;
    private CoreList<OtrClient> otherClients;

    public SelfChatheadView(Context context) {
        this(context, null);
    }

    public SelfChatheadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelfChatheadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalStateException(TAG + " needs 2 children");
        }

        imageAssetImageView = (ImageAssetImageView) getChildAt(0);
        imageAssetImageView.setDisplayType(ImageAssetImageView.DisplayType.CIRCLE);
        indicatorDot = (CircleView) getChildAt(1);
    }

    public void setSelf(Self self) {
        this.self = self;
        this.self.addUpdateListener(this);
        otherClients = self.getIncomingOtrClients();
        otherClients.addUpdateListener(this);
        updated();
    }

    public void tearDown() {
        if (this.self != null) {
            self.removeUpdateListener(this);
            otherClients.removeUpdateListener(this);
            self = null;
            otherClients = null;
            imageAssetImageView.reset();
        }
    }

    @Override
    public void updated() {
        imageAssetImageView.connectImageAsset(self.getPicture());

        if (otherClients.size() > 0) {
            indicatorDot.setVisibility(View.VISIBLE);
        } else {
            indicatorDot.setVisibility(View.GONE);
        }
    }

    public void setAccentColor(int color) {
        indicatorDot.setAccentColor(color);
    }
}
