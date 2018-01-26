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
import android.widget.{FrameLayout, Toast}
import com.waz.utils.returning
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.{RichView, ViewUtils}
import com.waz.zclient.{FragmentHelper, R}
import android.view.animation.Animation
import android.view.animation.AlphaAnimation
import com.waz.api.impl.ErrorResponse
import com.waz.threading.Threading
import com.waz.zclient.common.controllers.IntegrationsController
import com.waz.zclient.utils.ContextUtils.showToast

import scala.concurrent.Future

class IntegrationDetailsSummaryFragment extends Fragment with FragmentHelper {

  implicit private def ctx = getContext

  private lazy val integrationDetailsViewController = inject[IntegrationDetailsController]
  private lazy val integrationsController = inject[IntegrationsController]

  private lazy val descriptionText = returning(view[TypefaceTextView](R.id.integration_description)) { summaryText =>
    integrationDetailsViewController.currentIntegration.map(_.description).onUi(d => summaryText.foreach(_.setText(d)))
  }

  override def onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation = {
    val parent = getParentFragment
    if (!enter && parent != null && parent.isRemoving) {
      returning(new AlphaAnimation(1, 1)) {
        _.setDuration(ViewUtils.getNextAnimationDuration(parent))
      }
    } else super.onCreateAnimation(transit, enter, nextAnim)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View ={
    val localInflater =
      if (integrationDetailsViewController.addingToConversation.isEmpty && integrationDetailsViewController.removingFromConversation.isEmpty)
        inflater.cloneInContext(new ContextThemeWrapper(getActivity, R.style.Theme_Dark))
      else
        inflater

    localInflater.inflate(R.layout.fragment_integration_details_summary, container, false)
  }


  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    val addButton = findById[FrameLayout](R.id.add_service_button)
    val removeButton = findById[FrameLayout](R.id.remove_service_button)

    if (integrationDetailsViewController.removingFromConversation.nonEmpty) {
      addButton.setVisibility(View.GONE)
      removeButton.setVisibility(View.VISIBLE)
      removeButton.onClick {
        (integrationDetailsViewController.removingFromConversation match {
          case Some((cId, uId)) =>
            integrationsController.removeBot(cId, uId)
          case _ =>
            Future.successful(Left(ErrorResponse.internalError("Invalid conversation or bot")))
        }).map {
          case Left(e) =>
            showToast(integrationsController.errorMessage(e))
            Future.successful(())
          case Right(_) =>
            getParentFragment.getFragmentManager.popBackStack()
        } (Threading.Ui)
      }
    } else {
      addButton.setVisibility(View.VISIBLE)
      removeButton.setVisibility(View.GONE)
      addButton.onClick(integrationDetailsViewController.onAddServiceClick ! (()))
    }
    descriptionText
  }
}
