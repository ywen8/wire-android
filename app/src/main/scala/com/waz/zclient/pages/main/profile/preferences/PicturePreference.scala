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
  *//**
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
package com.waz.zclient.pages.main.profile.preferences

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.waz.api.Self
import com.waz.model.AssetId
import com.waz.utils.events.Signal
import com.waz.zclient.core.api.scala.ModelObserver
import com.waz.zclient.ui.utils.DrawableUtils
import com.waz.zclient.ui.views.FilledCircularBackgroundDrawable
import com.waz.zclient.views.ImageAssetDrawable
import com.waz.zclient.views.ImageController.WireImage
import com.waz.zclient.{R, WireApplication}
import net.xpece.android.support.preference.Preference

class PicturePreference(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) extends Preference(context, attrs, defStyleAttr, defStyleRes) {
  def this(context: Context, attrs: AttributeSet, defStyleAttr: Int) = this(context, attrs, defStyleAttr, R.style.Preference_Material)
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, R.attr.preferenceStyle)
  def this(context: Context) = this(context, null)

  implicit lazy val inj = context.getApplicationContext.asInstanceOf[WireApplication].injector
  implicit lazy val ctx = context.getApplicationContext.asInstanceOf[WireApplication].eventContext

  private val diameter: Int = getContext.getResources.getDimensionPixelSize(R.dimen.pref_account_icon_size)
  private val icon: FilledCircularBackgroundDrawable = new FilledCircularBackgroundDrawable(Color.TRANSPARENT, diameter)
  setIcon(DrawableUtils.drawableToBitmapDrawable(getContext.getResources, icon, diameter))

  final private val selfModelObserver: ModelObserver[Self] = new ModelObserver[Self]() {
    override def updated(model: Self): Unit = {

      val wireImage = WireImage(AssetId(model.getPicture.getId))
      val drawable: ImageAssetDrawable = new ImageAssetDrawable(Signal(wireImage), ImageAssetDrawable.ScaleType.CenterInside, ImageAssetDrawable.RequestBuilder.Round, Some(icon))
      drawable.setBounds(getIcon.getBounds)
      setIcon(drawable)
    }
  }

  def setSelfUser(self: Self): Unit = {
    if (self == null) {
      selfModelObserver.pauseListening()
    }
    else {
      selfModelObserver.setAndUpdate(self)
    }
  }
}
