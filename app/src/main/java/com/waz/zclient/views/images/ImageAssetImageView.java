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
package com.waz.zclient.views.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.waz.api.BitmapCallback;
import com.waz.api.ImageAsset;
import com.waz.api.LoadHandle;
import com.waz.api.UpdateListener;
import com.waz.zclient.R;
import com.waz.zclient.ui.animation.interpolators.penner.Expo;
import com.waz.zclient.ui.animation.interpolators.penner.Quart;
import com.waz.zclient.utils.ViewUtils;

public class ImageAssetImageView extends FrameLayout implements UpdateListener {
    public static final String TAG = ImageAssetImageView.class.getName();


    // swap position of the an image view
    private static final int SWAP_POSITION_ONE = 0;
    private static final int SWAP_POSITION_TWO = 1;
    private static final int SWAP_POSITION_NONE = 2;

    // default values
    private static final DisplayType DEFAULT_DISPLAY_TYPE = DisplayType.REGULAR;
    private static final TransitionType DEFAULT_TRANSITION_TYPE = TransitionType.FADE_EXPO_IN_OUT;
    private static final int DEFAULT_TRANSITION_DURATION = 350;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_BORDER_WIDTH = 0;
    private static final float DEFAULT_FROM_SCALE = 0.88f;


    /**
     * The image asset this view works upon.
     */
    private ImageAsset imageAsset;

    /**
     * The desired transition type when a new IamgeAsset is connected
     */
    private TransitionType transitionType;

    /**
     * The default transition type when a new image asset is connected
     */
    private TransitionType defaultTransitionType;

    /**
     * Blur radius for blurred images
     */
    private float blurRadius;

    /**
     * The display type refers to the getBitmap function in the image asset
     */
    private DisplayType displayType;

    /**
     * The handle to bitmap loading processes.
     * A started request can be canceled via the handle.
     */
    private LoadHandle loadHandle;

    /**
     * The duration of a crossfade transition;
     */
    private int crossfadeDuration;

    /**
     * Bearer of the bitmap. Swap Position 1.
     */
    private ImageView swapImageView1;

    /**
     * Bearer of the bitmap. Swap Position 2.
     */
    private ImageView swapImageView2;

    /**
     * The current swap position changes when a new image asset is loaded.
     */
    private int swapPosition;

    /**
     * This is the saturation value animatable.
     */
    private float saturation;

    // border of round bitmap
    private int borderColor;
    private int borderWidth;

    private float fromScale;

    /**
     * The transition type when a new image asset is loaded
     */
    public enum TransitionType {
        NONE,
        FADE,
        FADE_EXPO_IN_OUT,
        SCALE
    }

    /**
     * The display type
     */
    public enum DisplayType {
        REGULAR,
        CIRCLE
    }

    public enum BitmapError {
        IMAGE_ASSET_IS_NULL,
        IMAGE_ASSET_IS_EMPTY,
        BITMAP_LOADING_FAILED
    }


    /**
     * Default CTOR - no attributes are conveyed
     *
     * @param context Android's context
     */
    public ImageAssetImageView(Context context) {
        this(context, null);
    }

    /**
     * CTOR - attributes are given but no style
     *
     * @param context Android's context
     * @param attrs   A set of attributes specified in XML
     */
    public ImageAssetImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * CTOR - attributes and style are given
     *
     * @param context     Android's context
     * @param attrs       A set of attributes specified in XML
     * @param defStyleRes the style id
     */
    public ImageAssetImageView(Context context,
                               AttributeSet attrs,
                               int defStyleRes) {
        super(context, attrs, defStyleRes);
        init();
    }

    private void init() {
        displayType = DEFAULT_DISPLAY_TYPE;
        defaultTransitionType = DEFAULT_TRANSITION_TYPE;
        blurRadius = ViewUtils.toPx(getContext(), getResources().getInteger(R.integer.background__blur_radius));
        crossfadeDuration = DEFAULT_TRANSITION_DURATION;
        borderColor = DEFAULT_COLOR;
        borderWidth = ViewUtils.toPx(getContext(), DEFAULT_BORDER_WIDTH);
        fromScale = DEFAULT_FROM_SCALE;


        // TODO set the passed attributes from the xml layout file.

        swapImageView1 = new ImageView(getContext());
        swapImageView1.setScaleType(ImageView.ScaleType.CENTER_CROP);
        swapImageView2 = new ImageView(getContext());
        swapImageView2.setScaleType(ImageView.ScaleType.CENTER_CROP);

        addView(swapImageView1);
        addView(swapImageView2);
    }

    public ImageView.ScaleType getScaleType() {
        return swapImageView1.getScaleType();
    }

    /**
     * Supports an animatable gray scaling with an object animator.
     *
     * @param saturation
     */
    public void setSaturation(float saturation) {
        this.saturation = saturation;
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(saturation); //0 means grayscale
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
        swapImageView1.setColorFilter(cf);
        swapImageView2.setColorFilter(cf);
    }

    /**
     * Object Animator also demands a getter method
     *
     * @return
     */
    public float getSaturation() {
        return saturation;
    }

    /**
     * Changes the display type and if the change is relevant and an image asset exists
     * it updates the bitmap.
     *
     * @param displayType
     */
    public void setDisplayType(DisplayType displayType) {
        if (this.displayType != displayType) {
            this.displayType = displayType;
            updated();
        }
    }

    /**
     * connects a new image asset by using the default transition type
     *
     * @param imageAsset the image asset to be connected
     */
    public void connectImageAsset(ImageAsset imageAsset) {
        connectImageAsset(imageAsset, defaultTransitionType);
    }

    /**
     * Connects an image asset taking into account if an old image asset exists.
     *
     * @param imageAsset     the new image asset
     * @param transitionType if an old image asset exist the transition that should be used
     */
    public void connectImageAsset(ImageAsset imageAsset, TransitionType transitionType) {
        connectImageAsset(imageAsset, transitionType, false);
    }

    public void connectImageAsset(ImageAsset imageAsset, TransitionType transitionType, boolean forceBlur) {
        // new image asset is null
        if (imageAsset == null) {
            // disconnect former image asset and set image drawable to null
            disconnectImageAsset();
            updated();
            return;
        }

        // old image asset exists
        if (this.imageAsset != null) {
            if (this.imageAsset.getId().equals(imageAsset.getId())) {
                updated();
                return;
            } else {
                // otherwise disconnect
                disconnectImageAsset();
            }
        }

        // connect image asset
        this.imageAsset = imageAsset;
        this.imageAsset.addUpdateListener(this);
        this.transitionType = transitionType;

        updated();
    }

    /**
     * Disconnect image asset currently connected to this view.
     */
    private void disconnectImageAsset() {
        if (imageAsset != null) {
            imageAsset.removeUpdateListener(this);
            imageAsset = null;
        }

        if (loadHandle != null) {
            loadHandle.cancel();
            loadHandle = null;
        }
    }

    /**
     * Creates a drawable form a raw bitmap and sets it as
     * as the source of this image view.
     *
     * @param bitmap the source of
     */
    private void setBitmapWithTransition(Bitmap bitmap) {
        Drawable newDrawable = new BitmapDrawable(getContext().getResources(), bitmap);

        // no transition demanded - put image into visible view
        if (transitionType == TransitionType.NONE) {
            switch (swapPosition) {
                case SWAP_POSITION_ONE:
                    swapImageView1.setImageDrawable(newDrawable);
                    break;
                case SWAP_POSITION_TWO:
                    swapImageView2.setImageDrawable(newDrawable);
                    break;
                case SWAP_POSITION_NONE:
                    swapPosition = SWAP_POSITION_ONE;
                    swapImageView1.setImageDrawable(newDrawable);
                    swapImageView1.animate().alpha(1).setDuration(crossfadeDuration);
                    break;
            }
            return;
        }

        ImageView imageViewIn;
        ImageView imageViewOut;

        switch (swapPosition) {
            case SWAP_POSITION_NONE:
            case SWAP_POSITION_ONE:
                imageViewIn = swapImageView2;
                imageViewOut = swapImageView1;
                swapPosition = SWAP_POSITION_TWO;
                break;
            case SWAP_POSITION_TWO:
                imageViewIn = swapImageView1;
                imageViewOut = swapImageView2;
                swapPosition = SWAP_POSITION_ONE;
                break;
            default:
                return;
        }

        imageViewIn.setImageDrawable(newDrawable);

        // the transition always works with the same views:  in and out
        // If you need a custom transition, define one in TransitionType
        // and tell the views here how to behave during the transition.
        switch (transitionType) {
            case SCALE:
                imageViewIn.setScaleX(fromScale);
                imageViewIn.setScaleY(fromScale);
                imageViewIn.setAlpha(1.0f);
                imageViewIn.animate()
                           .scaleX(1.0f)
                           .scaleY(1.0f)
                           .setInterpolator(new Quart.EaseOut())
                           .setDuration(crossfadeDuration);

                imageViewOut.animate()
                            .alpha(0)
                            .setDuration(crossfadeDuration);
                break;
            case FADE:
                imageViewIn.animate()
                           .alpha(1)
                           .setDuration(crossfadeDuration);
                imageViewOut.animate()
                            .alpha(0)
                            .setDuration(crossfadeDuration);
                break;
            case FADE_EXPO_IN_OUT:
                imageViewIn.animate()
                           .alpha(1)
                           .setInterpolator(new Expo.EaseInOut())
                           .setDuration(550)
                           .setDuration(550);
                imageViewOut.animate()
                            .alpha(0)
                            .setInterpolator(new Expo.EaseInOut())
                            .setDuration(550);
                break;
        }
    }

    /**
     * Image asset callback. When this image asset gets updated this will be called.
     */
    @Override
    public void updated() {
        if (loadHandle != null) {
            loadHandle.cancel();
        }

        // check if image asset is null
        if (imageAsset == null) {
            return;
        }

        // check if image asset is empty
        if (imageAsset.isEmpty()) {
            return;
        }

        final int measuredWidth = getMeasuredWidth();
        if (measuredWidth == 0) {
            return;
        }

        if (displayType == null) {
            return;
        }

        switch (displayType) {
            case REGULAR:
                loadHandle = imageAsset.getBitmap(measuredWidth, callback);
                break;
            case CIRCLE:
                loadHandle = imageAsset.getRoundBitmap(measuredWidth, borderWidth, borderColor, callback);
                break;
        }
    }

    private BitmapCallback callback = new BitmapCallback() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap) {
            setBitmapWithTransition(bitmap);
            // once the bitmap loaded any further updates on this image assets
            // comes with no animation.
            transitionType = TransitionType.NONE;
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updated();
    }

    /**
     * Removes current bitmaps from background.
     */
    public void resetBackground() {
        swapPosition = SWAP_POSITION_NONE;
        if (swapImageView1 == null ||
            swapImageView2 == null) {
            return;
        }
        swapImageView1.animate()
                      .alpha(0)
                      .setDuration(crossfadeDuration)
                      .withEndAction(new Runnable() {
                          @Override
                          public void run() {
                              if (swapImageView1 != null) {
                                  swapImageView1.setImageDrawable(null);
                              }
                          }
                      });
        swapImageView2.animate()
                      .alpha(0)
                      .setDuration(crossfadeDuration)
                      .withEndAction(new Runnable() {
                          @Override
                          public void run() {
                              if (swapImageView2 != null) {
                                  swapImageView2.setImageDrawable(null);
                              }
                          }
                      });
    }

    public void reset() {
        swapPosition = SWAP_POSITION_NONE;
        if (swapImageView1 == null ||
            swapImageView2 == null) {
            return;
        }
        swapImageView1.setImageDrawable(null);
        swapImageView2.setImageDrawable(null);
    }
}
