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
import android.util.Log;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

class DocumentResolver {

    private static final String TAG = "TestingGallery";

    private static final String WIRE_DIRECTORY = "wire";

    static final File WIRE_TESTING_FILES_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(WIRE_DIRECTORY + "/files");
    private static final File WIRE_TESTING_IMAGES_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(WIRE_DIRECTORY + "/images");
    private static final File WIRE_TESTING_VIDEOS_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(WIRE_DIRECTORY + "/video");

    private final ContentResolver contentResolver;

    DocumentResolver(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    Uri getDocumentUri() {
        Log.i(TAG, "Received request for File");
        return fileQuery(WIRE_TESTING_FILES_DIRECTORY);
    }

    Uri getVideoUri() {
        Log.i(TAG, "Received request for Video file");
        return fileQuery(WIRE_TESTING_VIDEOS_DIRECTORY);
    }

    Uri getAudioUri() {
        Log.i(TAG, "Received request for Audio file");
        return fileQuery(WIRE_TESTING_FILES_DIRECTORY);
    }

    Uri getImageUri() {
        Log.i(TAG, "Received request for Image");
        return fileQuery(WIRE_TESTING_IMAGES_DIRECTORY);
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
        File[] files = baseDir.listFiles();
        Log.i(TAG, String.format("%s files found in %s", files.length, baseDir));
        File lastUpdatedFile = null;
        long theLastModifiedTime = 0;
        if (files.length > 0) {
            for (File file : files) {
                long modifiedTime = file.lastModified();
                if (modifiedTime > theLastModifiedTime) {
                    theLastModifiedTime = modifiedTime;
                    lastUpdatedFile = file;
                }
            }
            if (lastUpdatedFile != null) {
                Log.i(TAG, String.format("Got recent file: %s", lastUpdatedFile.getName()));
                return Uri.fromFile(lastUpdatedFile);
            } else {
                Log.w(TAG,
                    String.format("There was %s files, but none of them selected",
                        files.length));
            }
        }
        Log.w(TAG, "No files! Returning null!!");
        return null;
    }
}
