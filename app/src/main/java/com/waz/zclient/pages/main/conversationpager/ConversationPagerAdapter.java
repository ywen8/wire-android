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
package com.waz.zclient.pages.main.conversationpager;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;
import com.waz.zclient.utils.LayoutSpec;
import com.waz.zclient.utils.BuildConfigUtils;

import java.util.HashMap;
import java.util.Map;

public class ConversationPagerAdapter extends FragmentPagerAdapter {

    private final FragmentManager fragmentManager;
    private Context context;
    private LayoutSpec layoutSpec;
    private float firsElementPercentage;
    private Map<Integer, String> fragmentTags;

    public ConversationPagerAdapter(Context context, FragmentManager fm, LayoutSpec layoutSpec, float firsElementPercentage) {
        super(fm);
        this.context = context;
        this.fragmentManager = fm;
        this.layoutSpec = layoutSpec;
        this.firsElementPercentage = firsElementPercentage;
        fragmentTags = new HashMap<>();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case  0: return FirstPageFragment.newInstance();
            case  1: return SecondPageFragment.newInstance();
        }

        return null;
    }

    /**
     * Overriding this method to save the tags of the created fragments, to be retrieved by the position later.
     *
     * @param container
     * @param position
     * @return
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object obj = super.instantiateItem(container, position);
        if (obj instanceof Fragment) {
            Fragment f = (Fragment) obj;
            String tag = f.getTag();
            fragmentTags.put(position, tag);
        }
        return obj;
    }

    @Override
    public float getPageWidth(int position) {
        if (!isPhone() && position == 0) {
            return firsElementPercentage;
        }
        return super.getPageWidth(position);
    }

    private boolean isPhone() {
        // TODO: remove the if block if tablet version is ready
        if (BuildConfigUtils.isLocalBuild(context)) {
            return layoutSpec.equals(LayoutSpec.LAYOUT_PHONE);
        }
        return true;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return 2;
    }

    public Fragment getFragment(int position) {
        String tag = fragmentTags.get(position);
        if (tag == null) {
            return null;
        }
        return fragmentManager.findFragmentByTag(tag);
    }
}
