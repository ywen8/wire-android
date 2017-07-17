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
package com.waz.zclient.core.stores.appentry;

import com.waz.zclient.core.controllers.tracking.events.Event;

public interface AppEntryStateCallback {
    void onShowPhoneInvitationPage();
    void onShowEmailInvitationPage();
    void onInvitationFailed();
    void onInvitationSuccess();
    void onShowPhoneRegistrationPage();
    void onShowPhoneSignInPage();
    void onShowPhoneCodePage();
    void onShowPhoneAddEmailPage();
    void onShowPhoneVerifyEmailPage();
    void onShowPhoneNamePage();
    void onEnterApplication(boolean openSettings);
    void onShowPhoneSetPicturePage();
    void onShowEmailWelcomePage();
    void onShowEmailRegistrationPage();
    void onShowEmailVerifyEmailPage();
    void onShowEmailSignInPage();
    void onShowEmailSetPicturePage();
    void onShowEmailAddPhonePage();
    void onShowEmailPhoneCodePage();
    void onShowFirstLaunchPage();
    void tagAppEntryEvent(Event event);
}
