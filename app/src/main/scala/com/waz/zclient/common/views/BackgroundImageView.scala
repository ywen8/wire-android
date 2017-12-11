/**
 * Wire
 * Copyright (C) 2017 Wire Swiss GmbH
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
package com.waz.zclient.common.views

import android.content.Context
import android.graphics._
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.ImageView
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.zclient.ViewHelper
import com.waz.zclient.common.views.BackgroundDrawable.PictureInfo
import com.waz.zclient.common.views.ImageController.WireImage
import com.waz.zclient.ui.utils.ColorUtils

class BackgroundImageView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends ImageView(context, attrs, defStyleAttr) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  private val zms = inject[Signal[ZMessaging]]
  private val blackLevel = 0.58f

  private val pictureInfo = for {
    z <- zms
    Some(picture) <- z.usersStorage.signal(z.selfUserId).map(_.picture)
    assetData <- z.assetsStorage.signal(picture)
  } yield PictureInfo(WireImage(picture), assetData.dimensions)

  setBackground(new BackgroundDrawable(pictureInfo, getContext))
  setImageDrawable(new ColorDrawable(ColorUtils.injectAlpha(blackLevel, Color.BLACK)))
}
