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

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ComposeShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateInterpolator;

import java.util.Arrays;

public class BackgroundDrawable extends Drawable {

    private static final int DEFAULT_BACKGROUND_COLOR = Color.BLACK;

    private static final int COLOR_ANIMATION_DURATION = 600;
    private static final int OVERLAY_ANIMATION_DURATION = 300;

    private static final float MIN_COLOR_OPACITY = 0f;
    private static final float RESTING_COLOR_OPACITY = 0.11f;
    private static final float MAX_COLOR_OPACITY = 0.30f;

    private static final float MIN_DARKNESS_OPACITY = 0f;
    private static final float MAX_DARKNESS_OPACITY = 0.4f;

    private static final float MIN_RADIUS_FACTOR = 0.72f;
    private static final float MAX_RADIUS_FACTOR = 10f; //cause vignette to disappear

    private static final int VIGNETTE_COLOR_CENTER = Color.TRANSPARENT;
    private static final int VIGNETTE_COLOR_EDGE = Color.BLACK;

    private Bitmap bitmap;

    private final Paint paint;
    private final ColorMatrix colorMatrix;

    private final float width;
    private final float height;

    private Shader bitmapShader;
    private Shader vignetteShader;

    private final ValueAnimator overlayAnimation;
    private final ValueAnimator colorAnimation;

    private int accentColor;
    private float accentColorOpacity;

    private float darknessOpacity;
    private float vignetteRadiusFactor;

    private boolean isOverlayVisible;

    public BackgroundDrawable(Rect bounds) {
        width = (float) bounds.width();
        height = (float) bounds.height();

        paint = new Paint();
        colorMatrix = new ColorMatrix();

        colorAnimation = ValueAnimator.ofFloat(0f, 1f);
        overlayAnimation = ValueAnimator.ofFloat(0f, 1f);

        setAccentColor(Color.TRANSPARENT, false);
        setOverlayVisible(true, false);

        setupAccentColorAnimator();
        setupOverlayAnimation();
        fullUpdate();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        fullUpdate();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), paint);
    }

    public void setAccentColor(int color, boolean animate) {
        this.accentColor = color;

        if (animate && isOverlayVisible) {
            colorAnimation.start();
        } else if (!animate && isOverlayVisible) {
            accentColorOpacity = RESTING_COLOR_OPACITY;
        } else {
            accentColorOpacity = MIN_COLOR_OPACITY;
        }
    }

    public void setOverlayVisible(boolean visible, boolean animate) {
        isOverlayVisible = visible;
        if (animate) {
            overlayAnimation.start();
        } else {
            darknessOpacity = visible ? MAX_DARKNESS_OPACITY : MIN_DARKNESS_OPACITY;
            vignetteRadiusFactor = visible ? MIN_RADIUS_FACTOR : MAX_RADIUS_FACTOR;
        }
    }

    public int getWidth() {
        return (int) width;
    }

    private void createBackgroundShader() {
        if (bitmap == null) {
            bitmap = createBitmap((int) width, (int) height);
            if (bitmap == null) {
                return;
            }
        }

        float imageWidth = (float) bitmap.getWidth();
        float imageHeight = (float) bitmap.getHeight();

        bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        float scale = Math.max(width / imageWidth, height / imageHeight);
        matrix.setScale(scale, scale);
        matrix.postTranslate(-(imageWidth * scale - width) / 2f, -(imageHeight * scale - height) / 2f);
        bitmapShader.setLocalMatrix(matrix);
    }

    private Bitmap createBitmap(int width, int height) {
        Bitmap ret;
        try {
            int[] colors = new int[width * height];
            Arrays.fill(colors, DEFAULT_BACKGROUND_COLOR);
            ret = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
            return ret;
        } catch (OutOfMemoryError e) {
            return createBitmap(width / 2, height / 2);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void createVignetteShader() {
        vignetteShader = new RadialGradient(
            0.5f,
            0.5f,
            vignetteRadiusFactor,
            VIGNETTE_COLOR_CENTER,
            VIGNETTE_COLOR_EDGE,
            Shader.TileMode.CLAMP);

        Matrix matrix = new Matrix();
        matrix.setScale(width, height);
        vignetteShader.setLocalMatrix(matrix);

    }

    public void fullUpdate() {
        if (width <= 0 || height <= 0) {
            return;
        }
        createBackgroundShader();
        updateFilter();
    }

    /**
     * Separate from fullUpdate() to allow for animations without recreating the underlying bitmap.
     */
    private void updateFilter() {
        if (bitmapShader == null) {
            return;
        }
        //New vignette shader must be created here for the animation as the radius changes
        createVignetteShader();
        paint.setShader(new ComposeShader(bitmapShader, vignetteShader, PorterDuff.Mode.DARKEN));

        setColorMatrix();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

        invalidateSelf();
    }

    private void setColorMatrix() {
        colorMatrix.reset();
        float[] vals = colorMatrix.getArray();
        float srcR = ((float) Color.red(accentColor)) / 255;
        float srcG = ((float) Color.green(accentColor)) / 255;
        float srcB = ((float) Color.blue(accentColor)) / 255;

        vals[3] = accentColorOpacity * srcR;
        vals[8] = accentColorOpacity * srcG;
        vals[13] = accentColorOpacity * srcB;
        vals[18] = 1 - darknessOpacity;
    }

    private void setupAccentColorAnimator() {
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                accentColorOpacity = MAX_COLOR_OPACITY - (MAX_COLOR_OPACITY - RESTING_COLOR_OPACITY) * animation.getAnimatedFraction();
                updateFilter();
            }
        });
        colorAnimation.setInterpolator(new AccentColorInterpolator());
        colorAnimation.setDuration(COLOR_ANIMATION_DURATION);
    }

    private void setupOverlayAnimation() {
        overlayAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();

                float darknessRange = MAX_DARKNESS_OPACITY - MIN_DARKNESS_OPACITY;
                float radiusRage = MAX_RADIUS_FACTOR - MIN_RADIUS_FACTOR;
                float colorOpacityRange = RESTING_COLOR_OPACITY - MIN_COLOR_OPACITY;

                if (isOverlayVisible) { //animating on
                    darknessOpacity = MIN_DARKNESS_OPACITY + darknessRange * fraction;
                    vignetteRadiusFactor = MAX_RADIUS_FACTOR - radiusRage * fraction;
                    accentColorOpacity = MIN_COLOR_OPACITY + colorOpacityRange * fraction;
                } else { //animating off
                    darknessOpacity = MAX_DARKNESS_OPACITY - darknessRange * fraction;
                    vignetteRadiusFactor = MIN_RADIUS_FACTOR + radiusRage * fraction;
                    accentColorOpacity = RESTING_COLOR_OPACITY - colorOpacityRange * fraction;
                }
                updateFilter();
            }
        });
        overlayAnimation.setInterpolator(new AccelerateInterpolator());
        overlayAnimation.setDuration(OVERLAY_ANIMATION_DURATION);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
