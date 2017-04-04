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
package com.waz.zclient.views.chathead;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.waz.zclient.R;

public class ConversationChatheadDrawable extends Drawable {

    private final int smallBorderWidth;
    private final int largeBorderWidth;
    private final int minSizeForLargeBorderWidth;
    private Paint paint;
    private Paint clearPaint;
    private int[] accentColors;
    int borderWidth;

    public ConversationChatheadDrawable(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        clearPaint = new Paint();
        clearPaint.setColor(Color.TRANSPARENT);
        clearPaint.setAntiAlias(true);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        accentColors = context.getResources().getIntArray(R.array.selectable_accents_color);

        minSizeForLargeBorderWidth = (int) context.getResources().getDimension(R.dimen.chathead__min_size_large_border);
        smallBorderWidth = (int) context.getResources().getDimension(R.dimen.chathead__border_width);
        largeBorderWidth = (int) context.getResources().getDimension(R.dimen.chathead__large_border_width);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds.width() >= minSizeForLargeBorderWidth) {
            borderWidth = largeBorderWidth;
        } else {
            borderWidth = smallBorderWidth;
        }
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        int height = getBounds().height();
        int width = getBounds().width();
        int radius = Math.min(height, width) / 2 - borderWidth;

        RectF rectF = new RectF(0, 0, width, height);

        // Draw circle sections in different colors
        float startAngle = -90f;
        float sweepAngle = (360f / accentColors.length) + 1;
        for (int color : accentColors) {
            paint.setColor(color);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }

        // Cut out transparent center
        canvas.drawCircle(width / 2, height / 2, radius, clearPaint);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
