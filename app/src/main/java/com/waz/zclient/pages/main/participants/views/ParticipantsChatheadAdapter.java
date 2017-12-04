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
package com.waz.zclient.pages.main.participants.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import com.waz.api.CoreList;
import com.waz.api.UpdateListener;
import com.waz.api.User;
import com.waz.model.UserId;
import com.waz.zclient.R;
import com.waz.zclient.common.views.ChatheadWithTextFooter;

import java.util.ArrayList;
import java.util.List;

public class ParticipantsChatheadAdapter extends BaseAdapter implements UpdateListener {
    private static final int VIEW_TYPE_COUNT = 3;
    public static final int VIEW_TYPE_CHATHEAD = 0;
    public static final int VIEW_TYPE_SEPARATOR = 1;
    public static final int VIEW_TYPE_EMPTY = 2;

    private CoreList<User> usersList;
    private int numOfColumns;

    private List<User> userListVerified = new ArrayList<>();
    private List<User> userListUnverified = new ArrayList<>();

    private int separatorPos;

    public ParticipantsChatheadAdapter() {
        separatorPos = -1;
    }

    public void setUsersList(CoreList<User> usersList, int numOfColumns) {
        this.numOfColumns = numOfColumns;

        if (this.usersList != null) {
            this.usersList.removeUpdateListener(this);
        }


        this.usersList = usersList;
        if (this.usersList != null) {
            this.usersList.addUpdateListener(this);
            for (User user : usersList) {
                user.addUpdateListener(this);
            }
        }
        updated();
    }

    public void tearDown() {
        if (usersList != null) {
            for (User user : usersList) {
                user.removeUpdateListener(this);
            }
            usersList.removeUpdateListener(this);
        }
    }

    @Override
    public int getCount() {
        // Hack to make it overscrollable
        if (userListUnverified.isEmpty() && userListVerified.isEmpty()) {
            return 1;
        }

        int count = userListVerified.size() + userListUnverified.size();

        if (separatorPos != -1) {
            count += numOfColumns;
        }

        return count;
    }

    @Override
    public User getItem(int position) {

        if (position < userListUnverified.size()) {
            return userListUnverified.get(position);
        }

        int unverifiedSize = position - userListUnverified.size();

        if (separatorPos != -1) {
            if (position < separatorPos + numOfColumns) {
                return null;
            }

            unverifiedSize -= numOfColumns;
        }
        if (userListVerified.size() <= unverifiedSize) {
            return null;
        }

        return userListVerified.get(unverifiedSize);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < userListUnverified.size()) {
            return VIEW_TYPE_CHATHEAD;
        }

        if (position == separatorPos) {
            return VIEW_TYPE_SEPARATOR;
        }

        if (position < separatorPos + numOfColumns) {
            return VIEW_TYPE_EMPTY;
        }

        return VIEW_TYPE_CHATHEAD;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_CHATHEAD:
                return getChatheadLabel(position, convertView, parent);
            case VIEW_TYPE_SEPARATOR:
                return getSeparatorView(parent);
            default:
            case VIEW_TYPE_EMPTY:
                View view = new View(parent.getContext());
                view.setLayoutParams(new AbsListView.LayoutParams(0,
                        parent.getResources().getDimensionPixelSize(R.dimen.participants__verified_row__height)));
                view.setVisibility(View.GONE);
                return view;

        }
    }

    private View getChatheadLabel(int position, View convertView, ViewGroup parent) {
        ChatheadWithTextFooter view;
        if (convertView == null) {
            view = new ChatheadWithTextFooter(parent.getContext());
        } else {
            view = (ChatheadWithTextFooter) convertView;
        }
        User user = getItem(position);
        if (user != null) {
            view.setUserId(new UserId(user.getId()));
            view.setVisibility(View.VISIBLE);
        } else {
            //TODO https://wearezeta.atlassian.net/browse/AN-4276
            view.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    public View getSeparatorView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.participants_separator_row, parent, false);
        view.setLayoutParams(new AbsListView.LayoutParams(parent.getMeasuredWidth(),
                parent.getResources().getDimensionPixelSize(R.dimen.participants__verified_row__height)));
        return view;
    }

    @Override
    public void updated() {
        userListVerified.clear();
        userListUnverified.clear();
        if (usersList != null) {
            for (User user : usersList) {
                switch (user.getVerified()) {
                    case VERIFIED:
                        userListVerified.add(user);
                        break;
                    default:
                        userListUnverified.add(user);
                        break;
                }
            }
        }

        // fill up with empty spaces
        if (userListUnverified.size() > 0) {
            int rest = userListUnverified.size() % numOfColumns;
            if (rest != 0) {
                int fillupCount = numOfColumns - rest;
                for (int i = 0; i < fillupCount; i++) {
                    userListUnverified.add(null);
                }
            }
        }

        if (!userListVerified.isEmpty()) {
            separatorPos = userListUnverified.size();
        } else {
            separatorPos = -1;
        }

        notifyDataSetChanged();
    }
}
