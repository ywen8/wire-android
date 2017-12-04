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
import android.content.res.{ColorStateList, TypedArray}
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.{LinearLayout, ProgressBar, TextView}
import com.waz.threading.Threading
import com.waz.zclient.common.views.InputBox._
import com.waz.zclient.ui.cursor.CursorEditText
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils._
import com.waz.zclient.{R, ViewHelper}

import scala.concurrent.Future

class InputBox(context: Context, attrs: AttributeSet, style: Int) extends LinearLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.single_input_box)
  setOrientation(LinearLayout.VERTICAL)

  private val attributesArray: TypedArray =
    context.getTheme.obtainStyledAttributes(attrs, R.styleable.InputBox, 0, 0)
  private val hintAttr = Option(attributesArray.getString(R.styleable.InputBox_hint))

  val editText = findById[CursorEditText](R.id.edit_text)
  val hintText = findById[TypefaceTextView](R.id.hint_text)
  val confirmationButton = findById[GlyphButton](R.id.confirmation_button)
  val errorText = findById[TypefaceTextView](R.id.error_text)
  val progressBar = findById[ProgressBar](R.id.progress_bar)
  val startText = findById[TypefaceTextView](R.id.start_text)

  private var validator = Option.empty[Validator]
  private var onClick = (_: String) => Future.successful(Option.empty[String])

  confirmationButton.setBackgroundColors(ContextUtils.getColor(R.color.accent_blue), ContextUtils.getColor(R.color.teams_inactive_button))
  hintAttr.foreach(hintText.setText)
  editText.setAccentColor(ContextUtils.getColor(R.color.accent_blue))
  editText.addTextListener { text =>
    validate(text)
    hideErrorMessage()
    hintText.setVisible(text.isEmpty)
  }
  validate(editText.getText.toString)
  progressBar.setIndeterminate(true)
  progressBar.setVisible(false)
  errorText.setVisible(false)
  progressBar.setIndeterminateTintList(ColorStateList.valueOf(ContextUtils.getColor(R.color.teams_inactive_button)))
  editText.setImeOptions(EditorInfo.IME_ACTION_DONE)

  editText.setOnEditorActionListener(new OnEditorActionListener {
    override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean = {
      if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_NEXT) {
        if (validator.forall(_.f(editText.getText.toString)))
          confirmationButton.performClick()
      }
      false
    }
  })

  confirmationButton.onClick {
    hideErrorMessage()
    progressBar.setVisible(true)
    editText.setEnabled(false)
    confirmationButton.setEnabled(false)
    val content =
      if (validator.forall(_.shouldTrim))
        editText.getText.toString.trim
      else
        editText.getText.toString

    onClick(content).map { errorMessage =>
      errorMessage.foreach(t => showErrorMessage(Some(t)))
      progressBar.setVisible(false)
      editText.setEnabled(true)
      confirmationButton.setEnabled(true)
    } (Threading.Ui)
  }


  def setValidator(validator: Validator): Unit = {
    this.validator = Option(validator)
    validate(editText.getText.toString)
  }

  def showErrorMessage(text: Option[String] = None): Unit = {
    text.map(_.toUpperCase).foreach(errorText.setText)
    errorText.setVisible(true)
  }

  def hideErrorMessage(): Unit = {
    errorText.setVisible(false)
  }

  def setInputType(inputType: Int): Unit = editText.setInputType(inputType)

  private def validate(text: String): Unit = {
    if (validator.forall(_.f(text))) {
      confirmationButton.setEnabled(true)
    } else {
      confirmationButton.setEnabled(false)
    }
  }

  def setOnClick(f: (String) => Future[Option[String]]): Unit = onClick = f
}

object InputBox {

  case class Validator(f: String => Boolean) {
    def shouldTrim: Boolean = true
  }

  object PasswordValidator extends Validator({ t =>
    t.length >= 8 && t.length <= 101
  }) {
    override def shouldTrim: Boolean = false
  }

  object NameValidator extends Validator(_.trim.length >= 2)

  //TODO: Unify this code with the one from the change username fragment
  object UsernameValidator extends Validator({ t =>
    val ValidUsername = s"""^([a-z]|[0-9]|_)*""".r
    val MaxLength = 21
    val MinLength = 2
    t match {
      case ValidUsername(_) =>
        t.length match {
          case l if l > MaxLength => false
          case l if l < MinLength => false
          case _ => true
        }
      case _ => false
    }
  })

  object EmailValidator extends Validator({ t =>
    t.contains("@") && t.contains(".") && t.trim.length >= 3
  })
}
