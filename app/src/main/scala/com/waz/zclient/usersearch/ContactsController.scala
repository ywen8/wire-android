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
package com.waz.zclient.usersearch

import java.util.Locale

import com.waz.model
import com.waz.ZLog.ImplicitTag._
import com.waz.model.{Contact, EmailAddress, PhoneNumber}
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, Signal}
import com.waz.zclient.usersearch.ContactsController._
import com.waz.zclient.{Injectable, Injector}

import scala.collection.GenSeq
import scala.concurrent.Future

class ContactsController()(implicit injector: Injector, ec: EventContext) extends Injectable {
  import Threading.Implicits.Background

  private val zms = inject[Signal[ZMessaging]]

  def contacts(filter: String): Signal[GenSeq[ContactDetails]] = for {
    zms <- zms
    invited <- zms.invitations.invitedContacts
    contacts <- zms.contacts.unifiedContacts()
    onWire <- zms.contacts.contactsOnWire
  } yield contacts.contacts.values.toSeq.map(c => ContactDetails(c, invited = invited.contains(c.id)))
    .filterNot(c => onWire.containsRight(c.contact.id))
    .filter(c => filter.isEmpty || c.contact.name.toLowerCase.contains(filter.toLowerCase))//TODO: proper filter

  val contacts: Signal[GenSeq[ContactDetails]] = for {
    zms <- zms
    invited <- zms.invitations.invitedContacts
    contacts <- zms.contacts.unifiedContacts()
    onWire <- zms.contacts.contactsOnWire
  } yield contacts.contacts.values.toSeq.map(c => ContactDetails(c, invited = invited.contains(c.id))).filterNot(c => onWire.containsRight(c.contact.id))

  def invite(contactMethod: ContactMethod, message: String, locale: Locale): Future[Unit] =
    zms.head.flatMap(_.invitations.invite(contactMethod.contact.id, contactMethod.method, contactMethod.contact.name, message, Option(locale)))

}

object ContactsController {
  object ContactMethod {
    trait ContactType
    object Email extends ContactType
    object Phone extends ContactType
  }

  case class ContactMethod(contact: model.Contact, method: Either[EmailAddress, PhoneNumber]) {
    import ContactMethod._
    def stringRepresentation: String = method.fold(_.str, _.str)
    def getType: ContactType = method.fold(_ => Email, _ => Phone)
  }

  case class ContactDetails(contact: Contact, invited: Boolean) {
    def contactMethods =
      contact.emailAddresses.map(e => ContactMethod(contact, Left(e)))
        .++(contact.phoneNumbers.map(p => ContactMethod(contact, Right(p)))).toSeq
  }
}
