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
package com.waz.zclient.usersearch.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.waz.model.IntegrationData
import com.waz.zclient.common.views.ChatheadView
import com.waz.zclient.{R, ViewHelper}

class SearchResultIntegrationRowView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends FrameLayout(context, attrs, defStyleAttr) with IntegrationRowView with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  inflate(R.layout.list_row_pickuser_searchuser, this, addToParent = true)

  private val chathead = findById[ChatheadView](R.id.cv_pickuser__searchuser_chathead)
  private val contactListItemTextView = findById[ContactListItemTextView](R.id.clitv__contactlist__user__text_view)

  private var integrationData = Option.empty[IntegrationData]

  def setIntegration(data: IntegrationData): Unit = {
    this.integrationData = Some(data)
    contactListItemTextView.setIntegration(data)
    chathead.setIntegration(data)
  }

  def getIntegration = integrationData.orNull

  def applyDarkTheme(): Unit = {
    contactListItemTextView.applyDarkTheme()
  }

}
