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

package com.waz.zclient.appentry.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.model.UserId
import com.waz.service.AccountsService
import com.waz.threading.Threading
import com.waz.utils.returning
import com.waz.zclient.appentry.AppEntryActivity
import com.waz.zclient.appentry.fragments.FirstLaunchAfterLoginFragment._
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.{FragmentHelper, R}

object FirstLaunchAfterLoginFragment {
  val Tag: String = classOf[FirstLaunchAfterLoginFragment].getName
  val UserIdArg = "user_id_arg"

  def apply(): Fragment = new FirstLaunchAfterLoginFragment
  def apply(userId: UserId): Fragment = returning(new FirstLaunchAfterLoginFragment) { f =>
    val bundle = new Bundle()
    bundle.putString(UserIdArg, userId.str)
    f.setArguments(bundle)
  }
}

class FirstLaunchAfterLoginFragment extends FragmentHelper with View.OnClickListener {

  lazy val accountsService    = inject[AccountsService]

  private lazy val registerButton = returning(findById[ZetaButton](getView, R.id.zb__first_launch__confirm)){ v =>
    v.setIsFilled(true)
    v.setAccentColor(ContextCompat.getColor(getContext, R.color.text__primary_dark))
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {
    registerButton
  }

  override def onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup, savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_login_first_launch, viewGroup, false)

  override def onResume(): Unit = {
    super.onResume()
    registerButton.setOnClickListener(this)
  }

  override def onPause(): Unit = {
    registerButton.setOnClickListener(null)
    super.onPause()
  }

  def onClick(view: View): Unit = {
    view.getId match {
      case R.id.zb__first_launch__confirm =>
        implicit val ec = Threading.Ui
        getStringArg(UserIdArg).map(UserId(_)).foreach { userId =>
          accountsService.enterAccount(userId, None)
            .flatMap(_ => accountsService.setAccount(Some(userId)))
            .foreach(_ => activity.onEnterApplication(false))
        }
    }
  }

  def activity = getActivity.asInstanceOf[AppEntryActivity]

}
