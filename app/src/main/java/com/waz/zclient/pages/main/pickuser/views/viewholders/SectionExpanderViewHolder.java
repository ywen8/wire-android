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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.waz.zclient.R;
import com.waz.zclient.ui.text.TypefaceTextView;
import com.waz.zclient.utils.ViewUtils;

public class SectionExpanderViewHolder extends RecyclerView.ViewHolder {
    private final TypefaceTextView viewAllTextView;

    public SectionExpanderViewHolder(View itemView) {
        super(itemView);
        viewAllTextView = ViewUtils.getView(itemView, R.id.ttv_startui_section_header);
    }

    public void bind(int itemCount, View.OnClickListener clickListener) {
        String title = viewAllTextView.getContext().getResources().getString(R.string.people_picker__search_result__expander_title, Integer.toString(itemCount));
        viewAllTextView.setText(title);
        viewAllTextView.setOnClickListener(clickListener);
    }
}
