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
package com.waz.zclient.pages.main.pickuser;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.waz.api.IConversation;
import com.waz.api.User;
import com.waz.zclient.pages.main.pickuser.views.ConversationRowView;
import com.waz.zclient.pages.main.pickuser.views.UserRowView;

public class SearchResultOnItemTouchListener implements RecyclerView.OnItemTouchListener {
    private Callback callback;
    private GestureDetector gestureDetector;
    private int position = -1;
    private View rowView;

    public SearchResultOnItemTouchListener(Context context, Callback listener) {
        callback = listener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (rowView instanceof UserRowView) {
                    callback.onUserDoubleClicked(((UserRowView) rowView).getUser(), position, rowView);
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (rowView instanceof UserRowView) {
                    ((UserRowView) rowView).onClicked();
                    callback.onUserClicked(((UserRowView) rowView).getUser(), position, rowView);
                }
                if (rowView instanceof ConversationRowView) {
                    callback.onConversationClicked(((ConversationRowView) rowView).getConversation(), position);
                }
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        rowView = rv.findChildViewUnder(e.getX(), e.getY());
        position = rv.getChildAdapterPosition(rowView);
        if (rowView instanceof RecyclerView) {
            return false;
        }

        position = rv.getChildAdapterPosition(rowView);
        if (rowView != null && callback != null) {
            gestureDetector.onTouchEvent(e);
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public interface Callback {
        void onUserClicked(User user, int position, View anchorView);

        void onConversationClicked(IConversation conversation, int position);

        void onUserDoubleClicked(User user, int position, View anchorView);
    }
}
