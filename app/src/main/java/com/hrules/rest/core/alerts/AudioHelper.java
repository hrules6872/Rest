/*
 * Copyright (c) 2017. Héctor de Isidro - hrules6872
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hrules.rest.core.alerts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import com.hrules.rest.R;
import com.hrules.rest.commons.SupportVersion;
import com.hrules.rest.core.commons.ZenModeHelper;

public final class AudioHelper {
  private static final int MAX_STREAM_TYPES = 2;
  private static final int SOUND_PRIORITY = 1;
  private static final int SOUND_QUALITY = 0;

  private static final float VOLUME_MAX = 1f;
  private static final float VOLUME_NORMAL = 0.5f;

  private static final int DEFAULT_SOUNDPOOL_PLAY_PRIORITY = 1;
  private static final int DEFAULT_SOUNDPOOL_PLAY_NO_LOOP = 0;
  private static final float DEFAULT_SOUNDPOOL_PLAY_RATE = 1f;

  private final AudioManager audioManager;
  private final int streamType;
  private final boolean maxVolumeSound;

  private SoundPool soundPool;
  private ToggleMuteTask toggleMuteTask;

  private int soundShortId;
  private int soundShort2Id;
  private int soundLongId;
  private boolean soundShortLoaded;
  private boolean soundShort2Loaded;
  private boolean soundLongLoaded;

  private ZenModeHelper zenModeHelper;

  @SuppressWarnings("deprecation") public AudioHelper(@NonNull Context context, int streamType, boolean maxVolumeSound) {
    this.streamType = streamType;
    this.maxVolumeSound = maxVolumeSound;

    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    if (SupportVersion.isLollipopOrAbove()) {
      SoundPool.Builder builder = new SoundPool.Builder();
      AudioAttributes audioAttributes = new AudioAttributes.Builder().setLegacyStreamType(streamType).build();
      builder.setAudioAttributes(audioAttributes).setMaxStreams(MAX_STREAM_TYPES);
      soundPool = builder.build();
    } else {
      soundPool = new SoundPool(MAX_STREAM_TYPES, streamType, SOUND_QUALITY);
    }

    soundPool.setOnLoadCompleteListener((soundPoolSource, sampleId, status) -> {
      if (sampleId == soundShortId) {
        soundShortLoaded = true;
      } else if (sampleId == soundShort2Id) {
        soundShort2Loaded = true;
      } else if (sampleId == soundLongId) {
        soundLongLoaded = true;
      }
    });
    soundShortId = soundPool.load(context, R.raw.beep_short, SOUND_PRIORITY);
    soundShort2Id = soundPool.load(context, R.raw.beep_short2, SOUND_PRIORITY);
    soundLongId = soundPool.load(context, R.raw.beep_long, SOUND_PRIORITY);

    zenModeHelper = new ZenModeHelper(context);
  }

  private float getCurrentVolume(int streamType) {
    float currentVolume = audioManager.getStreamVolume(streamType);
    float maxVolume = audioManager.getStreamMaxVolume(streamType);
    return currentVolume / maxVolume * (streamType == AudioManager.STREAM_MUSIC || maxVolumeSound ? VOLUME_MAX : VOLUME_NORMAL);
  }

  public void playShort() {
    if (soundShortLoaded) {
      float volume = getCurrentVolume(streamType);
      soundPool.play(soundShortId, volume, volume, DEFAULT_SOUNDPOOL_PLAY_PRIORITY, DEFAULT_SOUNDPOOL_PLAY_NO_LOOP,
          DEFAULT_SOUNDPOOL_PLAY_RATE);
    }
  }

  public void playShort2() {
    if (soundShort2Loaded) {
      float volume = getCurrentVolume(streamType);
      soundPool.play(soundShort2Id, volume, volume, DEFAULT_SOUNDPOOL_PLAY_PRIORITY, DEFAULT_SOUNDPOOL_PLAY_NO_LOOP,
          DEFAULT_SOUNDPOOL_PLAY_RATE);
    }
  }

  public void playLong() {
    if (soundLongLoaded) {
      float volume = getCurrentVolume(streamType);
      soundPool.play(soundLongId, volume, volume, DEFAULT_SOUNDPOOL_PLAY_PRIORITY, DEFAULT_SOUNDPOOL_PLAY_NO_LOOP,
          DEFAULT_SOUNDPOOL_PLAY_RATE);
    }
  }

  public void toggleMute(long delay) {
    toggleMuteTask = new ToggleMuteTask(audioManager, delay, zenModeHelper);
    toggleMuteTask.execute();
  }

  public void release() {
    if (toggleMuteTask != null) {
      toggleMuteTask.cancel(true);
      toggleMuteTask = null;
    }

    if (soundPool != null) {
      soundPool.release();
      soundPool = null;
    }

    zenModeHelper.release();
    zenModeHelper = null;
  }

  private final static class ToggleMuteTask extends AsyncTask<Void, Void, Boolean> {
    private static final int VOLUME_MUTE_PERCENT = 30;
    private static final int NO_VISUAL_AUDIO_INDICATOR_FLAG = 0;

    private final AudioManager audioManager;

    private final int volumeMediaMax;
    private final int volumeMediaPrevious;

    private final boolean shouldChangeNotificationVolume;
    private final int volumeNotificationMax;
    private final int volumeNotificationPrevious;

    private final long delay;

    ToggleMuteTask(@NonNull AudioManager audioManager, long delay, @NonNull ZenModeHelper zenModeHelper) {
      this.audioManager = audioManager;
      this.delay = delay;

      volumeMediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      volumeMediaPrevious = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

      shouldChangeNotificationVolume = !zenModeHelper.isZenModeActive();
      volumeNotificationPrevious = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
      volumeNotificationMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
    }

    @Override protected void onPreExecute() {
      int volumeMediaAttenuate = (VOLUME_MUTE_PERCENT * volumeMediaMax) / 100;
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeMediaAttenuate, NO_VISUAL_AUDIO_INDICATOR_FLAG);

      if (shouldChangeNotificationVolume) {
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volumeNotificationMax, NO_VISUAL_AUDIO_INDICATOR_FLAG);
      }
    }

    @Override protected Boolean doInBackground(Void... params) {
      try {
        Thread.sleep(delay);
      } catch (InterruptedException ignored) {
      }
      return true;
    }

    @Override protected void onPostExecute(Boolean result) {
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeMediaPrevious, NO_VISUAL_AUDIO_INDICATOR_FLAG);

      if (shouldChangeNotificationVolume) {
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volumeNotificationPrevious, NO_VISUAL_AUDIO_INDICATOR_FLAG);
      }
    }
  }
}
