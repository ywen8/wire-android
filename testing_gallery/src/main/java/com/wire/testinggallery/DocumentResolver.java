/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wire.testinggallery;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;

public class DocumentResolver {

    private static final File WIRE_TESTING_FILES_DIRECTORY =
        Environment.getExternalStoragePublicDirectory("Wire");

    private final ContentResolver contentResolver;

    public DocumentResolver(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public Uri getDocumentPath() {
        return fileQuery(WIRE_TESTING_FILES_DIRECTORY);
    }

    public Uri getVideoPath() {
        return mediaQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null);
    }

    public Uri getAudioPath() {
        return mediaQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);
    }

    public Uri getImagePath() {
        return mediaQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null);
    }

    private Uri mediaQuery(Uri baseUri, String[] projection) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(baseUri, projection, null, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
            if (cursor == null) {
                return null;
            }
            final int columnFileIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            if (cursor.moveToNext()) {
                final int id = cursor.getInt(columnFileIdIndex);
                return Uri.withAppendedPath(baseUri, String.valueOf(id));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private Uri fileQuery(File baseDir) {
        File choice = null;
        long lastMod = 0;
        for (File file : baseDir.listFiles()) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        if (choice != null)
            return Uri.fromFile(choice);
        return null;
    }
}
