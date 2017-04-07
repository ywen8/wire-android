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
package com.waz.zclient.views;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.waz.api.User;
import com.waz.zclient.R;
import com.waz.zclient.controllers.mentioning.MentioningObserver;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.ui.utils.MathUtils;
import com.waz.zclient.utils.ViewUtils;

import java.util.List;

public class MentioningFragment extends BaseFragment<MentioningFragment.Container> implements MentioningObserver,
                                                                                              MentioningAdapter.OnItemClickListener {

    public static final String TAG = MentioningFragment.class.getSimpleName();
    private RecyclerView recyclerView;
    private MentioningAdapter mentioningAdapter;
    private View markerView;
    private View contentView;
    private int totalWidth;
    private int maxLeft;
    private int maxRight;
    private boolean visible;

    public static Fragment getInstance() {
        return new MentioningFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.mentioning_view, container, false);
        recyclerView = ViewUtils.getView(contentView, R.id.rv__mentioning__list);
        markerView = ViewUtils.getView(contentView, R.id.iv__mentioning__marker);
        return contentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        visible = true;
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), OrientationHelper.HORIZONTAL, false);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SpacingItemDecorator(getResources().getDimensionPixelSize(R.dimen.mentioning__item__padding_outer)));
        mentioningAdapter = new MentioningAdapter();
        mentioningAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(mentioningAdapter);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //noinspection deprecation
            recyclerView.setBackground(ViewUtils.getRoundedRect(getResources().getDimensionPixelSize(R.dimen.mentioning__popover__height) / 2,
                                                                getResources().getColor(R.color.mentioning__popover__background)));
        } else {
            recyclerView.setBackground(ViewUtils.getRoundedRect(getResources().getDimensionPixelSize(R.dimen.mentioning__popover__height) / 2,
                getResources().getColor(R.color.mentioning__popover__background, getContext().getTheme())));
        }
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null ||
                    getResources() == null ||
                    contentView == null) {
                    return;
                }
                maxLeft = getResources().getDimensionPixelSize(R.dimen.content__separator__avatar_container__width) -
                          getResources().getDimensionPixelSize(R.dimen.content__separator__chathead__size);
                totalWidth = contentView.getMeasuredWidth();
                maxRight = totalWidth - maxLeft;
            }
        });

        hide();
    }

    @Override
    public void onStart() {
        super.onStart();
        getControllerFactory().getMentioningController().addObserver(this);
    }

    @Override
    public void onStop() {
        getControllerFactory().getMentioningController().removeObserver(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mentioningAdapter = null;
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        contentView = null;
        markerView = null;
        super.onDestroyView();
    }

    @Override
    public void onQueryResultChanged(@NonNull List<User> usersList) {
        if (mentioningAdapter == null) {
            return;
        }
        mentioningAdapter.setUsers(usersList);
        if (usersList.size() == 0) {
            hide();
        } else {
            show();
        }
    }

    @Override
    public void onMentionedUserSelected(@NonNull String query, @NonNull User user) {
        hide();
    }

    @Override
    public void onCursorPositionChanged(final float x, final float y) {
        // TODO: Position relative to y as well
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (recyclerView == null) {
                    return;
                }
                final int recyclerViewWidth = recyclerView.getWidth();
                final LinearLayout.LayoutParams recyclerViewLP = (LinearLayout.LayoutParams) recyclerView.getLayoutParams();
                recyclerViewLP.leftMargin = (int) MathUtils.clamp(x - (recyclerViewWidth / 2),
                                                             maxLeft,
                                                             maxRight - recyclerViewWidth);
                recyclerView.setLayoutParams(recyclerViewLP);

                final int markerViewWidth = markerView.getWidth();
                final LinearLayout.LayoutParams markerLP = (LinearLayout.LayoutParams) markerView.getLayoutParams();
                markerLP.leftMargin = (int) (x - (markerViewWidth / 2));
                markerView.setLayoutParams(markerLP);

                final ViewGroup.LayoutParams contentLP = contentView.getLayoutParams();
                if (contentLP instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) contentLP).bottomMargin =
                        getResources().getDimensionPixelSize(R.dimen.cursor__black_box__height);
                    contentView.setLayoutParams(contentLP);
                }

                if (visible && contentView.getVisibility() != View.VISIBLE) {
                    contentView.setVisibility(View.VISIBLE);
                } else if (!visible && contentView.getVisibility() != View.GONE) {
                    contentView.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onItemClick(User user) {
        if (user == null) {
            return;
        }
        getControllerFactory().getMentioningController().completeUser(user);
    }

    private void show() {
        if (contentView == null ||
            (visible && contentView.getVisibility() == View.INVISIBLE) ||
            contentView.getVisibility() == View.VISIBLE) {
            return;
        }
        visible = true;
        contentView.setVisibility(View.INVISIBLE);
    }

    private void hide() {
        if (contentView == null || !visible) {
            return;
        }
        visible = false;
        contentView.setVisibility(View.GONE);
    }

    public interface Container {
    }

    private static class SpacingItemDecorator extends RecyclerView.ItemDecoration {

        private final int space;

        SpacingItemDecorator(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
            final int position = params.getViewLayoutPosition();
            final int itemCount = parent.getAdapter().getItemCount() - 1;
            if (position > 0 && position < itemCount) {
                outRect.left = space;
                outRect.right = space;
            } else if (position == 0) {
                outRect.right = space;
            } else if (position == itemCount) {
                outRect.left = space;
            }
        }
    }
}
