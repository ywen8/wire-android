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
package com.waz.zclient.pages.main.pickuser.views.viewholders;


import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.waz.zclient.R;
import com.waz.zclient.utils.ViewUtils;

public class AddressBookSectionHeaderViewHolder extends RecyclerView.ViewHolder {
    private TextView label;

    public AddressBookSectionHeaderViewHolder(View itemView, boolean darkTheme) {
        super(itemView);
        label = ViewUtils.getView(itemView, R.id.ttv_startui_section_header);
        if (darkTheme) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //noinspection deprecation
                label.setTextColor(itemView.getResources().getColor(R.color.text__primary_dark));
            } else {
                label.setTextColor(itemView.getResources().getColor(R.color.text__primary_dark, label.getContext().getTheme()));
            }
        }
    }

    public void bind(String labelText) {
        label.setText(labelText);
    }
}
