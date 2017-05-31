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
package com.waz.zclient.controllers.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IntDef;
import android.support.annotation.StyleRes;
import com.waz.zclient.R;
import com.waz.zclient.controllers.userpreferences.UserPreferencesController;
import com.waz.zclient.ui.theme.OptionsDarkTheme;
import com.waz.zclient.ui.theme.OptionsLightTheme;
import com.waz.zclient.ui.theme.OptionsTheme;
import com.waz.zclient.utils.LayoutSpec;

public class ThemeController implements IThemeController {
    public static final String TAG = ThemeController.class.getName();
    @IntDef({DARK_THEME,
             LIGHT_THEME
    })
    public @interface Theme { }
    private static final int LIGHT_THEME = 1;
    private static final int DARK_THEME = 2;
    private final String deprecatedThemeKey;
    private final String themeKey;
    private SharedPreferences prefs;
    private boolean isPending = false;

    private @Theme int currentTheme;
    private OptionsTheme optionsDarkTheme;
    private OptionsTheme optionsLightTheme;
    private final boolean isTablet;

    public ThemeController(Context context) {
        isTablet = LayoutSpec.isTablet(context);
        deprecatedThemeKey = context.getString(R.string.pref_account_theme_key);
        themeKey = context.getString(R.string.pref_options_theme_switch_key);
        prefs = context.getSharedPreferences(UserPreferencesController.USER_PREFS_TAG, Context.MODE_PRIVATE);
        currentTheme = getThemeFromPreferences();
        optionsDarkTheme = new OptionsDarkTheme(context);
        optionsLightTheme = new OptionsLightTheme(context);
    }

    @SuppressWarnings("ResourceType")
    private @Theme int getThemeFromPreferences() {
        if (isTablet) {
            return LIGHT_THEME;
        }

        if (prefs.contains(deprecatedThemeKey)) {
            final int oldTheme = Integer.parseInt(prefs.getString(deprecatedThemeKey, String.valueOf(LIGHT_THEME)));
            prefs.edit()
                 .putBoolean(themeKey, oldTheme == DARK_THEME)
                 .remove(deprecatedThemeKey)
                 .apply();

            return oldTheme;
        }

        if (prefs.getBoolean(themeKey, false)) {
            return DARK_THEME;
        }

        return LIGHT_THEME;
    }

    @Override
    public void toggleTheme(boolean fromPreferences) {
        final int newTheme;
        if (currentTheme == ThemeController.LIGHT_THEME) {
            newTheme = ThemeController.DARK_THEME;
        } else {
            newTheme = ThemeController.LIGHT_THEME;
        }
        setTheme(newTheme);
    }

    @Override
    public void toggleThemePending(boolean fromPreferences) {
        isPending = !isPending;
        toggleTheme(fromPreferences);
    }

    @SuppressWarnings("WrongConstant")
    private void setTheme(@Theme int themeId) {
        currentTheme = themeId;
        prefs.edit().putString(deprecatedThemeKey, String.valueOf(currentTheme)).apply();
        prefs.edit().putBoolean(themeKey, isDarkTheme()).apply();
    }

    @Override
    public @StyleRes int getTheme() {
        if (currentTheme == DARK_THEME) {
            return R.style.Theme_Dark;
        } else {
            return R.style.Theme_Light;
        }
    }

    @Override
    public OptionsTheme getOptionsDarkTheme() {
        return optionsDarkTheme;
    }

    @Override
    public OptionsTheme getOptionsLightTheme() {
        return optionsLightTheme;
    }

    @Override
    public OptionsTheme getThemeDependentOptionsTheme() {
        if (currentTheme == DARK_THEME) {
            return optionsDarkTheme;
        } else {
            return optionsLightTheme;
        }
    }

    @Override
    public void tearDown() {
        prefs = null;
        optionsDarkTheme.tearDown();
        optionsLightTheme.tearDown();
    }

    @Override
    public boolean isDarkTheme() {
        return currentTheme == DARK_THEME;
    }

    @Override
    public boolean isRestartPending() {
        return isPending;
    }

    @Override
    public void removePendingRestart() {
        isPending = false;
    }
}
