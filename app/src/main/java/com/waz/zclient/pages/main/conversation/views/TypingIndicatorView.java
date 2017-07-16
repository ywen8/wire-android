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
package com.waz.zclient.pages.main.conversation.views;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.waz.api.InputStateIndicator;
import com.waz.api.UsersList;
import com.waz.zclient.R;
import com.waz.zclient.core.api.scala.ModelObserver;
import com.waz.zclient.utils.ViewUtils;

import java.util.LinkedList;

public class TypingIndicatorView extends FrameLayout {

    private TextView nameTextView;
    private View dotsView;
    private View penView;

    private boolean animationRunning;
    private Handler handler;

    private ModelObserver<UsersList> typingUsersObserver = new ModelObserver<UsersList>() {
        @Override
        public void updated(UsersList usersList) {
            if (usersList == null || usersList.size() == 0) {
                nameTextView.setText("");
                setVisibility(GONE);
                endAnimation();
            } else {
                LinkedList<String> userNames = new LinkedList<>();
                for (int i = 0; i < usersList.size(); i++) {
                    userNames.add(usersList.get(i).getDisplayName());
                }
                nameTextView.setText(TextUtils.join(", ", userNames));
                setVisibility(VISIBLE);
                startAnimation();
            }
        }
    };

    public TypingIndicatorView(Context context) {
        this(context, null);
    }

    public TypingIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TypingIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.typing_indicator, this, true);
        nameTextView = ViewUtils.getView(this, R.id.ttv__typing_indicator_names);
        dotsView = ViewUtils.getView(this, R.id.gtv__is_typing_dots);
        penView = ViewUtils.getView(this, R.id.gtv__is_typing_pen);
        handler = new Handler();
    }

    private void startAnimation() {
        if (animationRunning) {
            return;
        }
        animationRunning = true;
        runAnimation();
    }

    private void runAnimation() {
        if (!animationRunning) {
            return;
        }
        final int stepDuration = getResources().getInteger(R.integer.animation_duration_medium_rare);
        int step1 = dotsView.getWidth() / 3;
        final int step2 = step1 * 2;
        final int step3 = dotsView.getWidth();

        penView.animate().translationX(step1).setDuration(stepDuration).start();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                penView.animate().translationX(step2).setDuration(stepDuration).start();
            }
        }, stepDuration * 2);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                penView.animate().translationX(step3).setDuration(stepDuration).start();
            }
        }, stepDuration * 4);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runAnimation();
            }
        }, stepDuration * 8);
    }

    private void endAnimation() {
        if (!animationRunning) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
        animationRunning = false;

    }

    public void setInputStateIndicator(InputStateIndicator inputStateIndicator) {
        if (inputStateIndicator == null) {
            typingUsersObserver.clear();
        } else {
            typingUsersObserver.setAndUpdate(inputStateIndicator.getTypingUsers());
        }
    }

    public void clear() {
        typingUsersObserver.clear();
        nameTextView.setText("");
        endAnimation();
    }
}
