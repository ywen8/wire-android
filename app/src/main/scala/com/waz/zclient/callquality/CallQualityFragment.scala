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

package com.waz.zclient.fragments
import android.app.{AlertDialog, Dialog}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View}
import android.widget.AdapterView
import com.waz.zclient.fragments.CallQualityFragment._
import com.waz.zclient.pages.BaseDialogFragment
import com.waz.zclient.{FragmentHelper, R}

object CallQualityFragment {
  def newInstance(): Fragment = {
    val fragment = new CallQualityFragment
    val bundle = new Bundle()
    fragment.setArguments(bundle)
    fragment

  }
  trait Container
}

class CallQualityFragment extends BaseDialogFragment[Container] with FragmentHelper with OnClickListener {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val view = LayoutInflater.from(getActivity).inflate(R.layout.fragment_call_quality, null)

    lazy val btn1 = findById[View](view, R.id.call_quality_button_1)
    lazy val btn2 = findById[View](view, R.id.call_quality_button_2)
    lazy val btn3 = findById[View](view, R.id.call_quality_button_3)
    lazy val btn4 = findById[View](view, R.id.call_quality_button_4)
    lazy val btn5 = findById[View](view, R.id.call_quality_button_5)

    Seq(btn1, btn2, btn3, btn4, btn5).foreach(_.setOnClickListener(this))

    new AlertDialog.Builder(getActivity).setView(view).setCancelable(true).create
  }

  override def onClick(view: View): Unit = {
    
  }
}


