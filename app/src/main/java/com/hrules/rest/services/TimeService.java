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

package com.hrules.rest.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.commons.SupportVersion;
import com.hrules.rest.core.alerts.AudioHelper;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.commons.ZenModeHelper;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.core.time.TimeManagerListener;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.helpers.StopwatchHelper;
import com.hrules.rest.presentation.views.activities.MainActivityView;
import java.util.concurrent.TimeUnit;

public final class TimeService extends Service {
  public static final int NOTIFICATION_ID = Integer.MAX_VALUE;
  public static final String NOTIFICATION_CHANNEL_ID = "main";

  private static final long MIN_TIME_TO_DISPATCH_HALFWAYALERT_MILLI = TimeUnit.MINUTES.toMillis(1) / 2;

  private static final long VOLUME_MUTE_ALERT_DURATION_MILLI = 900;
  private static final long VOLUME_MUTE_HALFWAY_DURATION_MILLI = VOLUME_MUTE_ALERT_DURATION_MILLI;
  private static final long VOLUME_MUTE_TEN_SECONDS_DURATION_MILLI = VOLUME_MUTE_ALERT_DURATION_MILLI / 2;
  private static final long VOLUME_MUTE_THREE_SECONDS_DURATION_MILLI = TimeUnit.SECONDS.toMillis(3) + VOLUME_MUTE_ALERT_DURATION_MILLI;
  private static final long VOLUME_MUTE_NO_MILLI = 0;

  private static final long REMOTEVIEWS_WORKAROUND_TRIGGER_SECONDS = 5;

  private NotificationManager notificationManager;
  private RemoteViews remoteViews;

  private BroadcastReceiver timeServiceReceiver;

  private StopwatchHelper stopwatchHelper;

  private Preferences preferences;
  private boolean prefsSound;
  private boolean prefsVibrate;
  private boolean prefsHalfwayAlert;
  private boolean prefsTenSecondsAlert;
  private boolean prefsThreeSecondsBeep;
  private boolean prefsUseMediaStream;
  private boolean prefsMuteMediaStream;
  private boolean prefs_alertMaxVolumeSound;
  private boolean prefsSmartStopwatch;
  private boolean prefsAutoStopStopwatch;
  private boolean prefsVibrateButtons;

  private boolean dispatchedAlert;
  private boolean dispatchedHalfwayAlert;
  private boolean dispatchedTenSecondsAlert;
  private boolean dispatchedThreeSecondsBeep1;
  private boolean dispatchedThreeSecondsBeep2;
  private boolean dispatchedThreeSecondsBeep3;

  private AudioHelper audioHelper;

  private long lastSecondNotificationUpdate;
  private long remoteViewsWorkaroundSecondsCounter;

  private VibratorHelper vibratorHelper;
  private ZenModeHelper zenModeHelper;

  @SuppressWarnings("deprecation") @Override public void onCreate() {
    vibratorHelper = new VibratorHelper(this);
    zenModeHelper = new ZenModeHelper(this);
    checkIsDNDModeActive();

    preferences = new Preferences(this);
    getPreferences();
    preferences.addListener(sharedPreferenceChangeListener);

    stopwatchHelper = new StopwatchHelper(preferences);

    resetAlertCounters();
    TimeManager.INSTANCE.setCountdownTimeMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));

    createAudioHelper();
    registerTimeServiceReceiver();

    createNotification();

    TimeManager.INSTANCE.addListener(timeManagerListener);
  }

  @Override public void onDestroy() {
    stopStopwatch();
    sendBroadcast(new Intent(AppConstants.ACTIONS.SERVICE_SHUTDOWN));

    TimeManager.INSTANCE.removeListener(timeManagerListener);

    unregisterTimeServiceReceiver();
    preferences.removeListener(sharedPreferenceChangeListener);
    releaseAudioHelper();

    zenModeHelper.release();
    zenModeHelper = null;
  }

  @Override public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException();
  }

  private void checkIsDNDModeActive() {
    if (zenModeHelper.isZenModeActive()) {
      Toast.makeText(this, R.string.text_dndWarning, Toast.LENGTH_LONG).show();
    }
  }

  private void getPreferences() {
    Resources resources = getResources();

    prefsSound =
        preferences.getBoolean(resources.getString(R.string.prefs_alertSoundKey), resources.getBoolean(R.bool.prefs_alertSoundDefault));
    prefsVibrate =
        preferences.getBoolean(resources.getString(R.string.prefs_alertVibrateKey), resources.getBoolean(R.bool.prefs_alertVibrateDefault));

    prefsHalfwayAlert =
        preferences.getBoolean(resources.getString(R.string.prefs_alertHalfwayKey), resources.getBoolean(R.bool.prefs_alertHalfwayDefault));
    prefsTenSecondsAlert = preferences.getBoolean(resources.getString(R.string.prefs_alertTenSecondsKey),
        resources.getBoolean(R.bool.prefs_alertTenSecondsDefault));
    prefsThreeSecondsBeep = preferences.getBoolean(resources.getString(R.string.prefs_alertThreeSecondsKey),
        resources.getBoolean(R.bool.prefs_alertThreeSecondsDefault));
    prefsUseMediaStream = preferences.getBoolean(resources.getString(R.string.prefs_alertUseMediaStreamKey),
        resources.getBoolean(R.bool.prefs_alertUseMediaStreamDefault));
    prefsMuteMediaStream = preferences.getBoolean(resources.getString(R.string.prefs_alertMuteMediaStreamKey),
        resources.getBoolean(R.bool.prefs_alertMuteMediaStreamDefault));
    prefs_alertMaxVolumeSound = preferences.getBoolean(resources.getString(R.string.prefs_alertMaxVolumeSoundKey),
        resources.getBoolean(R.bool.prefs_alertMaxVolumeSoundDefault));

    prefsSmartStopwatch = preferences.getBoolean(resources.getString(R.string.prefs_stopwatchSmartKey),
        resources.getBoolean(R.bool.prefs_stopwatchSmartDefault));
    prefsAutoStopStopwatch = preferences.getBoolean(resources.getString(R.string.prefs_stopwatchAutoStopKey),
        resources.getBoolean(R.bool.prefs_stopwatchAutoStopDefault));

    prefsVibrateButtons = preferences.getBoolean(resources.getString(R.string.prefs_controlVibrateButtonsKey),
        resources.getBoolean(R.bool.prefs_controlVibrateButtonsDefault));
  }

  private NotificationManager getNotificationManager() {
    if (notificationManager == null) {
      notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
    return notificationManager;
  }

  private void createNotification() {
    createRemoteViews();
    startForeground(NOTIFICATION_ID, getNotification());
  }

  private @NonNull Notification getNotification() {
    NotificationCompat.Builder builder = getNotificationBuilder();
    return builder.setContentIntent(getNotificationIntent())
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setSmallIcon(getSmallIconResId())
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
        .setCustomContentView(remoteViews)
        .setCustomBigContentView(remoteViews)
        .addAction(getActionExit())
        .setShowWhen(false)
        .build();
  }

  @SuppressWarnings("deprecation") private @NonNull NotificationCompat.Builder getNotificationBuilder() {
    if (SupportVersion.isOreoOrAbove()) {
      NotificationChannel channel =
          new NotificationChannel(TimeService.NOTIFICATION_CHANNEL_ID, getString(R.string.notification_channel_default),
              NotificationManager.IMPORTANCE_LOW);
      channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
      channel.enableLights(false);
      channel.enableVibration(false);
      channel.setSound(null, null);
      getNotificationManager().createNotificationChannel(channel);
      return new NotificationCompat.Builder(getApplicationContext(), TimeService.NOTIFICATION_CHANNEL_ID);
    } else {
      return new NotificationCompat.Builder(this);
    }
  }

  private @NonNull PendingIntent getNotificationIntent() {
    Intent notificationIntent = new Intent(this, MainActivityView.class);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return PendingIntent.getActivity(this, PendingIntent.FLAG_UPDATE_CURRENT, notificationIntent, 0);
  }

  private int getSmallIconResId() {
    if (TimeManager.INSTANCE.isRunning()) {
      if (TimeManager.INSTANCE.isCountdownOver()) {
        return R.drawable.ic_notification_over;
      } else {
        return R.drawable.ic_notification_running;
      }
    } else {
      return R.drawable.ic_notification;
    }
  }

  private @NonNull NotificationCompat.Action getActionExit() {
    return new NotificationCompat.Action(R.drawable.ic_exit, getString(R.string.text_notificationStop),
        PendingIntent.getBroadcast(this, 0, new Intent(AppConstants.ACTIONS.EXIT, null), PendingIntent.FLAG_UPDATE_CURRENT));
  }

  private void createRemoteViews() {
    remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification);
    updateRemoteViews(remoteViews);
    setOnClickPendingIntentOnRemoteViews(remoteViews);
  }

  private void updateRemoteViews(@NonNull RemoteViews remoteView) {
    if (TimeManager.INSTANCE.isRunning()) {
      remoteView.setImageViewResource(R.id.button_changeState, R.drawable.ic_stop);
      remoteView.setViewVisibility(R.id.button_replay, View.VISIBLE);
    } else {
      remoteView.setImageViewResource(R.id.button_changeState, R.drawable.ic_play);
      remoteView.setViewVisibility(R.id.button_replay, View.INVISIBLE);
    }

    remoteView.setViewVisibility(R.id.button_changeState,
        TimeManager.INSTANCE.getCountdownTimeMilli() == 0 ? View.INVISIBLE : View.VISIBLE);

    remoteView.setTextViewText(R.id.text_countDown,
        TimeUtils.milliToMinutesSecondsString(TimeUtils.getCountdownMilliUnsigned(), getResources()));
    remoteView.setTextColor(R.id.text_countDown, TimeUtils.getNotificationTextColorFromMilli());
  }

  private void setOnClickPendingIntentOnRemoteViews(@NonNull RemoteViews remoteView) {
    remoteView.setOnClickPendingIntent(R.id.button_changeState,
        PendingIntent.getBroadcast(this, 0, new Intent(AppConstants.ACTIONS.CHANGE_STATE, null), PendingIntent.FLAG_UPDATE_CURRENT));
    remoteView.setOnClickPendingIntent(R.id.button_replay,
        PendingIntent.getBroadcast(this, 0, new Intent(AppConstants.ACTIONS.REPLAY, null), PendingIntent.FLAG_UPDATE_CURRENT));
  }

  private void updateNotification() {
    if (TimeManager.INSTANCE.isRunning()) {
      if (remoteViewsWorkaroundSecondsCounter >= REMOTEVIEWS_WORKAROUND_TRIGGER_SECONDS) {
        // remoteViews huge memory usage workaround
        remoteViewsWorkaroundSecondsCounter = 0;
        createRemoteViews();
      }

      // refresh every second
      long elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(TimeManager.INSTANCE.getElapsedTime());
      if (elapsedTimeInSeconds >= lastSecondNotificationUpdate && TimeManager.INSTANCE.getElapsedTime() != 0) {
        lastSecondNotificationUpdate++;
        remoteViewsWorkaroundSecondsCounter++;

        notifyNotification();
      }
    } else {
      notifyNotification();
    }
  }

  private void notifyNotification() {
    updateRemoteViews(remoteViews);
    getNotificationManager().notify(NOTIFICATION_ID, getNotification());
  }

  private void registerTimeServiceReceiver() {
    timeServiceReceiver = new TimeServiceReceiver(timeServiceReceiverListener);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(AppConstants.ACTIONS.CHANGE_STATE);
    intentFilter.addAction(AppConstants.ACTIONS.REPLAY);
    intentFilter.addAction(AppConstants.ACTIONS.EXIT);
    registerReceiver(timeServiceReceiver, intentFilter);
  }

  private void unregisterTimeServiceReceiver() {
    try {
      unregisterReceiver(timeServiceReceiver);
    } catch (IllegalArgumentException ignored) {
    }
  }

  private void createAudioHelper() {
    audioHelper = new AudioHelper(this, prefsUseMediaStream ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_NOTIFICATION,
        prefs_alertMaxVolumeSound);
  }

  private void releaseAudioHelper() {
    audioHelper.release();
  }

  private void checkAlerts() {
    long countdownTime = TimeManager.INSTANCE.getCountdownTimeMilli();
    long timeLeft = countdownTime - TimeManager.INSTANCE.getElapsedTime();

    if (!TimeManager.INSTANCE.isCountdownOver()) {
      if (prefsHalfwayAlert && countdownTime >= MIN_TIME_TO_DISPATCH_HALFWAYALERT_MILLI && !dispatchedHalfwayAlert) {
        if (TimeManager.INSTANCE.getElapsedTime() >= countdownTime / 2) {
          dispatchedHalfwayAlert = true;
          playAlertShort2(VOLUME_MUTE_HALFWAY_DURATION_MILLI);
        }
      }

      if (prefsTenSecondsAlert && countdownTime > TimeUnit.SECONDS.toMillis(10) && !dispatchedTenSecondsAlert) {
        dispatchedTenSecondsAlert = timeLeft <= TimeUnit.SECONDS.toMillis(10);
        if (dispatchedTenSecondsAlert) {
          playAlertShort(VOLUME_MUTE_TEN_SECONDS_DURATION_MILLI);
        }
      }

      if (prefsThreeSecondsBeep && countdownTime > TimeUnit.SECONDS.toMillis(3)) {
        if (!dispatchedThreeSecondsBeep3) {
          dispatchedThreeSecondsBeep3 = timeLeft <= TimeUnit.SECONDS.toMillis(3);
          if (dispatchedThreeSecondsBeep3) {
            playAlertShort(VOLUME_MUTE_THREE_SECONDS_DURATION_MILLI);
          }
        }

        if (dispatchedThreeSecondsBeep3 && !dispatchedThreeSecondsBeep2) {
          dispatchedThreeSecondsBeep2 = timeLeft <= TimeUnit.SECONDS.toMillis(2);
          if (dispatchedThreeSecondsBeep2) {
            playAlertShort(VOLUME_MUTE_NO_MILLI);
          }
        }

        if (dispatchedThreeSecondsBeep3 && dispatchedThreeSecondsBeep2 && !dispatchedThreeSecondsBeep1) {
          dispatchedThreeSecondsBeep1 = timeLeft <= TimeUnit.SECONDS.toMillis(1);
          if (dispatchedThreeSecondsBeep1) {
            playAlertShort(VOLUME_MUTE_NO_MILLI);
          }
        }
      }
    } else {
      if (!dispatchedAlert) {
        dispatchedAlert = true;
        playAlertLong(dispatchedThreeSecondsBeep3 ? VOLUME_MUTE_NO_MILLI : VOLUME_MUTE_ALERT_DURATION_MILLI);
      }
    }
  }

  private void resetAlertCounters() {
    remoteViewsWorkaroundSecondsCounter = 0;
    lastSecondNotificationUpdate = 0;

    dispatchedAlert = false;
    dispatchedHalfwayAlert = false;
    dispatchedTenSecondsAlert = false;
    dispatchedThreeSecondsBeep1 = false;
    dispatchedThreeSecondsBeep2 = false;
    dispatchedThreeSecondsBeep3 = false;
  }

  private void playAlertShort(long delay) {
    if (prefsSound) {
      if (!prefsUseMediaStream && prefsMuteMediaStream && delay != VOLUME_MUTE_NO_MILLI) {
        audioHelper.toggleMute(delay);
      }
      audioHelper.playShort();
    }
    if (prefsVibrate) {
      vibratorHelper.vibrateShort();
    }
  }

  private void playAlertShort2(long delay) {
    if (prefsSound) {
      if (!prefsUseMediaStream && prefsMuteMediaStream && delay != VOLUME_MUTE_NO_MILLI) {
        audioHelper.toggleMute(delay);
      }
      audioHelper.playShort2();
    }
    if (prefsVibrate) {
      vibratorHelper.vibrateShortTwice();
    }
  }

  private void playAlertLong(long delay) {
    if (prefsSound) {
      if (!prefsUseMediaStream && prefsMuteMediaStream && delay != VOLUME_MUTE_NO_MILLI) {
        audioHelper.toggleMute(delay);
      }
      audioHelper.playLong();
    }
    if (prefsVibrate) {
      vibratorHelper.vibrateLong();
    }
  }

  private void checkVibrateOnClickState() {
    if (prefsVibrateButtons) {
      vibratorHelper.vibrateClick();
    }
  }

  private void playStopwatch() {
    if (prefsSmartStopwatch && TimeManager.INSTANCE.isRunning() && !stopwatchHelper.isRunning()) {
      stopwatchHelper.start();
    }
  }

  private void stopStopwatch() {
    if (stopwatchHelper.isRunning()) {
      if (prefsSmartStopwatch || prefsAutoStopStopwatch) {
        stopwatchHelper.stop(prefsSmartStopwatch);
      } else {
        Toast.makeText(this, getString(R.string.text_stopwatchStillRunning), Toast.LENGTH_LONG).show();
      }
    }
  }

  private final TimeManagerListener timeManagerListener = new TimeManagerListener() {
    @Override public void onStateChanged() {
      resetAlertCounters();
      updateNotification();
    }

    @Override public void onCountdownTimeChanged() {
      resetAlertCounters();
      updateNotification();
    }

    @Override public void onCountdownTick() {
      checkAlerts();
      updateNotification();
    }
  };

  private final TimeServiceReceiver.TimeServiceReceiverListener timeServiceReceiverListener =
      new TimeServiceReceiver.TimeServiceReceiverListener() {
        @Override public void onActionStateChanged() {
          checkVibrateOnClickState();
          TimeManager.INSTANCE.toggle();

          playStopwatch();
        }

        @Override public void onActionReplay() {
          checkVibrateOnClickState();
          TimeManager.INSTANCE.reStart();
        }

        @Override public void onActionExit() {
          TimeManager.INSTANCE.stop();
          stopForeground(true);
          stopSelf();
        }
      };

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
    getPreferences();

    releaseAudioHelper();
    createAudioHelper();
  };
}
