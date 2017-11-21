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
import android.view.ViewGroup.LayoutParams
import android.view._
import android.widget.TextView.OnEditorActionListener
import android.widget.{FrameLayout, ProgressBar, TableLayout, TextView}
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.ui.cursor.CursorEditText
import com.waz.zclient.ui.text.{TypefaceEditText, TypefaceTextView}
import com.waz.zclient.utils.{ContextUtils, _}
import com.waz.zclient.{R, ViewHelper}

import scala.concurrent.Future

class NumberCodeInput(context: Context, attrs: AttributeSet, style: Int) extends FrameLayout(context, attrs, style) with ViewHelper { self =>
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.code_input)

  val inputCount = 6
  val errorText = findById[TypefaceTextView](R.id.error_text)
  val progressBar = findById[ProgressBar](R.id.progress_bar)
  val texts = Seq(R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5, R.id.t6).map(findById[TypefaceEditText])
  val codeText = Signal[String]("")
  private var onCodeSet = (_: String) => Future.successful(Option.empty[String])

  progressBar.setIndeterminate(true)
  progressBar.setVisible(false)
  errorText.setVisible(false)
  progressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextUtils.getColor(R.color.teams_inactive_button)))
  setupInputs()
  codeText.onUi { code =>
    errorText.setVisible(false)
    if (code.length >= inputCount)
      setCode(code)
  }

  def inputCode(code: String): Unit = {
    texts.zip(code.toCharArray).foreach {
      case (editText, char) => editText.setText(char.toString)
    }
  }

  def requestInputFocus(): Unit = texts.headOption.foreach(_.requestFocus())

  private def setupInputs(): Unit = {
    texts.foreach(_.setAccentColor(ContextUtils.getColor(R.color.accent_blue)))
    texts.zipWithIndex.foreach {
      case (editText, i) =>

        def handleBackKeyEvent(event: KeyEvent): Boolean = {
          if (event.getKeyCode == KeyEvent.KEYCODE_DEL) {
            if (i != 0) {
              editText.setText("")
              texts(i - 1).requestFocus()
            }
          }
          false
        }

        editText.setOnKeyDownListener(new View.OnKeyListener {
          override def onKey(v: View, keyCode: Int, event: KeyEvent): Boolean = handleBackKeyEvent(event)
        })
        editText.addTextChangedListener(new TextWatcher {
          override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}
          override def afterTextChanged(s: Editable): Unit = {
            if (s.length() > 1) {
              val remainder = s.subSequence(1, s.length())
              s.delete(1, s.length())
              if (i < inputCount - 1) {
                texts(i + 1).setText(remainder)
              }
            } else if (s.length() > 0 && i < inputCount - 1) {
              texts(i + 1).requestFocus()
            }
            if (s.length() <= 1) {
              self.codeText ! getCode
            }
          }
          override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
        })
    }
  }

  def getCode: String = texts.map(_.getText.toString).mkString

  def setOnCodeSet(f: (String) => Future[Option[String]]): Unit = onCodeSet = f

  private def setCode(code: String): Unit = {
    progressBar.setVisible(true)
    errorText.setVisible(false)
    texts.foreach(_.setEnabled(false))
    onCodeSet(code).map {
      case Some(error) =>
        errorText.setVisible(true)
        errorText.setText(error)
        progressBar.setVisible(false)
        texts.foreach(_.setEnabled(true))
      case _ =>
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
