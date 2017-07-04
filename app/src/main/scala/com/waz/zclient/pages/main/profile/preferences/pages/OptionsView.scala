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
package com.waz.zclient.pages.main.profile.preferences.pages

import android.content.{Context, Intent}
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.{Fragment, FragmentTransaction}
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.waz.content.GlobalPreferences
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, EventStream, Signal}
import com.waz.zclient._
import com.waz.zclient.pages.main.profile.preferences.dialogs.DownloadImagesListDialog
import com.waz.zclient.pages.main.profile.preferences.pages.OptionsView._
import com.waz.zclient.pages.main.profile.preferences.views.{SwitchPreference, TextButton}
import com.waz.zclient.preferences.PreferencesActivity
import com.waz.zclient.utils.BackStackKey

trait OptionsView {
  val onVbrSwitch: EventStream[Boolean]
  val onVibrationSwitch: EventStream[Boolean]
  val onSendButtonSwitch: EventStream[Boolean]

  def setVbr(active: Boolean): Unit
  def setVibration(active: Boolean): Unit
   def setSendButton(active: Boolean): Unit

  def setSounds(string: String): Unit
  def setRingtone(string: String): Unit
  def setTextTone(string: String): Unit
  def setPingTone(string: String): Unit
  def setDownloadPictures(string: String): Unit
}

object OptionsView {
  val RingToneResultId = 0
  val TextToneResultId = 1
  val PingToneResultId = 2
}

class OptionsViewImpl(context: Context, attrs: AttributeSet, style: Int) extends LinearLayout(context, attrs, style) with OptionsView with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  implicit val ec = Threading.Ui
  protected lazy val zms = inject[Signal[ZMessaging]]

  inflate(R.layout.preferences_options_layout)

  val contactsSwitch       = findById[SwitchPreference](R.id.preferences_contacts)
  val vbrSwitch            = findById[SwitchPreference](R.id.preferences_vbr)
  val vibrationSwitch      = findById[SwitchPreference](R.id.preferences_vibration)
  val darkThemeSwitch      = findById[SwitchPreference](R.id.preferences_dark_theme)
  val sendButtonSwitch     = findById[SwitchPreference](R.id.preferences_send_button)
  val soundsButton         = findById[TextButton](R.id.preferences_sounds)
  val downloadImagesButton = findById[TextButton](R.id.preferences_options_image_download)

  val ringToneButton         = findById[TextButton](R.id.preference_sounds_ringtone)
  val textToneButton         = findById[TextButton](R.id.preference_sounds_text)
  val pingToneButton         = findById[TextButton](R.id.preference_sounds_ping)

  override val onVbrSwitch        = vbrSwitch.onCheckedChange
  override val onVibrationSwitch  = vibrationSwitch.onCheckedChange
  override val onSendButtonSwitch = sendButtonSwitch.onCheckedChange

  private lazy val defaultRingToneUri: Uri = Uri.parse("android.resource://" + getContext.getPackageName + "/" + R.raw.ringing_from_them)
  private lazy val defaultTextToneUri: Uri = Uri.parse("android.resource://" + getContext.getPackageName + "/" + R.raw.new_message)
  private lazy val defaultPingToneUri: Uri = Uri.parse("android.resource://" + getContext.getPackageName + "/" + R.raw.ping_from_them)

  private var ringToneUri = ""
  private var textToneUri = ""
  private var pingToneUri = ""

  contactsSwitch.setPreference(GlobalPreferences.ShareContacts)
  darkThemeSwitch.setPreference(GlobalPreferences.DarkTheme)

  downloadImagesButton.onClickEvent { _ =>
    zms.head.flatMap(_.prefs.preference(GlobalPreferences.DownloadImages).apply()).map(pref => showPrefDialog(DownloadImagesListDialog(pref), DownloadImagesListDialog.Tag))
  }

  ringToneButton.onClickEvent{ _ => showRingtonePicker(RingtoneManager.TYPE_RINGTONE, defaultRingToneUri, RingToneResultId, ringToneUri)}
  textToneButton.onClickEvent{ _ => showRingtonePicker(RingtoneManager.TYPE_NOTIFICATION, defaultTextToneUri, TextToneResultId, textToneUri) }
  pingToneButton.onClickEvent{ _ => showRingtonePicker(RingtoneManager.TYPE_NOTIFICATION, defaultPingToneUri, PingToneResultId, pingToneUri) }

  override def setVbr(active: Boolean) = vbrSwitch.setChecked(active)

  override def setVibration(active: Boolean) = vibrationSwitch.setChecked(active)

  override def setSendButton(active: Boolean) = sendButtonSwitch.setChecked(active)

  override def setSounds(string: String) = soundsButton.setSubtitle(string)

  override def setRingtone(uri: String) = {
    ringToneUri = uri
    setToneSubtitle(ringToneButton, defaultRingToneUri, uri)
  }

  override def setTextTone(uri: String) = {
    textToneUri = uri
    setToneSubtitle(textToneButton, defaultTextToneUri, uri)
  }

  override def setPingTone(uri: String) = {
    pingToneUri = uri
    setToneSubtitle(pingToneButton, defaultPingToneUri, uri)
  }

  private def setToneSubtitle(button: TextButton, defaultUri: Uri, uri: String): Unit = {
    val title =
      if (uri.isEmpty)
      "Silent" //TODO: res
    else if (uri.equals(defaultUri.toString))
      context.getString(R.string.pref_options_ringtones_default_summary)
    else
      RingtoneManager.getRingtone(context, Uri.parse(uri)).getTitle(context)
    button.setSubtitle(title)
  }

  override def setDownloadPictures(string: String) = {
    val keys = getResources.getStringArray(R.array.zms_image_download_values).toSeq
    val names = getResources.getStringArray(R.array.pref_options_image_download_entries).toSeq
    downloadImagesButton.setSubtitle(names.apply(keys.indexOf(string)))
  }

  private def showPrefDialog(f: Fragment, tag: String) = {
    context.asInstanceOf[BaseActivity]
      .getSupportFragmentManager
      .beginTransaction
      .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
      .add(f, tag)
      .addToBackStack(tag)
      .commit
  }

  private def showRingtonePicker(ringtoneType: Int, defaultUri: Uri, resultId: Int, selectedUri: String): Unit = {
    val intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
    val uri =
      if(selectedUri.isEmpty)
        null
      else if (defaultUri.toString.equals(selectedUri))
        Settings.System.DEFAULT_RINGTONE_URI
      else
        Uri.parse(selectedUri)
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri)
    context.asInstanceOf[PreferencesActivity].startActivityForResult(intent, resultId)
  }
}

case class OptionsBackStackKey(args: Bundle = new Bundle()) extends BackStackKey(args) {
  override def nameId: Int = R.string.pref_options_screen_title

  override def layoutId = R.layout.preferences_options

  var controller = Option.empty[OptionsViewController]

  override def onViewAttached(v: View) = {
    controller = Option(v.asInstanceOf[OptionsViewImpl]).map(ov => new OptionsViewController(ov)(ov.injector, ov))
  }

  override def onViewDetached() = {
    controller = None
  }
}

class OptionsViewController(view: OptionsView)(implicit inj: Injector, ec: EventContext) extends Injectable {
  val zms = inject[Signal[ZMessaging]]
  val prefs = zms.map(_.prefs)

  prefs.flatMap(_.preference(GlobalPreferences.DownloadImages).signal).on(Threading.Ui){ view.setDownloadPictures }

  prefs.flatMap(_.preference(GlobalPreferences.RingTone).signal).on(Threading.Ui){ view.setRingtone }
  prefs.flatMap(_.preference(GlobalPreferences.TextTone).signal).on(Threading.Ui){ view.setTextTone }
  prefs.flatMap(_.preference(GlobalPreferences.PingTone).signal).on(Threading.Ui){ view.setPingTone }
}
