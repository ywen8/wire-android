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
package com.waz.zclient.appentry.controllers

import com.waz.api.ImageAsset
import com.waz.utils.events.EventContext
import com.waz.zclient.newreg.fragments.SignUpPhotoFragment
import com.waz.zclient.newreg.fragments.SignUpPhotoFragment.RegistrationType
import com.waz.zclient.{Injectable, Injector}

class AppEntryController(implicit inj: Injector, eventContext: EventContext) extends Injectable {

  def setPicture(imageAsset: ImageAsset, source: SignUpPhotoFragment.Source, registrationType: RegistrationType): Unit = {
    throw new NotImplementedError("")
//    optZms.head.map {
//      case Some(zms) =>
//        zms.users.updateSelfPicture(imageAsset).map { _ =>
//          if (source != SignUpPhotoFragment.Source.Auto) {
//            val trackingSource = source match {
//              case SignUpPhotoFragment.Source.Unsplash => AddPhotoOnRegistrationEvent.Unsplash
//              case _ => AddPhotoOnRegistrationEvent.Gallery
//            }
//            val trackingRegType = registrationType match {
//              case SignUpPhotoFragment.RegistrationType.Email => Email
//              case SignUpPhotoFragment.RegistrationType.Phone => Phone
//            }
//            uiTracking.onAddPhotoOnRegistration(trackingRegType, trackingSource)
//          }
//        }
//      case _ =>
//    }
  }
}
