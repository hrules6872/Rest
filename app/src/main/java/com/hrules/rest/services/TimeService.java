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
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.alerts.AudioHelper;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.commons.ZenModeHelper;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.presentation.commons.ResUtils;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.presenters.activities.extras.StopwatchPresenter;
import com.hrules.rest.presentation.views.activities.MainActivityView;
import java.util.concurrent.TimeUnit;

public final class TimeService extends Service {
  public static final int NOTIFICATION_ID = 22011982;

  private static final long MIN_TIME_TO_DISPATCH_HALFWAYALERT_MILLI = TimeUnit.MINUTES.toMillis(1) / 2;

  private static final long VOLUME_MUTE_ALERT_DURATION_MILLI = 900;
  private static final long VOLUME_MUTE_HALFWAY_DURATION_MILLI = VOLUME_MUTE_ALERT_DURATION_MILLI;
  private static final long VOLUME_MUTE_TEN_SECONDS_DURATION_MILLI = VOLUME_MUTE_ALERT_DURATION_MILLI / 2;
  private static final long VOLUME_MUTE_THREE_SECONDS_DURATION_MILLI = TimeUnit.SECONDS.toMillis(3) + VOLUME_MUTE_ALERT_DURATION_MILLI;

  private static final long REMOTEVIEWS_WORKAROUND_TRIGGER_SECONDS = 5;

  private NotificationCompat.Builder builder;
  private NotificationManager notificationManager;
  private RemoteViews remoteViewCollapsed;
  private RemoteViews remoteViewExpanded;

  private BroadcastReceiver timeServiceReceiver;

  private Preferences preferences;
  private boolean prefsSound;
  private boolean prefsVibrate;
  private boolean prefsHalfwayAlert;
  private boolean prefsTenSecondsAlert;
  private boolean prefsThreeSecondsBeep;
  private boolean prefsUseMediaStream;
  private boolean prefsMuteMediaStream;
  private boolean prefsVibrateButtons;
  private boolean prefsAutoStopStopwatch;

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

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    vibratorHelper = new VibratorHelper(this);
    zenModeHelper = new ZenModeHelper(this);
    checkIsDNDModeActive();

    preferences = new Preferences(this);
    getPreferences();
    preferences.addListener(sharedPreferenceChangeListener);

    resetAlertCounters();
    TimeManager.INSTANCE.setCountdownTimeMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));

    createAudioHelper();
    registerTimeServiceReceiver();

    builder = new NotificationCompat.Builder(this);
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    createNotification();

    TimeManager.INSTANCE.addListener(timeManagerListener);
    return START_STICKY;
  }

  @Override public void onDestroy() {
    TimeManager.INSTANCE.removeListener(timeManagerListener);

    unregisterTimeServiceReceiver();
    preferences.removeListener(sharedPreferenceChangeListener);
    releaseAudioHelper();

    zenModeHelper.release();
    zenModeHelper = null;

    super.onDestroy();
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
        resources.getBoolean(R.bool.prefs_alertMuteMediaStreamKeyDefault));

    prefsVibrateButtons = preferences.getBoolean(resources.getString(R.string.prefs_controlVibrateButtonsKey),
        resources.getBoolean(R.bool.prefs_controlVibrateButtonsDefault));
    prefsAutoStopStopwatch = preferences.getBoolean(resources.getString(R.string.prefs_stopwatchAutoStopKey),
        resources.getBoolean(R.bool.prefs_stopwatchAutoStopDefault));
  }

  private void createNotification() {
    createRemoteViews();
    startForeground(NOTIFICATION_ID, buildNotification());
  }

  private Notification buildNotification() {
    Intent notificationIntent = new Intent(this, MainActivityView.class);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, PendingIntent.FLAG_UPDATE_CURRENT, notificationIntent, 0);
    builder.setContentIntent(pendingIntent);
    builder.setPriority(NotificationCompat.PRIORITY_MAX);
    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    builder.setOngoing(true);
    builder.setSmallIcon(getSmallIconResId());
    builder.setOnlyAlertOnce(true);
    builder.setContent(remoteViewCollapsed);
    builder.setCustomBigContentView(remoteViewExpanded);
    return builder.build();
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

        internalUpdateNotification();
      }
    } else {
      internalUpdateNotification();
    }
  }

  private void internalUpdateNotification() {
    updateRemoteView(remoteViewCollapsed);
    updateRemoteView(remoteViewExpanded);
    notificationManager.notify(NOTIFICATION_ID, buildNotification());
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

  private void createRemoteViews() {
    remoteViewCollapsed = getRemoteViewCollapsed();
    remoteViewExpanded = getRemoteViewExpanded();
  }

  private RemoteViews getRemoteViewExpanded() {
    RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.layout_notification_expanded);

    updateRemoteView(remoteView);
    setOnClickPendingIntentOnRemoteView(remoteView);
    remoteView.setOnClickPendingIntent(R.id.button_exit,
        PendingIntent.getBroadcast(this, 0, new Intent(TimeServiceReceiver.ACTION_EXIT, null), PendingIntent.FLAG_UPDATE_CURRENT));

    return remoteView;
  }

  private RemoteViews getRemoteViewCollapsed() {
    RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.layout_notification);

    updateRemoteView(remoteView);
    setOnClickPendingIntentOnRemoteView(remoteView);

    return remoteView;
  }

  private void setOnClickPendingIntentOnRemoteView(@NonNull RemoteViews remoteView) {
    remoteView.setOnClickPendingIntent(R.id.button_changeState,
        PendingIntent.getBroadcast(this, 0, new Intent(TimeServiceReceiver.ACTION_CHANGESTATE, null), PendingIntent.FLAG_UPDATE_CURRENT));
    remoteView.setOnClickPendingIntent(R.id.button_replay,
        PendingIntent.getBroadcast(this, 0, new Intent(TimeServiceReceiver.ACTION_REPLAY, null), PendingIntent.FLAG_UPDATE_CURRENT));
  }

  private void updateRemoteView(@NonNull RemoteViews remoteView) {
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
    remoteView.setTextColor(R.id.text_countDown, TimeUtils.getNotificationTextColorFromMilli(new ResUtils(App.getAppContext())));
  }

  private void registerTimeServiceReceiver() {
    timeServiceReceiver = new TimeServiceReceiver(timeServiceReceiverListener);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(TimeServiceReceiver.ACTION_CHANGESTATE);
    intentFilter.addAction(TimeServiceReceiver.ACTION_REPLAY);
    intentFilter.addAction(TimeServiceReceiver.ACTION_EXIT);
    registerReceiver(timeServiceReceiver, intentFilter);
  }

  private void unregisterTimeServiceReceiver() {
    try {
      unregisterReceiver(timeServiceReceiver);
    } catch (IllegalArgumentException ignored) {
    }
  }

  private void createAudioHelper() {
    audioHelper = new AudioHelper(this, prefsUseMediaStream ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_NOTIFICATION);
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
            playAlertShort(0);
          }
        }

        if (dispatchedThreeSecondsBeep3 && dispatchedThreeSecondsBeep2 && !dispatchedThreeSecondsBeep1) {
          dispatchedThreeSecondsBeep1 = timeLeft <= TimeUnit.SECONDS.toMillis(1);
          if (dispatchedThreeSecondsBeep1) {
            playAlertShort(0);
          }
        }
      }
    } else {
      if (!dispatchedAlert) {
        dispatchedAlert = true;
        playAlertLong(dispatchedThreeSecondsBeep3 ? 0 : VOLUME_MUTE_ALERT_DURATION_MILLI);
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
      if (!prefsUseMediaStream && prefsMuteMediaStream && delay != 0) {
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
      if (!prefsUseMediaStream && prefsMuteMediaStream && delay != 0) {
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
      if (!prefsUseMediaStream && prefsMuteMediaStream && delay != 0) {
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

  private void checkStopwatchState() {
    long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
    if (stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
      // stopwatch is running
      if (prefsAutoStopStopwatch) {
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, System.currentTimeMillis() - stopwatch);
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);

        sendBroadcast(new Intent(StopwatchPresenter.ACTION_STOPWATCHSTOP));
      } else {
        Toast.makeText(this, R.string.text_stopwatchStillRunning, Toast.LENGTH_LONG).show();
      }
    }
  }

  private final TimeManager.TimeManagerListener timeManagerListener = new TimeManager.TimeManagerListener() {
    @Override public void onStateChanged() {
      resetAlertCounters();
      updateNotification();
    }

    @Override public void onCountdownTimeChanged() {
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
          updateNotification();
        }

        @Override public void onActionReplay() {
          checkVibrateOnClickState();
          TimeManager.INSTANCE.reStart();
          updateNotification();
        }

        @Override public void onActionExit() {
          TimeManager.INSTANCE.stop();

          checkStopwatchState();

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
