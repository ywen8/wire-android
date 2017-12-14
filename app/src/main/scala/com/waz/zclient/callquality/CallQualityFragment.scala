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

package com.waz.zclient.callquality
import android.os.Bundle
import android.support.v4.app.{DialogFragment, Fragment}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.ZLog._
import com.waz.zclient.callquality.CallQualityFragment._
import com.waz.zclient.pages.BaseDialogFragment
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils
import com.waz.zclient.{FragmentHelper, R}

object CallQualityFragment {
  val Tag = logTagFor[CallQualityFragment]

  val QuestionTypeArg = "QuestionTypeArg"
  val CallSetupQuality = 1
  val CallQuality = 2

  def newInstance(questionType: Int): Fragment = {
    val fragment = new CallQualityFragment
    val bundle = new Bundle()
    bundle.putInt(QuestionTypeArg, questionType)
    fragment.setArguments(bundle)
    fragment

  }
  trait Container
}

class CallQualityFragment extends BaseDialogFragment[Container] with FragmentHelper with OnClickListener {

  lazy val callQualityController = inject[CallQualityController]

  def questionType = Option(getArguments).map(_.getInt(QuestionTypeArg)).getOrElse(CallSetupQuality)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = LayoutInflater.from(getActivity).inflate(R.layout.fragment_call_quality, null)

    val subtitle = findById[TypefaceTextView](view, R.id.subtitle)

    val buttons = Seq(
      R.id.call_quality_button_1,
      R.id.call_quality_button_2,
      R.id.call_quality_button_3,
      R.id.call_quality_button_4,
      R.id.call_quality_button_5).map(findById[View](view, _))

    buttons.foreach(_.setOnClickListener(this))

    questionType match {
      case CallSetupQuality =>
//        ContextUtils.getString(R.string.stuff)
        subtitle.setText("Call setup quality")
      case CallQuality =>
        subtitle.setText("Call quality")
      case _ =>
    }

    setCancelable(false)
    view
  }

  override def onClick(view: View): Unit = {

    val quality = view.getId match {
      case R.id.call_quality_button_1 => 1
      case R.id.call_quality_button_2 => 2
      case R.id.call_quality_button_3 => 3
      case R.id.call_quality_button_4 => 4
      case R.id.call_quality_button_5 => 5
      case _ => 0
    }

    questionType match {
      case CallSetupQuality =>
        callQualityController.setupQuality = quality
        callQualityController.callQualityShouldOpen ! (())
      case CallQuality =>
        callQualityController.callQuality = quality
        callQualityController.callToReport ! None
      case _ =>
    }
    dismiss()
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_AppCompat_NoActionBar)
  }
}


