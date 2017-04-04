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

import com.waz.zclient.core.R;

public enum AppEntryError {
    EMAIL_EXISTS                    (409, "key-exists",     R.string.new_reg_email_exists_header, R.string.new_reg_email_exists_message),
    EMAIL_INVALID                   (400, "invalid-email",  R.string.new_reg_email_invalid_header, R.string.new_reg_email_invalid_message),
    EMAIL_INVALID_REQUEST           (400, "invalid-request",R.string.new_reg_email_invalid_header, R.string.new_reg_email_invalid_message),
    EMAIL_GENERIC_ERROR             (0,   "",               R.string.new_reg_email_generic_error_header, R.string.new_reg_email_generic_error_message),
    EMAIL_REGISTER_GENERIC_ERROR    (0,   "",               R.string.new_reg_email_register_generic_error_header, R.string.new_reg_email_register_generic_error_message /* something wrong with email, name, or password */),
    EMAIL_INVALID_LOGIN_CREDENTIALS (403, "",               R.string.new_reg_email_invalid_login_credentials_header, R.string.new_reg_email_invalid_login_credentials_message/* invalid email / password combination*/),
    PHONE_EXISTS                    (409, "key-exists",     R.string.new_reg_phone_exists_header, R.string.new_reg_phone_exists_message),
    PHONE_INVALID_FORMAT            (400, "bad-request",    R.string.new_reg_phone_invalid_format_header, R.string.new_reg_phone_invalid_format_message),
    PHONE_INVALID_REGISTRATION_CODE (404, "invalid-code",   R.string.new_reg_phone_invalid_registration_code_header, R.string.new_reg_phone_invalid_registration_code_message/* invalid phone number / code combination when registering with phone*/),
    PHONE_INVALID_ADD_CODE          (404, "invalid-code",   R.string.new_reg_phone_invalid_add_code_header, R.string.new_reg_phone_invalid_add_code_message/* invalid phone number / code combination when adding phone to existing account*/),
    PHONE_INVALID_LOGIN_CODE        (403, "",               R.string.new_reg_phone_invalid_login_code_header, R.string.new_reg_phone_invalid_login_code_message/* invalid phone number / code combination*/),
    PHONE_PENDING_LOGIN             (403, "pending-login",  R.string.new_reg_phone_pending_login_header, R.string.new_reg_phone_pending_login_message/* SMS login (not registration) code was recently sent. try again in 10 min */),
    PHONE_ADD_PASSWORD              (0,   "",               R.string.new_reg_phone_add_password_header, R.string.new_reg_phone_add_password_message /*invalid password specified*/),
    PHONE_REGISTER_GENERIC_ERROR    (0,   "",               R.string.new_reg_phone_generic_error_header, R.string.new_reg_phone_generic_error_message),
    PHONE_ADD_TO_PROFILE_GENERIC_ERROR(0, "",               R.string.profile_phone_generic_error_header, R.string.profile_phone_generic_error_message),
    ADD_TO_PROFILE_GENERIC_ERROR    (0, "",                 R.string.profile_generic_error_header, R.string.profile_generic_error_message),
    TOO_MANY_ATTEMPTS               (429, "",               R.string.new_reg_phone_too_man_attempts_header, R.string.new_reg_phone_too_man_attempts_message /*too many login attempts*/),
    SERVER_CONNECTIVITY_ERROR       (600, "",               R.string.new_reg_server_connectivity_error_header, R.string.new_reg_server_connectivity_error_message),
    PHONE_BUDGET_EXHAUSTED          (403, "phone-budget-exhausted", R.string.new_reg_phone_budget_exhausted_title, R.string.new_reg_phone_budget_exhausted_message),
    LOGIN_GENERIC_ERROR             (0,   "",               R.string.new_sign_in_generic_error_header, R.string.new_sign_in_generic_error_message),

    NO_INTERNET                     (598, "",               R.string.new_reg_internet_connectivity_error_header, R.string.new_reg_internet_connectivity_error_message),
    PHONE_INVALID                   (400, "invalid-phone",  -1, -1), /* not displayed to user */
    FORBIDDEN                       (403, "",               -1, -1), /* not displayed to user */;

    public final int errorCode;
    public final String label;
    public final int headerResource;
    public final int messageResource;

    AppEntryError(int errorCode, String label, int headerResource, int messageResource) {
        this.errorCode = errorCode;
        this.label = label;
        this.headerResource = headerResource;
        this.messageResource = messageResource;
    }

    public boolean correspondsTo(int errorCode, String label) {
        return this.errorCode == errorCode && this.label.equals(label);
    }
}
