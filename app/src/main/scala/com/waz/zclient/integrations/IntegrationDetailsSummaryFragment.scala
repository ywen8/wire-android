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
package com.waz.zclient.integrations

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{ContextThemeWrapper, LayoutInflater, View, ViewGroup}
import android.widget.RelativeLayout
import com.waz.utils.returning
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.RichView
import com.waz.zclient.{FragmentHelper, R}

class IntegrationDetailsSummaryFragment extends Fragment with FragmentHelper {

  private lazy val integrationDetailsViewController = inject[IntegrationDetailsController]

  private lazy val descriptionText = returning(view[TypefaceTextView](R.id.integration_description)) { summaryText =>
    integrationDetailsViewController.currentIntegration.onUi { integrationData =>
      summaryText.foreach(_.setText(integrationData.description))
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View ={
    val localInflater =
      if (integrationDetailsViewController.addingToConversation.isEmpty)
        inflater.cloneInContext(new ContextThemeWrapper(getActivity, R.style.Theme_Dark))
      else
        inflater

    localInflater.inflate(R.layout.fragment_integration_details_summary, container, false)
  }


  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    findById[RelativeLayout](R.id.add_service_button).onClick(integrationDetailsViewController.onAddServiceClick ! (()))
    descriptionText
  }
}
