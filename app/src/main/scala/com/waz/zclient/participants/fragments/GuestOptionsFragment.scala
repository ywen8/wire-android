/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
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
package com.waz.zclient.participants.fragments

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{CompoundButton, Switch}
import com.waz.ZLog
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.zclient.{FragmentHelper, OnBackPressedListener, R}

class GuestOptionsFragment extends FragmentHelper with OnBackPressedListener {

  private lazy val zms = inject[Signal[ZMessaging]]

  private lazy val allowGustsSwitch = view[Switch](R.id.allow_guests_switch)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.guest_options_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    allowGustsSwitch.foreach { switch =>
      switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener {
        override def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = {
        }
      })
    }
  }

  override def onBackPressed(): Boolean = {
    getFragmentManager.popBackStackImmediate
    true
  }
}

object GuestOptionsFragment {
  val Tag = ZLog.ImplicitTag.implicitLogTag
}
