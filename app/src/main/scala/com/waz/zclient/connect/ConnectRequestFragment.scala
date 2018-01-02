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
package com.waz.zclient.connect

import android.os.Bundle
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import com.waz.model.UserId
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.connect.ConnectRequestFragment.Container
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.{FragmentHelper, R}

import scala.util.Try

class ConnectRequestFragment extends BaseFragment[Container] with FragmentHelper {

  import ConnectRequestFragment._

  private val scrollToOnOpen = Signal(Option.empty[UserId])

  private var rootView: View = _

  private lazy val adapter = new ConnectRequestAdapter(getContext)
  private lazy val recyclerView = returning(findById[RecyclerView](rootView, R.id.connect_request_list)) { rv =>
    rv.setAdapter(adapter)
    rv.setLayoutManager(new LinearLayoutManager(getContext, LinearLayoutManager.VERTICAL, false))
  }

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View = {
    rootView = inflater.inflate(R.layout.fragment_connect_request_inbox, viewContainer, false)
    recyclerView //initialise

    adapter.incomingRequests.map(_.isEmpty).onUi {
      case true => getContainer.dismissInboxFragment()
      case _ => //
    }

    (for {
      Some(u) <- scrollToOnOpen
      req     <- adapter.incomingRequests
    } yield if (req.contains(u)) Some(u) else None).onUi {
      case Some(u) =>
        adapter.findPosition(u).foreach { p =>
          recyclerView.scrollToPosition(p)
          scrollToOnOpen ! None
        }
      case _ => //
    }

    scrollToOnOpen ! Try(getArguments.getString(SelectedUser)).toOption.map(UserId)

    rootView
  }

  //FIXME - sometimes this is not called by the SecondPageFragment, so the position setting is lost.
  def setVisibleConnectRequest(userId: UserId) =
    scrollToOnOpen ! Some(userId)
}

object ConnectRequestFragment {

  val FragmentTag = classOf[ConnectRequestFragment].getName
  val SelectedUser = "ARG_SELECTED_USER"

  trait Container {
    def dismissInboxFragment(): Unit
  }

  def newInstance(userId: String) = {
    returning(new ConnectRequestFragment) { f =>
      f.setArguments(returning(new Bundle)(_.putString(SelectedUser, userId)))
    }
  }

}


