/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
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
package com.waz.zclient.pages.main.sharing;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.waz.api.IConversation;
import com.waz.utils.wrappers.URI;
import com.waz.zclient.R;
import com.waz.zclient.controllers.accentcolor.AccentColorObserver;
import com.waz.zclient.controllers.navigation.Page;
import com.waz.zclient.controllers.sharing.SharedContentType;
import com.waz.zclient.controllers.sharing.SharingObserver;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.pages.main.conversationlist.ConversationListFragment;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.views.LoadingIndicatorView;

import java.util.List;

public class SharingConversationListManagerFragment extends BaseFragment<SharingConversationListManagerFragment.Container> implements AccentColorObserver,
                                                                                                                                      ImageSharingPreviewFragment.Container,
                                                                                                                                      ConversationListFragment.Container,
                                                                                                                                      SharingObserver {
    public static final String TAG = SharingConversationListManagerFragment.class.getName();

    private LoadingIndicatorView loadingIndicatorView;
    private SharingIndicatorView sharingIndicatorView;

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  Lifecycle
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    public static Fragment newInstance() {
        return new SharingConversationListManagerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation_list_manager, viewGroup, false);

        loadingIndicatorView = ViewUtils.getView(view, R.id.liv__conversations__loading_indicator);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //noinspection deprecation
            loadingIndicatorView.setColor(getResources().getColor(R.color.people_picker__loading__color));
        } else {
            loadingIndicatorView.setColor(getResources().getColor(R.color.people_picker__loading__color, getContext().getTheme()));
        }
        sharingIndicatorView = ViewUtils.getView(view, R.id.siv__sharing_indicator);

        getControllerFactory().getNavigationController().setLeftPage(Page.CONVERSATION_LIST, TAG);

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getChildFragmentManager();
            fragmentManager.beginTransaction()
                           .add(R.id.fl__conversation_list_main,
                                ConversationListFragment.newInstance(ConversationListFragment.Mode.SHARING),
                                ConversationListFragment.TAG)
                           .commit();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        getControllerFactory().getSharingController().addObserver(this);
        getControllerFactory().getAccentColorController().addAccentColorObserver(this);
    }

    @Override
    public void onStop() {
        getControllerFactory().getSharingController().removeObserver(this);
        getControllerFactory().getAccentColorController().removeAccentColorObserver(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        loadingIndicatorView = null;
        sharingIndicatorView = null;
        super.onDestroyView();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  AccentColorObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAccentColorHasChanged(Object sender, int color) {
        if (sharingIndicatorView != null) {
            sharingIndicatorView.setAccentColor(color);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  ConversationStoreObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onDestinationSelected(final IConversation conversation) {
        SharedContentType sharedContentType = getControllerFactory().getSharingController().getSharedContentType();
        if (sharedContentType == null) {
            return;
        }

        switch (sharedContentType) {
            case TEXT:
                String sharedText = getControllerFactory().getSharingController().getSharedText();
                getControllerFactory().getSharingController().onContentShared(getActivity(), conversation, sharedText);
                getActivity().finish();
                break;
            case IMAGE:
                sharingIndicatorView.setVisibility(View.GONE);
                getChildFragmentManager().beginTransaction()
                                         .setCustomAnimations(R.anim.fade_in,
                                                              R.anim.fade_out)
                                         .add(R.id.fl__conversation_list__sharing_preview,
                                              ImageSharingPreviewFragment.newInstance(),
                                              ImageSharingPreviewFragment.TAG)
                                         .commit();

                break;
            case FILE:
                final List<URI> sharedFileUris = getControllerFactory().getSharingController().getSharedFileUris();
                new AlertDialog.Builder(getActivity())
                               .setMessage(getResources().getQuantityString(R.plurals.sharing__files__message, sharedFileUris.size(), sharedFileUris.size(), conversation.getName()))
                               .setPositiveButton(R.string.sharing__files__ok, new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog, int which) {
                                       getControllerFactory().getSharingController().onContentShared(getActivity(), conversation, sharedFileUris);
                                       getActivity().finish();
                                   }
                               })
                               .setNegativeButton(R.string.sharing__files__cancel, null)
                               .setCancelable(true)
                               .create()
                               .show();
                break;
        }
    }

    public interface Container {
    }
}
