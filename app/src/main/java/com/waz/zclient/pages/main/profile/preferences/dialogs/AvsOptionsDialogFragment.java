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
package com.waz.zclient.pages.main.profile.preferences.dialogs;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.waz.zclient.R;
import com.waz.zclient.pages.BaseDialogFragment;
import com.waz.zclient.utils.ViewUtils;

public class AvsOptionsDialogFragment extends BaseDialogFragment<AvsOptionsDialogFragment.Container> {
    public static final String TAG = AvsOptionsDialogFragment.class.getName();

    private TextView loggingTextView;
    private TextView postSessionIdTextView;
    private TextView lastSessionIdTextView;

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  LifeCycle
    //
    //////////////////////////////////////////////////////////////////////////////////////////


    public static AvsOptionsDialogFragment newInstance() {
        AvsOptionsDialogFragment fragment = new AvsOptionsDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Theme_Dark_Preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vcontainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_avs_options, vcontainer, false);

        loggingTextView = ViewUtils.getView(view, R.id.avs__logging);
        loggingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //boolean updatedValue = !getStoreFactory().getZMessagingApiStore().getAvs().isLoggingEnabled();
                //getStoreFactory().getZMessagingApiStore().getAvs().setLoggingEnabled(updatedValue);
                //updateButton(loggingTextView, updatedValue);
            }
        });
        postSessionIdTextView = ViewUtils.getView(view, R.id.avs__post_session_id);
        postSessionIdTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean updatedValue = !getControllerFactory().getUserPreferencesController().isPostSessionIdToConversation();
                getControllerFactory().getUserPreferencesController().setPostSessionIdToConversation(updatedValue);
                updateButton(postSessionIdTextView, updatedValue);
            }
        });
        lastSessionIdTextView = ViewUtils.getView(view, R.id.avs__last_session_id);
        final String lastCallSessionId = getControllerFactory().getUserPreferencesController().getLastCallSessionId();
        lastSessionIdTextView.setText(lastCallSessionId);
        lastSessionIdTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.pref_dev_avs_last_call_session_id_title),
                                                      lastCallSessionId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(),
                               getString(R.string.pref_dev_avs_last_call_session_id_copied_to_clipboard),
                               Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //updateButton(loggingTextView, getStoreFactory().getZMessagingApiStore().getAvs().isLoggingEnabled());
        updateButton(postSessionIdTextView, getControllerFactory().getUserPreferencesController().isPostSessionIdToConversation());
    }

    @Override
    public void onDestroyView() {
        loggingTextView = null;
        postSessionIdTextView = null;
        lastSessionIdTextView = null;
        super.onDestroyView();
    }

    @SuppressLint("SetTextI18n")
    private void updateButton(TextView button, boolean enabled) {
        if (button == null) {
            return;
        }
        // These strings are only for internal usage and should not be translated
        // as they are only visible to devs
        if (enabled) {
            button.setText("Enabled");
        } else {
            button.setText("Disabled");
        }
    }

    public interface Container {
    }
}
