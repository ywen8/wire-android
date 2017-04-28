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
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.support.annotation.{NonNull, Nullable}
import android.text.style.{AbsoluteSizeSpan, ReplacementSpan}
import android.text.{Spanned, _}
import android.util.AttributeSet
import android.view.ActionMode.Callback
import android.view._
import android.view.inputmethod.{EditorInfo, InputConnection, InputConnectionWrapper}
import com.waz.zclient.R
import com.waz.zclient.pages.main.pickuser.UserTokenSpan
import com.waz.zclient.ui.text.SpannableEditText
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.utils.ContextUtils._

trait PickableElement {
  def id: String
  def name: String
}

class PickerSpannableEditText(val context: Context, val attrs: AttributeSet, val defStyle: Int) extends SpannableEditText(context, attrs, defStyle) with TextWatcher {

  initAttributes(attrs)
  init()

  private var flagNotifyAfterTextChanged: Boolean = true
  private var hintTextSmallScreen: String = ""
  private var hintTextSize: Int = 0
  private var lightTheme: Boolean = false
  private var elements: Set[PickableElement] = null
  private var hasText: Boolean = false
  private var callback: PickerSpannableEditText.Callback = null

  def this(context: Context, attrs: AttributeSet) {
    this(context, attrs, 0)
  }

  def this(context: Context) {
    this(context, null)
  }

  def applyLightTheme(lightTheme: Boolean): Unit = {
    this.lightTheme = lightTheme
  }

  private def initAttributes(@Nullable attrs: AttributeSet): Unit = {
    if (attrs == null) {
      return
    }
    val a: TypedArray = getContext.obtainStyledAttributes(attrs, R.styleable.PickUserEditText)
    hintTextSmallScreen = a.getString(R.styleable.PickUserEditText_hintSmallScreen)
    hintTextSize = a.getDimensionPixelSize(R.styleable.PickUserEditText_hintTextSize, 0)
    a.recycle()
  }

  private def init(): Unit = {
    setCustomSelectionActionModeCallback(new Callback {
      override def onDestroyActionMode(mode: ActionMode) = {}

      override def onCreateActionMode(mode: ActionMode, menu: Menu) = false

      override def onActionItemClicked(mode: ActionMode, item: MenuItem) = false

      override def onPrepareActionMode(mode: ActionMode, menu: Menu) = false
    })

    setLongClickable(false)
    setTextIsSelectable(false)
    addTextChangedListener(this)
    setHintText(getHint)
  }

  override protected def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    adjustHintTextForSmallScreen()
  }

  def setHintText(newHint: CharSequence): Unit = {
    val span: SpannableString = new SpannableString(newHint)
    span.setSpan(new AbsoluteSizeSpan(hintTextSize), 0, newHint.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    setHint(span)
  }

  def setCallback(callback: PickerSpannableEditText.Callback): Unit = {
    this.callback = callback
    super.setCallback(new SpannableEditText.Callback() {
      def onRemovedTokenSpan(id: String): Unit = {
        for (element <- elements) {
          if (element.id == id) {
            callback.onRemovedTokenSpan(element)
            return
          }
        }
      }

      def onClick(v: View): Unit = {
      }
    })
  }

  override protected def setHintCursorSize(cursorDrawable: ShapeDrawable): Unit = {
    if (hasText || Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1 || getHint.length() == 0) {
      return
    }
    val padding: Int = ViewUtils.toPx(getContext, PickerSpannableEditText.EXTRA_PADDING_DP)
    val textSizeDifferencePx: Int = getTextSize.toInt - hintTextSize
    val bottomPadding: Int = textSizeDifferencePx + padding
    cursorDrawable.setPadding(0, padding, 0, bottomPadding)
  }

  def addElement(element: PickableElement): Unit = {
    if (hasToken(element)) {
      return
    }
    if (elements == null) {
      elements = Set[PickableElement]()
    }
    elements = elements + element
    flagNotifyAfterTextChanged = false
    addElementToken(element.id, element.name)
    flagNotifyAfterTextChanged = true
    clearNonSpannableText()
    resetDeleteModeForSpans()
  }

  private def hasToken(element: PickableElement): Boolean = {
    if (element == null) {
      return false
    }
    val buffer: Editable = getText
    val spans: Array[SpannableEditText.TokenSpan] = buffer.getSpans(0, buffer.length, classOf[SpannableEditText.TokenSpan])
    var i: Int = spans.length - 1
    while (i >= 0) {
      {
        val span: SpannableEditText.TokenSpan = spans(i)
        if (span.getId == element.id) {
          return true
        }
      }
      {
        i -= 1
        i + 1
      }
    }
    false
  }

  def removeElement(element: PickableElement): Unit = {
    if (elements == null || !hasToken(element) || !removeSpan(element.id)) {
      return
    }
    elements = elements - element
    resetDeleteModeForSpans()
  }

  def setSelectedElements(elements: List[PickableElement]): Unit = {
    if (this.elements != null && equalLists(elements, this.elements.toList)) {
      return
    }
    this.elements = elements.toSet
    notifyDatasetChanged()
  }

  private def equalLists(one: List[PickableElement], two: List[PickableElement]): Boolean = {
    if (one == null && two == null) {
      return true
    }
    if (one == null || two == null || one.size != two.size) {
      return false
    }
    two.forall(one.contains) && one.forall(two.contains)
  }

  private def notifyDatasetChanged(): Unit = {
    reset()
    if (elements == null) {
      return
    }
    flagNotifyAfterTextChanged = false
    for (element <- elements) {
      addElementToken(element.id, element.name)
    }
    flagNotifyAfterTextChanged = true
  }

  def reset(): Unit = {
    clearNonSpannableText()
    setText("")
  }

  def getSearchFilter: String = {
    getNonSpannableText
  }

  override def onCreateInputConnection(@NonNull outAttrs: EditorInfo): InputConnection = {
    val conn: InputConnection = super.onCreateInputConnection(outAttrs)
    outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION
    new CustomInputConnection(conn, true)
  }

  private def addElementToken(userId: String, userName: String): Unit = {
    val context: Context = getContext
    val lineWidth: Int = getMeasuredWidth - getPaddingLeft - getPaddingRight
    val userTokenSpan: UserTokenSpan = new UserTokenSpan(userId, userName, context, false, lineWidth)
    userTokenSpan.setDeleteModeTextColor(getAccentColor)
    if (lightTheme) {
      userTokenSpan.setTextColor(getColor(R.color.text__primary_light)(context))
    }
    appendSpan(userTokenSpan)
    setSelection(getText.length)
  }

  private def moveTypedTextToEnd(start: Int, before: Int, count: Int): Unit = {
    val buffer: Editable = getText
    val allSpans: Array[ReplacementSpan] = buffer.getSpans(0, buffer.length, classOf[ReplacementSpan])
    if (allSpans.length <= 0) {
      return
    }
    val lastSpan: ReplacementSpan = allSpans(allSpans.length - 1)
    val to: Int = buffer.getSpanStart(lastSpan)
    if (start < to && before == 0) {
      val typedText: String = buffer.toString.substring(start, start + count)
      buffer.delete(start, start + count)
      append(typedText)
      setSelection(getText.length)
    }
  }

  private def adjustHintTextForSmallScreen(): Unit = {
    if (TextUtils.isEmpty(getHint) || TextUtils.isEmpty(hintTextSmallScreen)) {
      return
    }
    val paint: TextPaint = getPaint
    val hintWidth: Float = paint.measureText(getHint, 0, getHint.length)
    val availableTextSpace: Float = getMeasuredWidth - getPaddingLeft - getPaddingRight
    if (hintWidth > availableTextSpace) {
      setHint(hintTextSmallScreen)
    }
  }

  private def removeSelectedElementToken(): Boolean = {
    val buffer: Editable = getText
    val spans: Array[UserTokenSpan] = buffer.getSpans(0, buffer.length, classOf[UserTokenSpan])
    for (span <- spans) {
      if (span.getDeleteMode) {
        super.removeSpan(span)
        return true
      }
    }
    false
  }

  private def deleteSpanBeforeSelection(): Boolean = {
    val buffer: Editable = getText
    val spans: Array[SpannableEditText.TokenSpan] = buffer.getSpans(getSelectionStart, getSelectionEnd, classOf[SpannableEditText.TokenSpan])
    if (spans.length == 0) {
      return false
    }
    val selectionEnd: Int = getSelectionEnd
    var i: Int = spans.length - 1
    while (i >= 0) {
      {
        val span: SpannableEditText.TokenSpan = spans(i)
        val end: Int = buffer.getSpanEnd(span)
        val atLineBreak: Boolean = getLayout.getLineForOffset(end) != getLayout.getLineForOffset(selectionEnd)
        if (end <= selectionEnd || (end <= (selectionEnd + 1) && atLineBreak)) {
          super.removeSpan(span)
          setSelection(getText.length)
          return true
        }
      }
      {
        i -= 1
        i + 1
      }
    }
    false
  }

  def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {
  }

  override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
    moveTypedTextToEnd(start, before, count)
  }

  def afterTextChanged(s: Editable): Unit = {
    if (!notifyTextWatcher) {
      return
    }
    if (flagNotifyAfterTextChanged) {
      callback.afterTextChanged(getSearchFilter)
    }
    val hadText: Boolean = hasText
    hasText = s.length > 0
    if (hadText && !hasText || !hadText && hasText) {
      updateCursor()
    }
  }

  private class CustomInputConnection (val target: InputConnection, val mutable: Boolean) extends InputConnectionWrapper(target, mutable) {
    override def deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean = {
      sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    override def sendKeyEvent(event: KeyEvent): Boolean = {
      if (event.getAction == KeyEvent.ACTION_DOWN && event.getKeyCode == KeyEvent.KEYCODE_DEL) {
        if (removeSelectedElementToken()) {
          return true
        }
        if (deleteSpanBeforeSelection()) {
          return true
        }
      }
      super.sendKeyEvent(event)
    }
  }

}

object PickerSpannableEditText {
  val EXTRA_PADDING_DP: Int = 2

  trait Callback {
    def onRemovedTokenSpan(element: PickableElement): Unit

    def afterTextChanged(s: String): Unit
  }

}
