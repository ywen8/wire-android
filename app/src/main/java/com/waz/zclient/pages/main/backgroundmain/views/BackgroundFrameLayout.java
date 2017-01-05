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
package com.waz.zclient.pages.main.backgroundmain.views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.waz.api.BitmapCallback;
import com.waz.api.ImageAsset;
import com.waz.api.LoadHandle;
import com.waz.zclient.R;
import com.waz.zclient.controllers.accentcolor.AccentColorObserver;
import com.waz.zclient.controllers.background.BackgroundObserver;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.utils.LayoutSpec;
import com.waz.zclient.utils.ViewUtils;

public class BackgroundFrameLayout extends FrameLayout implements BackgroundObserver,
                                                                  AccentColorObserver {

    private final boolean isTablet;
    private boolean scaledToMax;

    private BackgroundDrawable drawable;
    private LoadHandle loadHandle;

    private int width;
    private int height;

    private ModelObserver<ImageAsset> imageAssetObserver = new ModelObserver<ImageAsset>() {
        @Override
        public void updated(ImageAsset imageAsset) {
            if (loadHandle != null) {
                loadHandle.cancel();
            }
            loadHandle = imageAsset.getBitmap(drawable.getWidth(), new BitmapCallback() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap) {
                    drawable.setBitmap(bitmap);
                }
            });
        }
    };

    public BackgroundFrameLayout(Context context) {
        this(context, null);
    }

    public BackgroundFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BackgroundFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        isTablet = LayoutSpec.isTablet(context);
        if (isTablet) {
            resizeIfNeeded(getResources().getConfiguration());
        } else {
            setDrawable(ViewUtils.getOrientationDependentDisplayBounds(getContext()));
        }
    }

    private void setDrawable(Rect bounds) {
        drawable = new BackgroundDrawable(bounds);
        setBackground(drawable);
    }

    @Override
    public void onLoadImageAsset(ImageAsset imageAsset) {
        imageAssetObserver.setAndUpdate(imageAsset);
    }

    @Override
    public void onScaleToMax(boolean max) {
        scaledToMax = max;
        resizeIfNeeded(getResources().getConfiguration());
    }

    @Override
    public void onAccentColorHasChanged(Object sender, int color) {
        drawable.setAccentColor(color, true);
    }

    private void resizeIfNeeded(Configuration configuration) {
        if (!isTablet) {
            return;
        }
        final int width = scaledToMax ?
                          ViewUtils.toPx(getContext(), configuration.screenWidthDp) :
                          getResources().getDimensionPixelSize(R.dimen.framework__sidebar_width);
        final int height = ViewUtils.toPx(getContext(), configuration.screenHeightDp);

        if (this.width != width || this.height != height) {
            resize(width, height);
        }
    }

    private void resize(int width, int height) {
        this.width = width;
        this.height = height;
        setDrawable(new Rect(0, 0, width, height));
        imageAssetObserver.forceUpdate();
    }

    public boolean isExpanded() {
        return scaledToMax;
    }
}
