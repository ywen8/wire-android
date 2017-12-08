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
import com.waz.zclient.{FragmentHelper, R}

object CallQualityFragment {
  val Tag = logTagFor[CallQualityFragment]
  def newInstance(): Fragment = {
    val fragment = new CallQualityFragment
    val bundle = new Bundle()
    fragment.setArguments(bundle)
    fragment

  }
  trait Container
}

class CallQualityFragment extends BaseDialogFragment[Container] with FragmentHelper with OnClickListener {

  lazy val callQualityController = inject[CallQualityController]


  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = LayoutInflater.from(getActivity).inflate(R.layout.fragment_call_quality, null)

    val buttons = Seq(
      R.id.call_quality_button_1,
      R.id.call_quality_button_2,
      R.id.call_quality_button_3,
      R.id.call_quality_button_4,
      R.id.call_quality_button_5).map(findById[View](view, _))

    buttons.foreach(_.setOnClickListener(this))

    setCancelable(false)
    view
  }

  override def onClick(view: View): Unit = {
    callQualityController.callToReport ! None
    dismiss()
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_AppCompat_NoActionBar)
  }
}


