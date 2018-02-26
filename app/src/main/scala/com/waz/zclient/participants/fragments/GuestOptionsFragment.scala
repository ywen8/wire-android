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

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.SwitchCompat
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{CompoundButton, TextView}
import com.waz.ZLog.ImplicitTag._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{FragmentHelper, OnBackPressedListener, R}

class GuestOptionsFragment extends FragmentHelper with OnBackPressedListener {

  import Threading.Implicits.Background

  implicit def cxt: Context = getActivity

  private lazy val zms = inject[Signal[ZMessaging]]

  private lazy val convCtrl = inject[ConversationController]

  //TODO look into using something more similar to SwitchPreference
  private lazy val guestsSwitch = returning(view[SwitchCompat](R.id.guest_toggle)) { vh =>
    convCtrl.currentConvIsTeamOnly.onUi(teamOnly => vh.foreach(_.setChecked(!teamOnly)))
  }

  private lazy val guestsTitle = view[TextView](R.id.guest_toggle_title)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.guest_options_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    guestsSwitch
  }

  override def onResume() = {
    super.onResume()
    guestsSwitch.foreach {
      _.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener {
        override def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = {
          setGuestsSwitchEnabled(false)
          (for {
            z <- zms.head
            c <- convCtrl.currentConvId.head
            resp <- z.conversations.setToTeamOnly(c, !isChecked)
          } yield resp).map { resp =>
            setGuestsSwitchEnabled(true)
            resp match {
              case Right(_) => //
              case Left(err) =>
                //TODO handle error properly
                showToast(s"Something went wrong: $err")
            }
          }(Threading.Ui)
        }
      })
    }
  }

  private def setGuestsSwitchEnabled(enabled: Boolean) = {
    guestsSwitch.foreach(_.setEnabled(enabled))
    guestsTitle.foreach(_.setAlpha(if (enabled) 1f else 0.5f))
  }

  override def onStop() = {
    guestsSwitch.foreach(_.setOnCheckedChangeListener(null))
    super.onStop()
  }

  override def onBackPressed(): Boolean = {
    getFragmentManager.popBackStackImmediate
    true
  }
}

object GuestOptionsFragment {
  val Tag = implicitLogTag
}
