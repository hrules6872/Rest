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

package com.hrules.rest.presentation.presenters.extras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import com.hrules.darealmvp.DRMVPPresenter;
import com.hrules.darealmvp.DRMVPView;
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.core.time.TimeManagerListener;
import com.hrules.rest.presentation.commons.DateUtils;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.components.StopwatchTimeLayout;
import com.hrules.rest.presentation.commons.extensions.StringSpan;
import com.hrules.rest.presentation.commons.helpers.StopwatchHelper;
import com.hrules.rest.presentation.commons.resources.ResBoolean;
import com.hrules.rest.presentation.commons.resources.ResId;
import com.hrules.rest.presentation.commons.resources.ResInteger;
import com.hrules.rest.presentation.commons.resources.ResString;
import com.hrules.rest.presentation.commons.resources.base.ResWrapper;
import com.hrules.rest.presentation.commons.threads.UIHandler;
import com.hrules.rest.presentation.commons.threads.base.Handler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.hrules.rest.services.TimeService.ACTION_SERVICE_SHUTDOWN;

public final class StopwatchPresenter extends DRMVPPresenter<StopwatchPresenter.Contract> {
  private static final long DEFAULT_STOPWATCH_DELAY_MILLI = 0;
  private static final long DEFAULT_STOPWATCH_PERIOD_MILLI = 33;

  private static final int DEFAULT_EXECUTOR_CORE_POOL_SIZE = 1;

  private StopwatchHelper stopwatchHelper;

  private Preferences preferences;
  private boolean prefsSmartStopwatch;
  private boolean prefsAutoStopStopwatch;
  private boolean prefsVibrateButtons;

  private VibratorHelper vibratorHelper;

  private final Handler stopwatchHandler = new UIHandler();
  private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(DEFAULT_EXECUTOR_CORE_POOL_SIZE);
  private ScheduledFuture scheduledFutureStopwatch;

  private long stopwatchStartTime;

  @Override public void bind(@NonNull Contract view) {
    super.bind(view);
    preferences = new Preferences(App.getAppContext());
    stopwatchHelper = new StopwatchHelper(preferences);
    vibratorHelper = new VibratorHelper(App.getAppContext());

    registerStopwatchReceiver();
    TimeManager.INSTANCE.addListener(timeManagerListener);
  }

  @Override public void unbind() {
    super.unbind();
    unregisterStopwatchReceiver();
    TimeManager.INSTANCE.removeListener(timeManagerListener);

    scheduledThreadPoolExecutor.shutdown();
    stopwatchHandler.removeCallbacksAndMessages(null);
  }

  public void onViewReady() {
    getPreferences();
    preferences.addListener(sharedPreferenceChangeListener);
  }

  public void onViewResume() {
    setStopwatchLastTime();
    manageStopwatchState(stopwatchHelper.getStopwatchMilli());
  }

  public void onViewStop() {
    stopStopwatchRunnable();
  }

  private void getPreferences() {
    prefsSmartStopwatch = preferences.getBoolean(ResString.getPrefs_stopwatchSmartKey(), ResBoolean.getPrefs_stopwatchSmartDefault());
    getView().setStopwatchButtonChangeStateSmart(prefsSmartStopwatch, stopwatchHelper.isRunning());

    prefsAutoStopStopwatch =
        preferences.getBoolean(ResString.getPrefs_stopwatchAutoStopKey(), ResBoolean.getPrefs_stopwatchAutoStopDefault());

    prefsVibrateButtons =
        preferences.getBoolean(ResString.getPrefs_controlVibrateButtonsKey(), ResBoolean.getPrefs_controlVibrateButtonsDefault());

    String stopwatchSize =
        preferences.getString(ResString.getPrefs_stopwatchSizeKey(), String.valueOf(ResInteger.getPrefs_stopwatchSizeDefault()));
    String stopwatchSizeNormal = ResString.getPrefs_stopwatchSizeValuesNormal();
    if (stopwatchSize.equals(stopwatchSizeNormal)) {
      getView().setStopwatchTimeSize(StopwatchTimeLayout.STOPWATCH_SIZE_NORMAL);
    } else {
      getView().setStopwatchTimeSize(StopwatchTimeLayout.STOPWATCH_SIZE_LARGE);
    }
  }

  private void checkVibrateOnClickState() {
    if (prefsVibrateButtons) {
      vibratorHelper.vibrateClick();
    }
  }

  private void startStopwatchRunnable() {
    stopStopwatchRunnable();
    scheduledFutureStopwatch = scheduledThreadPoolExecutor.scheduleAtFixedRate(updateStopwatchRunnable, DEFAULT_STOPWATCH_DELAY_MILLI,
        DEFAULT_STOPWATCH_PERIOD_MILLI, TimeUnit.MILLISECONDS);
  }

  private void stopStopwatchRunnable() {
    if (scheduledFutureStopwatch != null) {
      scheduledFutureStopwatch.cancel(false);
    }
  }

  private final Runnable updateStopwatchRunnable = new Runnable() {
    @Override public void run() {
      final long stopwatch = System.currentTimeMillis() - stopwatchStartTime;
      stopwatchHandler.post(() -> getView().updateStopwatch(
          stopwatchStartTime != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI ? stopwatch : AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI));
    }
  };

  private void manageStopwatchState(long stopwatch) {
    if (stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
      // start countdown
      getView().setStopwatchButtonChangeStatePlaying(true);

      stopwatchStartTime = stopwatch;
      startStopwatchRunnable();
    } else {
      // stop countdown
      getView().setStopwatchButtonChangeStatePlaying(false);

      stopwatchStartTime = AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI;
      stopStopwatchRunnable();

      getView().updateStopwatch(AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
    }
  }

  private void setStopwatchLastTime() {
    CharSequence text;
    if (prefsSmartStopwatch) {
      if (!stopwatchHelper.isRunning() && !DateUtils.isToday(
          preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_BACKUP, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_BACKUP))) {
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST_BACKUP, stopwatchHelper.getStopwatchMilliLast());
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST);
      }
      long current = stopwatchHelper.getStopwatchMilliLast();
      long last =
          preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST_BACKUP, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST_BACKUP);

      text = StringSpan.format(ResString.getText_smartStopWatchFormatted(),
          TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(current, ResWrapper.getResources()),
          TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(last, ResWrapper.getResources()));
    } else {
      long last = stopwatchHelper.getStopwatchMilliLast();
      text = TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(last, ResWrapper.getResources());
    }

    getView().setStopwatchTimeLastTime(text);
  }

  private void play() {
    stopwatchHelper.play();

    setStopwatchLastTime();
    manageStopwatchState(stopwatchHelper.getStopwatchMilli());
  }

  private void stop() {
    stopwatchHelper.stop(prefsSmartStopwatch);

    setStopwatchLastTime();
    manageStopwatchState(AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
  }

  private void registerStopwatchReceiver() {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ACTION_SERVICE_SHUTDOWN);
    App.getAppContext().registerReceiver(stopwatchReceiver, intentFilter);
  }

  private void unregisterStopwatchReceiver() {
    try {
      App.getAppContext().unregisterReceiver(stopwatchReceiver);
    } catch (IllegalArgumentException ignored) {
    }
  }

  public void onButtonStopwatchChangeStateClick() {
    if (!prefsSmartStopwatch) {
      if (stopwatchHelper.isRunning()) {
        getView().showTooltip(ResId.getButton_stopwatchChangeState(), ResString.getText_longClickStopwatch());
      } else {
        // stopwatch will be started
        checkVibrateOnClickState();
        play();
      }
    }
  }

  public void onButtonStopwatchChangeStateLongClick() {
    if (!prefsSmartStopwatch) {
      if (stopwatchHelper.isRunning()) {
        // stopwatch will be stopped
        checkVibrateOnClickState();
        stop();
      }
    }
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPreferences, key) -> getPreferences();

  private final TimeManagerListener timeManagerListener = new TimeManagerListener() {
    @Override public void onStateChanged() {
      if (prefsSmartStopwatch && TimeManager.INSTANCE.isRunning() && !stopwatchHelper.isRunning()) {
        play();
      }
    }
  };

  private final BroadcastReceiver stopwatchReceiver = new BroadcastReceiver() {
    @Override public void onReceive(@NonNull Context context, Intent intent) {
      if (intent.getAction().equals(ACTION_SERVICE_SHUTDOWN)) {
        if (prefsSmartStopwatch || prefsAutoStopStopwatch) {
          setStopwatchLastTime();
          manageStopwatchState(AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
        }
      }
    }
  };

  public interface Contract extends DRMVPView {
    void showTooltip(@IdRes int viewResId, @NonNull String message);

    void updateStopwatch(long milli);

    void setStopwatchTimeLastTime(@NonNull CharSequence text);

    void setStopwatchTimeSize(@StopwatchTimeLayout.Size int size);

    void setStopwatchButtonChangeStateSmart(boolean smart, boolean playing);

    void setStopwatchButtonChangeStatePlaying(boolean playing);
  }
}