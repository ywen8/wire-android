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
package com.waz.zclient.usersearch.views

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.{KeyEvent, LayoutInflater, View}
import android.widget.{FrameLayout, TextView}
import com.waz.api.User
import com.waz.zclient.common.views.{PickableElement, PickerSpannableEditText}
import com.waz.zclient.ui.utils.TypefaceUtils
import com.waz.zclient.{R, ViewHelper}

object SearchBoxView {

  trait Callback extends PickerSpannableEditText.Callback {
    def onFocusChange(hasFocus: Boolean): Unit

    def onClearButton(): Unit
  }

}

class SearchBoxView(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends FrameLayout(context, attrs, defStyleAttr) with ViewHelper {
  private var inputEditText: PickerSpannableEditText = null
  private var clearButton: TextView = null
  private var colorBottomBorder: View = null
  private var callback: SearchBoxView.Callback = null

  init(context)

  def this(context: Context, attrs: AttributeSet) {
    this(context, attrs, 0)
  }

  def this(context: Context) {
    this(context, null)
  }

  private def init(context: Context): Unit = {
    LayoutInflater.from(context).inflate(R.layout.search_box_view, this, true)
    clearButton = findById[TextView](R.id.gtv_pickuser__clearbutton)
    inputEditText = findById[PickerSpannableEditText](R.id.puet_pickuser__searchbox)
    colorBottomBorder = findById[View](R.id.v_people_picker__input__color_bottom_border)
    val hintColorStartUI = ContextCompat.getColor(getContext, R.color.text__secondary_light)
    inputEditText.setTypeface(TypefaceUtils.getTypeface(context.getString(R.string.wire__typeface__light)))
    inputEditText.setCallback(new PickerSpannableEditText.Callback() {
      def onRemovedTokenSpan(element: PickableElement): Unit = {
        if (callback != null) {
          callback.onRemovedTokenSpan(element)
        }
      }

      def afterTextChanged(s: String): Unit = {
        if (callback != null) {
          callback.afterTextChanged(s)
        }
      }
    })
    inputEditText.setFocusable(true)
    inputEditText.setFocusableInTouchMode(true)
    inputEditText.setHintTextColor(hintColorStartUI)
    inputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean =
        Option(callback).filter(_ => actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode == KeyEvent.KEYCODE_ENTER)) match {
          case Some(c) =>
            true
          case _ =>
            false
        }
    })
    inputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      def onFocusChange(view: View, hasFocus: Boolean): Unit = {
        if (callback != null) {
          callback.onFocusChange(hasFocus)
        }
      }
    })
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
      inputEditText.setLineSpacing(getResources.getDimensionPixelSize(R.dimen.people_picker__input__line_spacing_extra__greater_than_android_sdk_19), 1)
    }
    clearButton.setOnClickListener(new View.OnClickListener() {
      def onClick(v: View): Unit = {
        if (callback != null) {
          callback.onClearButton()
        }
      }
    })
  }

  def setHintText(hintText: CharSequence): Unit = {
    inputEditText.setHintText(hintText)
  }

  def showClearButton(show: Boolean): Unit = {
    clearButton.setVisibility(if (show) View.VISIBLE else View.GONE)
  }

  def forceDarkTheme(): Unit = {
    val textColor = ContextCompat.getColor(getContext, R.color.text__primary_dark)
    inputEditText.setTextColor(textColor)
    inputEditText.setHintTextColor(textColor)
    clearButton.setTextColor(textColor)
    inputEditText.applyLightTheme(false)
    setBackgroundColor(Color.TRANSPARENT)
  }

  def applyLightTheme(light: Boolean): Unit = {
    inputEditText.applyLightTheme(light)
  }

  def setCallback(callback: SearchBoxView.Callback): Unit = {
    this.callback = callback
  }

  def setAccentColor(color: Int): Unit = {
    colorBottomBorder.setBackgroundColor(color)
    inputEditText.setAccentColor(color)
  }

  def addUser(user: User): Unit = {
    inputEditText.addElement(new PickableElement() {
      def name: String = {
        user.getDisplayName
      }

      def id: String =
      {
        user.getId
      }
    })
  }

  def removeUser(user: User): Unit = {
    inputEditText.removeElement(new PickableElement() {
      def name: String = {
        user.getDisplayName
      }

      def id: String =
      {
        user.getId
      }
    })
  }

  def getSearchFilter: String = {
    inputEditText.getSearchFilter
  }

  def reset(): Unit = {
    inputEditText.reset()
  }

  def setFocus(): Unit = {
    inputEditText.setCursorVisible(true)
    inputEditText.requestFocus
  }
}
