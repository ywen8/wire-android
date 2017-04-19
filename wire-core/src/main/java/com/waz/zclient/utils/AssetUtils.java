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
package com.waz.zclient.utils;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import com.waz.utils.wrappers.AndroidURI;
import com.waz.utils.wrappers.AndroidURIUtil;
import com.waz.utils.wrappers.URI;
import timber.log.Timber;

import java.util.List;

public class AssetUtils {

    public static int assetSizeToMB(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return 0;
        }
        return (int) (sizeInBytes / 1024 / 1024);
    }

    public static String assetMimeTypeToExtension(String mimeType) {
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = mimeTypeMap.getExtensionFromMimeType(mimeType);
        if (extension == null) {
            return mimeType;
        }
        return extension;
    }

    public static Intent getOpenFileIntent(URI uri, String mimeType) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(AndroidURIUtil.unwrap(uri), mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    public static boolean fileTypeCanBeOpened(PackageManager manager, Intent intent) {
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return infos.size() > 0;

    }

    public static long getVideoAssetDurationMilliSec(Context context, URI uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, AndroidURIUtil.unwrap(uri));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(time);
    }

    /**
     * From https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
     * Get a file path from a URI. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The URI to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final URI uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, AndroidURIUtil.unwrap(uri))) {
            if (isExternalStorageDocument(uri)) {
                // ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(AndroidURIUtil.unwrap(uri));
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                final String id = DocumentsContract.getDocumentId(AndroidURIUtil.unwrap(uri));
                final URI contentUri = new AndroidURI(ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id))
                );

                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                // MediaProvider
                final String docId = DocumentsContract.getDocumentId(AndroidURIUtil.unwrap(uri));
                final String[] split = docId.split(":");
                final String type = split[0];

                URI contentUri = null;
                if ("image".equals(type)) {
                    contentUri = new AndroidURI(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                } else if ("video".equals(type)) {
                    contentUri = new AndroidURI(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                } else if ("audio".equals(type)) {
                    contentUri = new AndroidURI(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                    split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // MediaStore (and general)
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // File
            return uri.getPath();
        }
        return null;
    }

    /**
     * From https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
     * Get the value of the data column for this URI. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The URI to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, URI uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(AndroidURIUtil.unwrap(uri), projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Timber.w(e, "Unable to get data column");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    /**
     * From https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
     * @param uri The URI to check.
     * @return Whether the URI authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(URI uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * From https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
     * @param uri The URI to check.
     * @return Whether the URI authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(URI uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * From https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
     * @param uri The URI to check.
     * @return Whether the URI authority is MediaProvider.
     */
    public static boolean isMediaDocument(URI uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
