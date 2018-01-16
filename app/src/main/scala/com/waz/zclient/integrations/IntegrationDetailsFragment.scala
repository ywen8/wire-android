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

import android.content.Context
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v4.app.FragmentManager
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.ImageView
import com.waz.model.{AssetId, IntegrationData, IntegrationId, ProviderId}
import com.waz.utils.events.Signal
import com.waz.zclient.common.views.ImageAssetDrawable
import com.waz.zclient.common.views.ImageAssetDrawable.{RequestBuilder, ScaleType}
import com.waz.zclient.common.views.ImageController.{ImageSource, WireImage}
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.ContextUtils
import com.waz.zclient.{FragmentHelper, OnBackPressedListener, R, ViewHolder}
import com.waz.ZLog._
import com.waz.ZLog.ImplicitTag._
import com.waz.utils.returning
import com.waz.zclient.common.controllers.IntegrationsController
import com.waz.zclient.controllers.navigation.{INavigationController, Page}
import com.waz.zclient.conversationlist.views.{ConversationListTopToolbar, IntegrationTopToolbar}
import com.waz.zclient.preferences.views.TextButton
import com.waz.zclient.usersearch.PickUserFragment
import com.waz.zclient.utils.RichView

class IntegrationDetailsFragment extends FragmentHelper with OnBackPressedListener {
  implicit def ctx: Context = getActivity

  private lazy val controller = inject[IntegrationsController]

  private lazy val providerId = ProviderId(getArguments.getString(IntegrationDetailsFragment.ProviderId))
  private lazy val integrationId = IntegrationId(getArguments.getString(IntegrationDetailsFragment.IntegrationId))

  private var pictureView: ViewHolder[ImageView] = _

  override def onCreateView(inflater: LayoutInflater, viewContainer: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_integration_details, viewContainer, false)
  }

  override def onViewCreated(v: View, @Nullable savedInstanceState: Bundle): Unit = {
    super.onViewCreated(v, savedInstanceState)

    val nameView = returning(view[TypefaceTextView](R.id.integration_name)){ nv =>
      integration.map(_.name).onUi { name =>nv.foreach(_.setText(name)) }
    }

    val summaryView = returning(view[TypefaceTextView](R.id.integration_summary)){ sv =>
      integration.map(_.summary).onUi { summary => sv.foreach(_.setText(summary)) }
    }

    val descriptionView = returning(view[TypefaceTextView](R.id.integration_description)){ dv =>
      integration.map(_.description).onUi { description => dv.foreach(_.setText(description)) }
    }

    val topToolbar = returning(view[IntegrationTopToolbar](R.id.integration_top_toolbar)){ t =>
      t.foreach(_.closeButtonEnd.onClick(close()))
      t.foreach(_.backButton.onClick(goBack()))
      integration.onUi { data => t.foreach(_.setTitle(data)) }
    }

    pictureView = returning(view[ImageView](R.id.integration_picture)){ pv =>
      pv.foreach(_.setImageDrawable(ContextUtils.getDrawable(R.drawable.services)))
    }

    val addButton = findById[TextButton](R.id.integration_add_button)

    integrationsIds ! (providerId, integrationId)

    addButton.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        verbose(s"adding a service ${integration.currentValue.map(_.name)}")

      }
    })
  }

  private val integrationsIds = Signal[(ProviderId, IntegrationId)]()
  private val integration = integrationsIds.flatMap {
    case (pId, iId) => controller.getIntegration(pId, iId)
  }

  integration.onUi{ data =>
    verbose(s"IN got integration: ${data.name} ")
  }

  integration.map(_.assets.headOption).onUi {
    case Some(asset) =>
      pictureAssetId ! asset.id // TODO: check the asset type for the profile pic
      pictureView.setImageDrawable(drawable)
    case None =>

  }

  private val pictureAssetId = Signal[AssetId]()
  private val picture: Signal[ImageSource] = pictureAssetId.collect{ case pic => WireImage(pic) }
  private lazy val drawable = new ImageAssetDrawable(picture, scaleType = ScaleType.CenterInside, request = RequestBuilder.Regular)

  override def onBackPressed(): Boolean = goBack()

  def goBack(): Boolean = {
    getFragmentManager.popBackStack()
    inject[INavigationController].setLeftPage(Page.PICK_USER, IntegrationDetailsFragment.Tag)
    true
  }

  def close(): Boolean = {
    getFragmentManager.popBackStack()
    getFragmentManager.popBackStack()
    inject[INavigationController].setLeftPage(Page.CONVERSATION_LIST, IntegrationDetailsFragment.Tag)
    true
  }
}

object IntegrationDetailsFragment {

  val Tag = classOf[IntegrationDetailsFragment].getName
  val IntegrationId = "ARG_INTEGRATION_ID"
  val ProviderId = "ARG_PROVIDER_ID"

  def newInstance(providerId: ProviderId, integrationId: IntegrationId) = returning(new IntegrationDetailsFragment) {
    _.setArguments(returning(new Bundle){ b =>
      b.putString(ProviderId, providerId.str)
      b.putString(IntegrationId, integrationId.str)
    })
  }

}
