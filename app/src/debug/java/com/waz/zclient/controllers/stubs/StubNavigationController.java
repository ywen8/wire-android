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
package com.waz.zclient.controllers.stubs;

import android.os.Bundle;
import com.waz.zclient.controllers.navigation.INavigationController;
import com.waz.zclient.controllers.navigation.NavigationControllerObserver;
import com.waz.zclient.controllers.navigation.Page;
import com.waz.zclient.controllers.navigation.PagerControllerObserver;

public class StubNavigationController implements INavigationController {
  @Override
  public boolean isPagerEnabled() {
    return false;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {

  }

  @Override
  public void setPagerEnabled(boolean enabled) {

  }

  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {

  }

  @Override
  public void removeNavigationControllerObserver(NavigationControllerObserver navigationControllerObserver) {

  }

  @Override
  public void setVisiblePage(Page page, String sender) {

  }

  @Override
  public int getPagerPosition() {
    return 0;
  }

  @Override
  public Page getCurrentPage() {
    return null;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {

  }

  @Override
  public void setPagerPosition(int position) {

  }

  @Override
  public void addPagerControllerObserver(PagerControllerObserver pagerControllerObserver) {

  }

  @Override
  public void removePagerControllerObserver(PagerControllerObserver pagerControllerObserver) {

  }

  @Override
  public void setPagerSettingForPage(Page page) {

  }

  @Override
  public void setIsLandscape(boolean isLandscape) {

  }

  @Override
  public void onPageScrollStateChanged(int arg0) {

  }

  @Override
  public void resetPagerPositionToDefault() {

  }

  @Override
  public void setRightPage(Page leftPage, String sender) {

  }

  @Override
  public void addNavigationControllerObserver(NavigationControllerObserver navigationControllerObserver) {

  }

  @Override
  public Page getCurrentLeftPage() {
    return null;
  }

  @Override
  public void tearDown() {

  }

  @Override
  public void setLeftPage(Page leftPage, String sender) {

  }

  @Override
  public Page getCurrentRightPage() {
    return null;
  }

  @Override
  public void onPageSelected(int arg0) {

  }
}
