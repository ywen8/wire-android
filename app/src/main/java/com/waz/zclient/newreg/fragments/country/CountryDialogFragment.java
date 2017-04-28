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
package com.waz.zclient.newreg.fragments.country;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.waz.zclient.R;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.utils.ViewUtils;

public class CountryDialogFragment extends BaseFragment<CountryDialogFragment.Container> implements AdapterView.OnItemClickListener {
    public static final String TAG = CountryDialogFragment.class.getName();

    private CountryCodeAdapter countryAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_phone__country_dialog, container, false);

        ListView listView = ViewUtils.getView(view, R.id.lv__country_code);

        countryAdapter = new CountryCodeAdapter();

        listView.setAdapter(countryAdapter);
        listView.setOnItemClickListener(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //noinspection deprecation
            listView.setDivider(new ColorDrawable(getResources().getColor(R.color.country_divider_color)));
        } else {
            listView.setDivider(new ColorDrawable(getResources().getColor(R.color.country_divider_color, getContext().getTheme())));
        }
        listView.setDividerHeight(ViewUtils.toPx(getActivity(), 1));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        countryAdapter.setCountryList(getContainer().getCountryController().getSortedCountries());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        getContainer().getCountryController().setCountry(countryAdapter.getItem(i));
        dismiss();
    }

    private void dismiss() {
        getContainer().dismissCountryBox();
    }


    public interface Container {
        CountryController getCountryController();

        void dismissCountryBox();
    }
}
