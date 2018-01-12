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

import android.app.{AlertDialog, Dialog}
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.{AppCompatCheckBox, LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup, WindowManager}
import android.widget.CompoundButton
import com.waz.ZLog
import com.waz.model.EmailAddress
import com.waz.utils.events._
import com.waz.zclient.appentry.controllers.{ContactsController, InvitationsController}
import com.waz.zclient.appentry.fragments.AddressBookAdapter.EmailViewHolder
import com.waz.zclient.appentry.fragments.AddressBookPickDialog._
import com.waz.zclient.appentry.fragments.CountryDialogFragment.Container
import com.waz.zclient.common.views.{PickableElement, PickerSpannableEditText}
import com.waz.zclient.pages.BaseDialogFragment
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.usersearch.views.SearchEditText
import com.waz.zclient.{FragmentHelper, R}

import scala.collection.GenSeq
import scala.concurrent.Future

class AddressBookPickDialog extends BaseDialogFragment[Container] with FragmentHelper {

  private lazy val contactsController = inject[ContactsController]
  private lazy val invitationsController = inject[InvitationsController]

  private lazy val adapter = new AddressBookAdapter()
  private lazy val filter = Signal("")
  private lazy val emails = for {
    invitations <- invitationsController.invitations
    contacts <- contactsController.contacts
    filter <- filter.map(_.toLowerCase)
  } yield contacts
      .filter(c => c.emailAddresses.nonEmpty && !c.emailAddresses.exists(e => invitations.keySet.contains(e)))
      .flatMap(c => c.emailAddresses.map(e => Entry(c.name, e)))
      .filter(e => e.name.toLowerCase.contains(filter) || e.email.str.toLowerCase.contains(filter))

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val view = LayoutInflater.from(getActivity).inflate(R.layout.address_book_pick_layout, null)
    val recyclerView = findById[RecyclerView](view, R.id.recycler_view)
    val searchEditText = findById[SearchEditText](view, R.id.search_box)

    recyclerView.setLayoutManager(new LinearLayoutManager(getContext))
    recyclerView.setAdapter(adapter)

    emails.onUi { adapter.setData }

    adapter.selectedEntries.onUi { entries =>
      searchEditText.removeAllElements()
      entries.foreach { element =>
        searchEditText.addElement(PickableEntry(element))
      }
    }

    searchEditText.setCallback(new PickerSpannableEditText.Callback {
      override def afterTextChanged(s: String): Unit =
        filter ! s
      override def onRemovedTokenSpan(element: PickableElement): Unit =
        adapter.onCheckBoxChanged ! (element.asInstanceOf[PickableEntry].entry, false)
    })

    new AlertDialog.Builder(getActivity).setView(view).setCancelable(true).create
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_Dark_Preferences)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    getDialog.getWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    super.onViewCreated(view, savedInstanceState)
  }
}

object AddressBookPickDialog {
  val Tag = ZLog.ImplicitTag.implicitLogTag

  case class Entry(name: String, email: EmailAddress)
  case class PickableEntry(entry: Entry) extends PickableElement{
    override def id: String = entry.email.str
    override def name: String = entry.name
  }
}

class AddressBookAdapter(implicit eventContext: EventContext) extends RecyclerView.Adapter[EmailViewHolder] {

  private var data = GenSeq.empty[Entry]
  val onCheckBoxChanged = EventStream[(Entry, Boolean)]()
  private var _selectedEntries =  Set.empty[Entry]

  val selectedEntries = new AggregatingSignal[(Entry, Boolean), Set[Entry]](onCheckBoxChanged, Future.successful(_selectedEntries), {
    case (set, (entry, true)) => set + entry
    case (set, (entry, false)) => set - entry
  })

  setHasStableIds(true)
  selectedEntries.onUi { selectedEntries =>
    _selectedEntries = selectedEntries
    notifyDataSetChanged()
  }

  def setData(data: GenSeq[Entry]): Unit = {
    this.data = data
    notifyDataSetChanged()
  }

  override def getItemCount: Int = data.size

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailViewHolder =
    EmailViewHolder(LayoutInflater.from(parent.getContext).inflate(R.layout.email_address_layout, parent, false))

  override def onBindViewHolder(holder: EmailViewHolder, position: Int): Unit =
    holder.bind(data(position), _selectedEntries.contains(data(position)), onCheckBoxChanged)

  override def getItemId(position: Int): Long = data(position).email.str.hashCode
}

object AddressBookAdapter {
  case class EmailViewHolder(v: View) extends RecyclerView.ViewHolder(v) {

    private lazy val checkBox = v.findViewById[AppCompatCheckBox](R.id.check_box)
    private lazy val nameView = v.findViewById[TypefaceTextView](R.id.name_text)
    private lazy val emailView = v.findViewById[TypefaceTextView](R.id.email_text)

    def bind(entry: Entry, checked: Boolean, checkEventStream: SourceStream[(Entry, Boolean)]): Unit = {
      nameView.setText(entry.name)
      emailView.setText(entry.email.str)
      checkBox.setOnCheckedChangeListener(null)
      checkBox.setChecked(checked)
      checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener {
        override def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = {
          checkEventStream ! (entry, isChecked)
        }
      })
    }
  }
}
