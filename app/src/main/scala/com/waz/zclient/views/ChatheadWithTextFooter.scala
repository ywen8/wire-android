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
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.waz.ZLog
import com.waz.model.{UserData, UserId}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.common.views.ChatheadView
import com.waz.zclient.ui.text.{TextTransform, TypefaceTextView}
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.views.pickuser.UserRowView
import com.waz.zclient.{R, ViewHelper}

class ChatheadWithTextFooter(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends LinearLayout(context, attrs, defStyleAttr) with ViewHelper with UserRowView {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  implicit val logTag = ZLog.logTagFor[ChatheadWithTextFooter]

  inflate(R.layout.chathead_with_text_footer, this, addToParent = true)

  private val chathead = findById[ChatheadView](R.id.cv__chathead)
  private val footer = findById[TypefaceTextView](R.id.ttv__text_view)
  private val transformer = TextTransform.get(context.getResources.getString(R.string.participants__chathead__name_label__text_transform))
  private val userId = Signal[UserId]()
  private val userData = for{
    z <- inject[Signal[ZMessaging]]
    uId <- userId
    data <- z.usersStorage.signal(uId)
  } yield data

  setOrientation(LinearLayout.VERTICAL)
  initAttributes(attrs)
  userData.on(Threading.Ui) { data =>
    updateView(data)
  }

  override def isSelected: Boolean = chathead.isSelected

  override def setSelected(selected: Boolean): Unit = {
    chathead.setSelected(selected)
  }

  def setUser(user: UserData): Unit = {
    updateView(user)
    userId ! user.id
  }

  def setUserId(userId: UserId): Unit = {
    this.userId ! userId
  }

  private def updateView(user: UserData): Unit = {
    chathead.setUserId(user.id)
    footer.setText(transformer.transform(user.getDisplayName))
  }

  def getUser: Option[UserId] = userData.currentValue.map(_.id)

  def onClicked(): Unit = {
    chathead.setSelected(!chathead.isSelected)
  }

  def setChatheadFooterTextColor(color: Int): Unit = {
    footer.setTextColor(color)
  }

  def setChatheadFooterFont(fontName: String): Unit = {
    footer.setTypeface(fontName)
  }

  override def setOnClickListener(l: View.OnClickListener): Unit = {
    super.setOnClickListener(new View.OnClickListener() {
      def onClick(v: View): Unit = {
        l.onClick(chathead)
      }
    })
  }

  def setChatheadDimension(size: Int): Unit = {
    ViewUtils.setWidth(chathead, size)
    ViewUtils.setHeight(chathead, size)
  }

  def setFooterWidth(width: Int): Unit = {
    ViewUtils.setWidth(footer, width)
  }

  private def initAttributes(attrs: AttributeSet): Unit = {
    var chatheadSize: Int = 0
    var a: TypedArray = null
    try {
      a = getContext.obtainStyledAttributes(attrs, R.styleable.ChatheadWithTextFooter)
      chatheadSize = a.getDimensionPixelSize(R.styleable.ChatheadWithTextFooter_chathead_size, 0)
    } finally {
      if (a != null) {
        a.recycle()
      }
    }
    if (chatheadSize > 0) {
      setChatheadDimension(chatheadSize)
    }
  }
}
