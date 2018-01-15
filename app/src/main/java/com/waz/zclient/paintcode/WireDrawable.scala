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
package com.waz.zclient.paintcode

import android.graphics.drawable.Drawable
import android.graphics.{ColorFilter, Paint}

trait WireDrawable extends Drawable {

  protected val paint = new Paint()

  override def setColorFilter(colorFilter: ColorFilter): Unit = paint.setColorFilter(colorFilter)

  override def getOpacity: Int = paint.getAlpha

  override def setAlpha(alpha: Int): Unit = paint.setAlpha(alpha)

  def setColor(color: Int): Unit = paint.setColor(color)
}
