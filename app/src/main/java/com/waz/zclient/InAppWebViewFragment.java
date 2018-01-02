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
package com.waz.zclient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.utils.ViewUtils;

public class InAppWebViewFragment extends BaseFragment<InAppWebViewFragment.Container> {
    public static final String TAG = InAppWebViewFragment.class.getName();
    public static final String ARG_URL_TO_BE_OPENED = "ARG_URL_TO_BE_OPENED";
    public static final String ARG_WITH_CLOSE_BUTTON = "ARG_WITH_CLOSE_BUTTON";

    private WebView webView;

    public static InAppWebViewFragment newInstance(String url, boolean withCloseButton) {
        InAppWebViewFragment fragment = new InAppWebViewFragment();

        Bundle bundle = new Bundle();
        bundle.putString(ARG_URL_TO_BE_OPENED, url);
        bundle.putBoolean(ARG_WITH_CLOSE_BUTTON, withCloseButton);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_in_app_web_view, viewGroup, false);
        webView = ViewUtils.getView(view, R.id.wv__inapp);
        if (getArguments() == null || !getArguments().containsKey(ARG_URL_TO_BE_OPENED)) {
            throw new RuntimeException("InAppWebViewFragment can only be opened via newInstance(url");
        }
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(getArguments().getString(ARG_URL_TO_BE_OPENED));

        View closeButton = ViewUtils.getView(view, R.id.gtv__inapp__close);
        if (getArguments().getBoolean(ARG_WITH_CLOSE_BUTTON)) {
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getContainer().dismissInAppWebView();
                }
            });
        } else {
            closeButton.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        webView = null;
        super.onDestroyView();
    }

    public interface Container {
        void dismissInAppWebView();
    }
}
