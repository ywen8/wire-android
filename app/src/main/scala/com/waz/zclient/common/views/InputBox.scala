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
import android.widget.{LinearLayout, ProgressBar}
import com.waz.threading.Threading
import com.waz.zclient.common.views.InputBox._
import com.waz.zclient.ui.cursor.CursorEditText
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils._
import com.waz.zclient.views.GlyphButton
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

  confirmationButton.onClick {
    hideErrorMessage()
    progressBar.setVisible(true)
    editText.setEnabled(false)
    confirmationButton.setEnabled(false)
    onClick(editText.getText.toString).map { errorMessage =>
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
    text.foreach(errorText.setText)
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

  case class Validator(f: String => Boolean)

  object PasswordValidator extends Validator({ t =>
    //TODO: Where are these values?
    t.length >= 6 && t.length <= 101
  })

  object NameValidator extends Validator(_.length >= 2)

  //TODO: do
  object UsernameValidator extends Validator({ t =>
    t.length > 2
  })

  object EmailValidator extends Validator({ t =>
    //TODO: Get a better validator for emails
    t.contains("@") && t.contains(".") && t.length >= 3
  })
}
