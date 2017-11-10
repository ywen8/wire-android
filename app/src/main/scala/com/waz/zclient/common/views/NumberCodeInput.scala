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
import android.content.res.ColorStateList
import android.text.{Editable, TextWatcher}
import android.util.AttributeSet
import android.view.{ActionMode, Menu, MenuItem}
import android.view.ViewGroup.LayoutParams
import android.widget.{FrameLayout, ProgressBar, TableLayout}
import com.waz.threading.Threading
import com.waz.zclient.ui.cursor.CursorEditText
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils
import com.waz.zclient.{R, ViewHelper}
import com.waz.zclient.utils._

import scala.concurrent.Future

class NumberCodeInput(context: Context, attrs: AttributeSet, style: Int) extends FrameLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.code_input)

  val inputCount = 6
  val codeInput = findById[CursorEditText](R.id.code_edit_text)
  val errorText = findById[TypefaceTextView](R.id.error_text)
  val progressBar = findById[ProgressBar](R.id.progress_bar)
  val texts = Seq(R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5, R.id.t6).map(findById[TypefaceTextView])
  private var onCodeSet = (_: String) => Future.successful(Option.empty[String])

  progressBar.setIndeterminate(true)
  progressBar.setVisible(false)
  errorText.setVisible(false)
  progressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextUtils.getColor(R.color.teams_inactive_button)))

  codeInput.setCustomSelectionActionModeCallback(
    new ActionMode.Callback {
      override def onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true
      override def onDestroyActionMode(mode: ActionMode): Unit = {}
      override def onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false
      override def onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = {
        Seq(android.R.id.selectAll, android.R.id.cut, android.R.id.copy, android.R.id.replaceText, android.R.id.shareText).foreach(menu.removeItem)
        true
      }
  })

  codeInput.setAccentColor(ContextUtils.getColor(R.color.accent_blue))
  codeInput.addTextChangedListener(new TextWatcher {
    override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}
    override def afterTextChanged(s: Editable): Unit = {
      (0 until inputCount).foreach { i =>
        val text = if (s.length() <= i) "" else s.charAt(i).toString
        texts(i).setText(text)
      }
      if (s.length() == inputCount) setCode(s.toString)
    }
    override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
  })
  codeInput.requestFocus()
  codeInput.setSelectionChangedCallback(new CursorEditText.SelectionChangedCallback {
    override def onSelectionChanged(selStart: Int, selEnd: Int): Unit = {
      if (selStart != selEnd || selEnd != codeInput.getText.length())
        codeInput.setSelection(codeInput.getText.length())
    }
  })

  def setOnCodeSet(f: (String) => Future[Option[String]]): Unit = onCodeSet = f

  private def setCode(code: String): Unit = {
    progressBar.setVisible(true)
    codeInput.setEnabled(false)
    errorText.setVisible(false)
    onCodeSet(code).map {
      case Some(error) =>
        errorText.setVisible(true)
        errorText.setText(error)
        codeInput.setEnabled(true)
        progressBar.setVisible(false)
      case _ =>
        progressBar.setVisible(false)
    } (Threading.Ui)
  }
}

class SingleNumberCodeInput(context: Context, attrs: AttributeSet, style: Int) extends FrameLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.single_code_number_input)
  setLayoutParams(new TableLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

  val editText = findById[CursorEditText](R.id.edit_text)
  editText.setAccentColor(ContextUtils.getColor(R.color.accent_blue))
}
