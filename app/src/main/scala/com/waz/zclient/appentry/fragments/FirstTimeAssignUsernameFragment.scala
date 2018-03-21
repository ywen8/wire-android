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

import java.text.Normalizer
import java.util.Locale

import android.content.Context
import android.graphics.{Color, PorterDuff}
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v4.app.Fragment
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.ImageView
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag._
import com.waz.model.Handle
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.crypto.ZSecureRandom
import com.waz.utils.events.Signal
import com.waz.zclient.common.controllers.global.AccentColorController
import com.waz.zclient.pages.BaseFragment
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.{BitmapUtils, ColorUtils, ResourceUtils, TextViewUtils}
import com.waz.zclient.ui.views.ZetaButton
import com.waz.zclient.utils.{ContextUtils, StringUtils, ViewUtils}
import com.waz.zclient.common.views.ImageAssetDrawable
import com.waz.zclient.common.views.ImageAssetDrawable.{RequestBuilder, ScaleType}
import com.waz.zclient.common.views.ImageController.{ImageSource, WireImage}
import com.waz.zclient.{FragmentHelper, R}

import scala.util.{Failure, Success}

object FirstTimeAssignUsernameFragment {
  val TAG: String = classOf[FirstTimeAssignUsernameFragment].getName
  private val ARG_SUGGESTED_USERNAME: String = "ARG_SUGGESTED_USERNAME"
  private val ARG_NAME: String = "ARG_NAME"

  def newInstance(name: String, suggestedUsername: String): Fragment = {
    val fragment: Fragment = new FirstTimeAssignUsernameFragment
    val arg: Bundle = new Bundle
    arg.putString(ARG_NAME, name)
    arg.putString(ARG_SUGGESTED_USERNAME, suggestedUsername)
    fragment.setArguments(arg)
    fragment
  }

  trait Container {
    def onChooseUsernameChosen(): Unit

    def onKeepUsernameChosen(username: String): Unit

    def onOpenUrl(url: String): Unit
  }

}

class FirstTimeAssignUsernameFragment extends BaseFragment[FirstTimeAssignUsernameFragment.Container]
  with FragmentHelper {

  import Threading.Implicits.Ui

  private implicit def context: Context = getActivity

  private val USERNAME_MAX_LENGTH = 21
  private val NORMAL_ATTEMPTS = 30
  private val RANDOM_ATTEMPTS = 20
  private val MAX_RANDOM_TRAILING_NUMBER = 1000

  private lazy val zms = inject[Signal[ZMessaging]]
  private lazy val accentColor = inject[AccentColorController].accentColor

  private lazy val nameTextView = findById[TypefaceTextView](getView, R.id.ttv__name)
  private lazy val usernameTextView = findById[TypefaceTextView](getView, R.id.ttv__username)
  private lazy val backgroundImageView  = findById[ImageView](getView, R.id.user_photo)
  private lazy val keepButton = findById[ZetaButton](getView, R.id.zb__username_first_assign__keep)

  private lazy val self = for {
    z <- zms
    userData <- z.usersStorage.signal(z.selfUserId)
  } yield userData

  private var suggestedUsername: String = ""

  override def onViewCreated(view: View, savedInstanceState: Bundle) = {

    val vignetteOverlay: ImageView = ViewUtils.getView(view, R.id.iv_background_vignette_overlay)
    val chooseYourOwnButton: ZetaButton = ViewUtils.getView(view, R.id.zb__username_first_assign__choose)
    val summaryTextView: TypefaceTextView = ViewUtils.getView(view, R.id.ttv__username_first_assign__summary)
    val selfPicture: Signal[ImageSource] = self.map(_.picture).collect{case Some(pic) => WireImage(pic)}

    backgroundImageView.setImageDrawable(new ImageAssetDrawable(selfPicture, scaleType = ScaleType.CenterCrop, request = RequestBuilder.Single))
    usernameTextView.setVisibility(View.INVISIBLE)
    keepButton.setVisibility(View.GONE)

    val darkenColor: Int = ColorUtils.injectAlpha(ResourceUtils.getResourceFloat(getResources, R.dimen.background_solid_black_overlay_opacity), Color.BLACK)
    vignetteOverlay.setImageBitmap(BitmapUtils.getVignetteBitmap(getResources))
    vignetteOverlay.setColorFilter(darkenColor, PorterDuff.Mode.DARKEN)
    chooseYourOwnButton.setIsFilled(true)
    chooseYourOwnButton.setOnClickListener(new View.OnClickListener() {
      def onClick(view: View): Unit = {
        getContainer.onChooseUsernameChosen()
      }
    })

    keepButton.setIsFilled(false)
    keepButton.setOnClickListener(new View.OnClickListener() {
      def onClick(view: View): Unit = {
        getContainer.onKeepUsernameChosen(suggestedUsername)
      }
    })

    TextViewUtils.linkifyText(summaryTextView, Color.WHITE, com.waz.zclient.ui.R.string.wire__typeface__light, false, new Runnable() {
      def run(): Unit = getContainer.onOpenUrl(getString(R.string.usernames__learn_more__link))
    })

    self.onUi { self =>
      self.handle.foreach{ handle =>
        suggestedUsername = handle.string
        usernameTextView.setText(StringUtils.formatHandle(handle.string))
      }
      nameTextView.setText(self.getDisplayName)
    }

    accentColor.onUi { color =>
      chooseYourOwnButton.setAccentColor(color.getColor)
      keepButton.setAccentColor(color.getColor)
    }

    self.onUi(user => startUsernameGenerator(user.name))
  }

  override def onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup, @Nullable savedInstanceState: Bundle): View =
    inflater.inflate(R.layout.fragment_username_first_launch, container, false)

  override def onBackPressed(): Boolean = {
    super.onBackPressed()
    true
  }

  def onValidUsernameGenerated(generatedUsername: String) = {
    ZLog.verbose(s"onValidUsernameGenerated $generatedUsername")
    suggestedUsername = generatedUsername
    usernameTextView.setText(StringUtils.formatHandle(suggestedUsername))
    usernameTextView.setVisibility(View.VISIBLE)
    keepButton.setVisibility(View.VISIBLE)
  }

  private def startUsernameGenerator(baseName: String): Unit = {
    val baseGeneratedUsername = generateUsernameFromName(baseName)
    val randomUsername = generateUsernameFromName("")
    zms.head.map { z =>
      z.handlesClient.getHandlesValidation(getAttempts(baseGeneratedUsername, NORMAL_ATTEMPTS) ++ getAttempts(randomUsername, RANDOM_ATTEMPTS))
      .onComplete {
        case Success(response) =>
          response match {
            case Left(_) =>
              ContextUtils.showToast(R.string.username__set__toast_error)
            case Right(r) =>
              r.foreach(nicks => onValidUsernameGenerated(nicks.head.username))
          }
        case Failure(_) =>
          ContextUtils.showToast(R.string.username__set__toast_error)
      }
    }
  }

  private def getAttempts(base: String, attempts: Int): Seq[Handle] =
    (0 until attempts).map(getTrailingNumber).map { tN =>
      Handle(StringUtils.truncate(base, USERNAME_MAX_LENGTH - tN.length) + tN)
    }

  private def getTrailingNumber(attempt: Int): String = {
    val blah = ZSecureRandom.nextInt(0, MAX_RANDOM_TRAILING_NUMBER * 10 ^ (attempt / 10))
    if (attempt > 0) String.format(Locale.getDefault, "%d", Int.box(blah))
    else ""
  }

  private def generateUsernameFromName(name: String): String = {
    var cleanName: String = Handle.transliterated(name).toLowerCase
    cleanName = Normalizer.normalize(cleanName, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
    cleanName = Normalizer.normalize(cleanName, Normalizer.Form.NFD).replaceAll("\\W+", "")
    if (cleanName.isEmpty) {
      cleanName = generateFromDictionary()
    }
    cleanName
  }

  private def generateFromDictionary(): String = {
    val names = ContextUtils.getStringArray(R.array.random_names)
    val adjectives = ContextUtils.getStringArray(R.array.random_adjectives)
    val namesIndex = ZSecureRandom.nextInt(names.length)
    val adjectivesIndex = ZSecureRandom.nextInt(adjectives.length)
    (adjectives(adjectivesIndex) + names(namesIndex)).toLowerCase
  }
}
