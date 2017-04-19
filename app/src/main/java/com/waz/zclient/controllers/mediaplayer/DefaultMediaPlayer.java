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
package com.waz.zclient.controllers.mediaplayer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import com.waz.utils.wrappers.URI;
import timber.log.Timber;

public class DefaultMediaPlayer implements IMediaPlayer,
                                           MediaPlayer.OnCompletionListener,
                                           MediaPlayer.OnErrorListener,
                                           MediaPlayer.OnPreparedListener,
                                           MediaPlayer.OnSeekCompleteListener {

    public final static int PLAYING_NOT_STARTED = -1;

    private MediaPlayerListener listener;
    private MediaPlayer mediaPlayer;
    private MediaPlayerState mediaPlayerState;
    private URI dataSource;

    public DefaultMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayerState = MediaPlayerState.Idle;
    }

    @Override
    public void setDataSource(URI uri) {
        this.dataSource = uri;
        try {
            if (mediaPlayerState.isSetDatasourceAllowed()) {
                mediaPlayer.setDataSource(uri.toString());
                setMediaPlayerState(MediaPlayerState.Initialized);
            }
        } catch (Exception e) {
            Timber.e(e, "setDataSource('%s')", dataSource);
            if (listener != null) {
                listener.onError();
            }
        }
    }

    @Override
    public void start() {
        if (mediaPlayerState.isStartAllowed()) {
            mediaPlayer.start();
            setMediaPlayerState(MediaPlayerState.Started);
            if (listener != null) {
                listener.onStart();
            }
        } else {
            if (mediaPlayerState != MediaPlayerState.Error) {
                setMediaPlayerState(MediaPlayerState.Idle);
            }
            reset();
            try {
                if (dataSource == null) {
                    onError();
                } else {
                    setAudioStreamType(AudioManager.STREAM_MUSIC);
                    setDataSource(dataSource);
                    prepareAsync();
                }
            } catch (Exception e) {
                onError();
            }
        }
    }

    @Override
    public void pause() {
        if (!mediaPlayerState.isPauseAllowed()) {
            return;
        }
        mediaPlayer.pause();
        setMediaPlayerState(MediaPlayerState.Paused);
        if (listener != null) {
            listener.onPause();
        }
    }

    private void setMediaPlayerState(MediaPlayerState state) {
        // We want to skip state after an error (like stop, playback complete etc.)
        if (mediaPlayerState == MediaPlayerState.Error &&
            !(state == MediaPlayerState.Initialized ||
              state == MediaPlayerState.Idle)) {
            return;
        }
        this.mediaPlayerState = state;
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    @Override
    public void stop() {
        if (!mediaPlayerState.isStopAllowed()) {
            return;
        }
        mediaPlayer.stop();
        setMediaPlayerState(MediaPlayerState.Stopped);
        if (listener != null) {
            listener.onStop();
        }
    }

    @Override
    public void release() {
        mediaPlayer.release();
        setMediaPlayerState(MediaPlayerState.Idle);
    }

    @Override
    public void reset() {
        if (!mediaPlayerState.isResetAllowed()) {
            return;
        }
        mediaPlayer.reset();
        setMediaPlayerState(MediaPlayerState.Idle);
    }

    @Override
    public void seekTo(int positionMs) {
        if (positionMs > PLAYING_NOT_STARTED &&
            mediaPlayerState.isSeekToAllowed()) {
            mediaPlayer.seekTo(positionMs);
        } else {
            start();
        }
    }

    @Override
    public int getCurrentPosition() {
        if (mediaPlayerState.isGetCurrentPositionAllowed()) {
            return mediaPlayer.getCurrentPosition();
        }
        return PLAYING_NOT_STARTED;
    }

    @Override
    public MediaPlayerState getState() {
        return mediaPlayerState;
    }

    @Override
    public void setState(MediaPlayerState state) {
        switch (state) {
            case Idle:
            case Initialized:
            case Prepared:
            case Started:
            case Paused:
            case Stopped:
            case PlaybackCompleted:
            case End:
                throw new IllegalArgumentException();
        }
        setMediaPlayerState(state);
    }

    private void prepareAsync() {
        if (mediaPlayerState.isPrepareAllowed()) {
            mediaPlayer.prepareAsync();
        } else {
            onPrepared(mediaPlayer);
        }
    }

    private void setAudioStreamType(int type) {
        if (!mediaPlayerState.isSetAudioStreamTypeAllowed()) {
            return;
        }
        mediaPlayer.setAudioStreamType(type);
    }

    @Override
    public void setMediaPlayerListener(MediaPlayerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (listener != null) {
            listener.onCompletion();
        }
        setMediaPlayerState(MediaPlayerState.PlaybackCompleted);
    }

    private void onError() {
        onError(mediaPlayer, 0, 0);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
            case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
            case -38: // means generic error
                return true;
            default:
                setMediaPlayerState(MediaPlayerState.Error);
                if (listener != null) {
                    listener.onError();
                }
                return false;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        setMediaPlayerState(MediaPlayerState.Prepared);
        if (listener != null) {
            listener.onPrepared();
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (listener != null) {
            listener.onSeekComplete();
        }
    }
}
