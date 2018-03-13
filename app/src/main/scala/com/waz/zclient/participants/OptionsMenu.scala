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
package com.waz.zclient.participants

import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.view.ViewGroup
import android.widget.{LinearLayout, TextView}
import com.waz.model.ConvId
import com.waz.utils.returning
import com.waz.zclient.ui.animation.interpolators.penner.{Expo, Quart}
import com.waz.zclient.ui.optionsmenu.OptionsMenuItem
import com.waz.zclient.ui.text.GlyphTextView
import com.waz.zclient.utils.ViewUtils.getView
import com.waz.zclient.utils.{RichView, ViewUtils}
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{DialogHelper, R}

case class OptionsMenu(context: Context, inConvList: Boolean, convId: ConvId) extends BottomSheetDialog(context, R.style.message__bottom_sheet__base) with DialogHelper {
  private implicit val ctx = context

  val ctrl = OptionsMenuController(convId, inConvList)

  private val data = ctrl.optionItems.disableAutowiring()

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val view = getLayoutInflater.inflate(R.layout.message__bottom__menu, null).asInstanceOf[LinearLayout]
    setContentView(view)

    def params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.toPx(getContext, 48))

    data.onUi { items =>

      view.removeAllViews()

      items.foreach { item =>
        view.addView(returning(getLayoutInflater.inflate(R.layout.message__bottom__menu__row, view, false)) { itemView =>

          val isRed = Set(OptionsMenuItem.DELETE, OptionsMenuItem.LEAVE).contains(item)

          returning(getView[GlyphTextView](itemView, R.id.icon)) { v =>
            v.setText(item.resGlyphId)
            if (isRed) v.setTextColor(getColor(R.color.accent_red))
          }
          returning(getView[TextView](itemView, R.id.text)) { v =>
            v.setText(item.resTextId)
            if (isRed) v.setTextColor(getColor(R.color.accent_red))
          }
          itemView.onClick {
            ctrl.onMenuItemClicked ! item
            dismiss()
          }
        }, params)
      }
    }
  }
}

object OptionsMenu {

  lazy val quartOut = new Quart.EaseOut
  lazy val expoOut  = new Expo.EaseOut
  lazy val expoIn   = new Expo.EaseIn

  trait AnimState
  case object Open    extends AnimState
  case object Opening extends AnimState
  case object Closing extends AnimState
  case object Closed  extends AnimState
}
