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
package com.waz.zclient.pages.main.pickuser.views;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.waz.api.User;
import com.waz.zclient.R;
import com.waz.zclient.ui.utils.TypefaceUtils;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.views.PickableElement;
import com.waz.zclient.views.PickerSpannableEditText;

public class SearchBoxView extends FrameLayout {

    private PickerSpannableEditText inputEditText;
    private TextView clearButton;
    private View colorBottomBorder;

    private Callback callback;

    public SearchBoxView(Context context) {
        this(context, null);
    }

    public SearchBoxView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchBoxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.search_box_view, this, true);

        clearButton = ViewUtils.getView(this, R.id.gtv_pickuser__clearbutton);
        inputEditText = ViewUtils.getView(this, R.id.puet_pickuser__searchbox);
        colorBottomBorder = ViewUtils.getView(this, R.id.v_people_picker__input__color_bottom_border);

        int hintColorStartUI;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //noinspection deprecation
            hintColorStartUI = getResources().getColor(R.color.text__secondary_light);
        } else {
            hintColorStartUI = getResources().getColor(R.color.text__secondary_light, context.getTheme());
        }
        inputEditText.setTypeface(TypefaceUtils.getTypeface(context.getString(R.string.wire__typeface__light)));
        inputEditText.setCallback(new PickerSpannableEditText.Callback() {

            @Override
            public void onRemovedTokenSpan(PickableElement element) {
                if (callback != null) {
                    callback.onRemovedTokenSpan(element);
                }
            }

            @Override
            public void afterTextChanged(String s) {
                if (callback != null) {
                    callback.afterTextChanged(s);
                }
            }
        });
        inputEditText.setFocusable(true);
        inputEditText.setFocusableInTouchMode(true);
        inputEditText.setHintTextColor(hintColorStartUI);
        inputEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (callback != null && (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
                    callback.onKeyboardDoneAction();
                    return true;
                }
                return false;
            }
        });

        inputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (callback != null) {
                    callback.onFocusChange(hasFocus);
                }
            }
        });

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            inputEditText.setLineSpacing(getResources().getDimensionPixelSize(R.dimen.people_picker__input__line_spacing_extra__greater_than_android_sdk_19), 1);
        }

        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onClearButton();
                }
            }
        });

    }

    public void setHintText(CharSequence hintText) {
        inputEditText.setHintText(hintText);
    }

    public void showClearButton(boolean show) {
        clearButton.setVisibility(show ? VISIBLE : GONE);
    }

    public void forceDarkTheme() {
        int textColor;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //noinspection deprecation
            textColor = getResources().getColor(R.color.text__primary_dark);
        } else {
            textColor = getResources().getColor(R.color.text__primary_dark, getContext().getTheme());
        }
        inputEditText.setTextColor(textColor);
        inputEditText.setHintTextColor(textColor);
        clearButton.setTextColor(textColor);
        inputEditText.applyLightTheme(false);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void applyLightTheme(boolean light) {
        inputEditText.applyLightTheme(light);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setAccentColor(int color) {
        colorBottomBorder.setBackgroundColor(color);
        inputEditText.setAccentColor(color);
    }

    public void addUser(final User user) {
        inputEditText.addElement(new PickableElement() {
            @Override
            public String name() {
                return user.getDisplayName();
            }

            @Override
            public String id() {
                return user.getId();
            }
        });
    }

    public void removeUser(final User user) {
        inputEditText.removeElement(new PickableElement() {
            @Override
            public String name() {
                return user.getDisplayName();
            }

            @Override
            public String id() {
                return user.getId();
            }
        });
    }

    public String getSearchFilter() {
        return inputEditText.getSearchFilter();
    }

    public void reset() {
        inputEditText.reset();
    }

    public void setFocus() {
        inputEditText.setCursorVisible(true);
        inputEditText.requestFocus();
    }

    public interface Callback extends PickerSpannableEditText.Callback {
        void onKeyboardDoneAction();
        void onFocusChange(boolean hasFocus);
        void onClearButton();
    }
}
