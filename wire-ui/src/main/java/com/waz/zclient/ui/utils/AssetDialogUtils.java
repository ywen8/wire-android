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
package com.waz.zclient.ui.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.waz.api.Asset;

import com.waz.utils.wrappers.URI;
import com.waz.zclient.ui.R;
import com.waz.zclient.utils.ViewUtils;

public class AssetDialogUtils {

    public static void showFileActionSheet(final Context context, final Asset asset, final URI uri, boolean fileCanBeOpened, final AssetDialogCallback callback) {
        final AppCompatDialog fileActionSheetDialog = new AppCompatDialog(context);
        fileActionSheetDialog.setTitle(asset.getName());
        fileActionSheetDialog.setContentView(R.layout.file_action_sheet_dialog);

        TextView title = ViewUtils.getView(fileActionSheetDialog, R.id.title);
        title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        title.setTypeface(TypefaceUtils.getTypeface(context.getResources().getString(R.string.wire__typeface__medium)));
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                          context.getResources().getDimensionPixelSize(R.dimen.wire__text_size__regular));
        title.setGravity(Gravity.CENTER);

        TextView openButton =  ViewUtils.getView(fileActionSheetDialog, R.id.ttv__file_action_dialog__open);
        View noAppFoundLabel =  ViewUtils.getView(fileActionSheetDialog, R.id.ttv__file_action_dialog__open__no_app_found);
        View saveButton =  ViewUtils.getView(fileActionSheetDialog, R.id.ttv__file_action_dialog__save);

        // Opening file
        if (fileCanBeOpened) {
            noAppFoundLabel.setVisibility(View.GONE);
            openButton.setAlpha(1f);
            openButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onOpenedFile(uri);
                    fileActionSheetDialog.dismiss();
                }
            });
        } else {
            noAppFoundLabel.setVisibility(View.VISIBLE);
            float disabledAlpha = ResourceUtils.getResourceFloat(context.getResources(), R.dimen.button__disabled_state__alpha);
            openButton.setAlpha(disabledAlpha);
        }

        // Saving file
        final Asset.LoadCallback<URI> saveFileLoadCallback = new Asset.LoadCallback<URI>() {
            @Override
            public void onLoaded(URI uri) {
                callback.onSavedFile(uri);
            }

            @Override
            public void onLoadFailed() {
            }
        };

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileActionSheetDialog.dismiss();
                asset.saveToDownloads(saveFileLoadCallback);
            }
        });

        fileActionSheetDialog.show();
    }

    public interface AssetDialogCallback {
        void onOpenedFile(@NonNull URI uri);

        void onSavedFile(@NonNull URI uri);
    }
}
