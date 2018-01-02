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
package com.waz.zclient.controllers.background;

import com.waz.api.ImageAsset;
import com.waz.api.UpdateListener;
import com.waz.api.User;

import java.util.HashSet;
import java.util.Set;

public class DialogBackgroundImageController implements IDialogBackgroundImageController,
                                                        UpdateListener {
    private Set<DialogBackgroundImageObserver> observers = new HashSet<>();
    private ImageAsset imageAsset;
    private User user;

    @Override
    public void tearDown() {
        if (imageAsset != null) {
            imageAsset = null;
        }
        if (user != null) {
            user.removeUpdateListener(this);
            user = null;
        }
        observers = null;
    }

    @Override
    public void addObserver(DialogBackgroundImageObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(DialogBackgroundImageObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void setImageAsset(ImageAsset imageAsset, boolean blurred) {
        if (this.imageAsset != null) {
            this.imageAsset = null;
        }
        if (user != null) {
            user.removeUpdateListener(this);
            user = null;
        }

        if (imageAsset == null) {
            for (DialogBackgroundImageObserver observer : observers) {
                observer.onImageAssetLoaded(null, blurred);
            }
            return;
        }

        this.imageAsset = imageAsset;

        for (DialogBackgroundImageObserver observer : observers) {
            observer.onImageAssetLoaded(imageAsset, blurred);
        }
    }

    @Override
    public void setUser(User user) {
        if (this.user != null) {
            this.user.removeUpdateListener(this);
        }
        this.user = user;
        if (user == null) {
            for (DialogBackgroundImageObserver observer : observers) {
                observer.onImageAssetLoaded(null, false);
            }
            return;
        }
        imageAsset = user.getPicture();
        user.addUpdateListener(this);
        for (DialogBackgroundImageObserver observer : observers) {
            observer.onImageAssetLoaded(imageAsset, false);
        }
    }

    @Override
    public ImageAsset getImageAsset() {
        return imageAsset;
    }

    @Override
    public void updated() {
        imageAsset = user.getPicture();
        for (DialogBackgroundImageObserver observer : observers) {
            observer.onImageAssetLoaded(imageAsset, false);
        }
    }
}
