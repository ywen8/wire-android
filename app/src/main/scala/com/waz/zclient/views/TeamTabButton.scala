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
package com.waz.zclient.views

import android.content.Context
import android.graphics.Color
import android.util.{AttributeSet, TypedValue}
import android.view.ViewGroup.MarginLayoutParams
import android.view.{View, ViewGroup}
import android.widget.FrameLayout.LayoutParams
import android.widget.{FrameLayout, ImageView}
import com.waz.api.impl.{AccentColor, AccentColors}
import com.waz.model._
import com.waz.threading.Threading
import com.waz.utils.NameParts
import com.waz.zclient.controllers.global.AccentColorController
import com.waz.zclient.drawables.TeamIconDrawable
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.views.CircleView
import com.waz.zclient.{R, ViewHelper}

class TeamTabButton(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends FrameLayout(context, attrs, defStyleAttr) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  inflate(R.layout.view_team_tab)
  setLayoutParams(new LayoutParams(context.getResources.getDimensionPixelSize(R.dimen.teams_tab_width), ViewGroup.LayoutParams.MATCH_PARENT))

  val icon = findById[ImageView](R.id.team_icon)
  val name = findById[TypefaceTextView](R.id.team_name)
  val unreadIndicator = findById[CircleView](R.id.unread_indicator)
  val animationDuration = getResources.getInteger(R.integer.team_tabs__animation_duration)
  val accentColor = inject[AccentColorController].accentColor

  val drawable = new TeamIconDrawable
  icon.setImageDrawable(drawable)
  setLayerType(View.LAYER_TYPE_SOFTWARE, null)
  val params = name.getLayoutParams.asInstanceOf[MarginLayoutParams]
  val tv = new TypedValue
  val height =
    if (getContext.getTheme.resolveAttribute(android.R.attr.actionBarSize, tv, true))
      TypedValue.complexToDimensionPixelSize(tv.data,getResources.getDisplayMetrics)
    else
      getResources.getDimensionPixelSize(R.dimen.teams_tab_default_height)
  params.topMargin = height / 2 - getResources.getDimensionPixelSize(R.dimen.teams_tab_bottom_offset)
  private var selectedColor = AccentColors.defaultColor.getColor()
  private var buttonSelected = false

  accentColor.on(Threading.Ui){ accentColor =>
    selectedColor = accentColor.getColor()
  }

  def setTeamData(teamData: TeamData, selected: Boolean): Unit = {
    val color = if (selected) selectedColor else Color.TRANSPARENT
    drawable.setInfo(NameParts.maybeInitial(teamData.name).getOrElse(""), color, TeamIconDrawable.TeamCorners)
    name.setText(teamData.name)
    unreadIndicator.setAccentColor(AccentColors.defaultColor.getColor())
    drawable.assetId ! None
    buttonSelected = selected
  }

  def setUserData(userData: UserData, selected: Boolean): Unit = {
    val color = if (selected) AccentColor(userData.accent).getColor() else Color.TRANSPARENT
    drawable.setInfo(NameParts.maybeInitial(userData.displayName).getOrElse(""), color, TeamIconDrawable.UserCorners)
    name.setText(userData.displayName)
    unreadIndicator.setAccentColor(AccentColor(userData.accent).getColor())
    drawable.assetId ! userData.picture
    buttonSelected = selected
  }

  def animateExpand(): Unit = {
    name.animate().translationY(0f).setDuration(animationDuration).start()
    icon.animate().translationY(0f).setDuration(animationDuration).alpha(1f).start()
    name.setTextColor(Color.WHITE)
  }

  def animateCollapse(): Unit = {
    val margin = height / 2 - getResources.getDimensionPixelSize(R.dimen.teams_tab_bottom_offset_half)
    name.animate().translationY(-margin).setDuration(animationDuration).start()
    icon.animate().translationY(-margin).alpha(0f).setDuration(animationDuration).start()
    val textColor = if (buttonSelected) selectedColor else Color.WHITE
    name.setTextColor(textColor)
  }
}
