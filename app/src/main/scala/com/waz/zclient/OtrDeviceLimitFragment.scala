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
package com.waz.zclient

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.EventStream
import com.waz.utils.returning
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.utils.ContextUtils._

class OtrDeviceLimitFragment extends FragmentHelper with OnBackPressedListener with View.OnClickListener {

  val onLogout        = EventStream[Unit]()
  val onManageDevices = EventStream[Unit]()
  val onDismiss       = EventStream[Unit]()

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_otr_device_limit, container, false)
    view.setOnClickListener(this)

    returning(findById[ZetaButton](view, R.id.zb__otr_device_limit__logout)) { v =>
      v.setIsFilled(false)
      v.setOnClickListener(this)
      v.setAccentColor(getColorWithTheme(R.color.text__primary_dark, getContext))
    }

    returning(findById[ZetaButton](view, R.id.zb__otr_device_limit__manage_devices)) { v =>
      v.setIsFilled(true)
      v.setOnClickListener(this)
      v.setAccentColor(getColorWithTheme(R.color.text__primary_dark, getContext))
    }

    ZMessaging.accountsService.map {
      _.activeAccountManager.collect {
        case Some(am) => am.clientState
      }.flatten.onChanged.filter(_.clientId.isDefined).onUi { _ =>
        onDismiss ! ({})
      }
    }(Threading.Background)

    view
  }

  def onClick(v: View) =
    v.getId match {
      case R.id.zb__otr_device_limit__logout         => onLogout ! ({})
      case R.id.zb__otr_device_limit__manage_devices => onManageDevices ! ({})
    }

  def onBackPressed = true

}

object OtrDeviceLimitFragment {

  val Tag = classOf[OtrDeviceLimitFragment].getName

  def newInstance = new OtrDeviceLimitFragment
}
