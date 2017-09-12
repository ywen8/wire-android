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
package com.waz.zclient.controllers.stubs;

import com.waz.api.IConversation;
import com.waz.utils.wrappers.URI;
import com.waz.zclient.controllers.sharing.ISharingController;
import com.waz.zclient.controllers.sharing.SharedContentType;

import java.util.List;

public class StubSharingController implements ISharingController {
  @Override
  public void setSharedUris(List<URI> imageUris) {

  }

  @Override
  public List<URI> getSharedFileUris() {
    return null;
  }

  @Override
  public void maybeResetSharedText(IConversation currentConversation) {

  }

  @Override
  public void maybeResetSharedUris(IConversation currentConversation) {

  }

  @Override
  public void setSharingConversationId(String conversationId) {

  }

  @Override
  public String getSharedText() {
    return null;
  }

  @Override
  public void setSharedContentType(SharedContentType type) {

  }

  @Override
  public void tearDown() {

  }

  @Override
  public boolean isSharedConversation(IConversation conversation) {
    return false;
  }

  @Override
  public void setSharedText(String text) {

  }
}
