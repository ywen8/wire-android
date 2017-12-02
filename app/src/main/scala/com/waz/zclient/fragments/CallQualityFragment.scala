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
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.{LayoutInflater, View}
import android.widget.{AdapterView, ListView}
import com.waz.zclient.controllers.SignInController
import com.waz.zclient.fragments.CountryDialogFragment._
import com.waz.zclient.newreg.fragments.country.{CountryCodeAdapter, CountryController}
import com.waz.zclient.pages.BaseDialogFragment
import com.waz.zclient.utils.ViewUtils
import com.waz.zclient.{FragmentHelper, R}

object CallQualityFragment {
  def newInstance(title:String): Fragment = {
    val fragment = new ImageFragment
    val bundle = new Bundle()
    bundle.putString("title", title)
    fragment.setArguments(bundle)
    fragment

  }
}

class CallQualityFragment extends BaseDialogFragment[Container] with FragmentHelper with AdapterView.OnItemClickListener{

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    new AlertDialog.Builder(getActivity)
      // set Dialog Title
      .setTitle("Call Quality Survey")
      // Set Dialog Message
      .setMessage("Question 1").create

  }

  def onItemClick(adapterView: AdapterView[_], view: View, i: Int, l: Long): Unit = {

  }
}


