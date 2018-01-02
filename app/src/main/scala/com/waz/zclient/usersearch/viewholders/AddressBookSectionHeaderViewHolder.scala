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
package com.waz.zclient.usersearch.viewholders

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.waz.zclient.R
import com.waz.zclient.utils.ViewUtils

class AddressBookSectionHeaderViewHolder(val view: View, val darkTheme: Boolean) extends RecyclerView.ViewHolder(view) {

  private val label: TextView = ViewUtils.getView(view, R.id.ttv_startui_section_header)

  if (darkTheme) label.setTextColor(ContextCompat.getColor(view.getContext, R.color.text__primary_dark))

  def bind(labelText: String): Unit = {
    label.setText(labelText)
  }
}
