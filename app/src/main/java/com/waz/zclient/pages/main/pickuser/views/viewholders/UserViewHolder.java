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
import com.waz.api.User;
import com.waz.zclient.R;
import com.waz.zclient.pages.main.pickuser.views.SearchResultUserRowView;
import com.waz.zclient.utils.ViewUtils;

public class UserViewHolder extends RecyclerView.ViewHolder {
    private final SearchResultUserRowView userRow;

    public UserViewHolder(View itemView, boolean darkTheme, boolean showContactInfo) {
        super(itemView);
        userRow =  ViewUtils.getView(itemView, R.id.srurv_startui_user);
        if (darkTheme) {
            userRow.applyDarkTheme();
        }
        userRow.setShowContatctInfo(showContactInfo);
    }

    public void bind(User user, boolean isSelected) {
        userRow.setUser(user);
        userRow.setSelected(isSelected);
    }
}
