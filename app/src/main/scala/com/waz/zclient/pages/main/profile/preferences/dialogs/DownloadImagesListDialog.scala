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
package com.waz.zclient.pages.main.profile.preferences.dialogs
import android.os.Bundle
import com.waz.content.GlobalPreferences
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.zclient.R
import DownloadImagesListDialog._

class DownloadImagesListDialog extends PreferenceListDialog{

  protected lazy val zms = inject[Signal[ZMessaging]]

  override protected lazy val title: String = getString(R.string.pref_options_image_download_title)
  override protected lazy val keys: Array[String] = getResources.getStringArray(R.array.zms_image_download_values)
  override protected lazy val names: Array[String] = getResources.getStringArray(R.array.pref_options_image_download_entries)
  override protected lazy val defaultValue: Int = Option(getArguments.getString(DefaultValueArg)).fold(0)(keys.indexOf)

  protected lazy val prefKey = GlobalPreferences.DownloadImages

  override protected def updatePref(value: String) = {
    zms.map(_.prefs.preference(prefKey)).head.map(_.update(value))(Threading.Ui)
  }
}

object DownloadImagesListDialog {
  val Tag = DownloadImagesListDialog.getClass.getSimpleName

  protected val DefaultValueArg = "DefaultValueArg"

  def apply(default: String): DownloadImagesListDialog = {
    returning(new DownloadImagesListDialog()){ fragment =>
      val bundle = new Bundle()
      bundle.putString(DefaultValueArg, default)
      fragment.setArguments(bundle)
    }
  }
}
