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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.format.DateUtils;
import com.hrules.darealmvp.DRMVPPresenter;
import com.hrules.darealmvp.DRMVPView;
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.core.time.TimeManagerListener;
import com.hrules.rest.presentation.commons.ResUtils;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.components.StopwatchTimeLayout;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.hrules.rest.services.TimeService.ACTION_SERVICE_SHUTDOWN;

public class StopwatchPresenter extends DRMVPPresenter<StopwatchPresenter.Contract> {
  private static final long DEFAULT_STOPWATCH_DELAY_MILLI = 0;
  private static final long DEFAULT_STOPWATCH_PERIOD_MILLI = 33;

  private static final int DEFAULT_EXECUTOR_CORE_POOL_SIZE = 1;

  private ResUtils resources;
  private Preferences preferences;
  private boolean prefsSmartStopwatch;
  private boolean prefsAutoStopStopwatch;
  private boolean prefsVibrateButtons;

  private VibratorHelper vibratorHelper;

  private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());
  private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(DEFAULT_EXECUTOR_CORE_POOL_SIZE);
  private ScheduledFuture scheduledFutureStopwatch;

  private long stopwatchStartTime;

  @Override public void bind(@NonNull Contract view) {
    super.bind(view);
    resources = new ResUtils(App.getAppContext());
    preferences = new Preferences(App.getAppContext());
    vibratorHelper = new VibratorHelper(App.getAppContext());

    registerStopwatchReceiver();
    TimeManager.INSTANCE.addListener(timeManagerListener);
  }

  @Override public void unbind() {
    super.unbind();
    long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
    if (stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
      stop();
    }

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
    manageStopwatchState(preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI));
  }

  public void onViewStop() {
    stopStopwatchRunnable();
  }

  private void getPreferences() {
    prefsSmartStopwatch = preferences.getBoolean(resources.getString(R.string.prefs_stopwatchSmartKey),
        resources.getBoolean(R.bool.prefs_stopwatchSmartDefault));
    boolean playing = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI)
        != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI;
    getView().setStopwatchButtonChangeStateSmart(prefsSmartStopwatch, playing);

    prefsAutoStopStopwatch = preferences.getBoolean(resources.getString(R.string.prefs_stopwatchAutoStopKey),
        resources.getBoolean(R.bool.prefs_stopwatchAutoStopDefault));

    prefsVibrateButtons = preferences.getBoolean(resources.getString(R.string.prefs_controlVibrateButtonsKey),
        resources.getBoolean(R.bool.prefs_controlVibrateButtonsDefault));

    String stopwatchSize = preferences.getString(resources.getString(R.string.prefs_stopwatchSizeKey),
        String.valueOf(resources.getInteger(R.integer.prefs_stopwatchSizeDefault)));
    String stopwatchSizeNormal = resources.getString(R.string.prefs_stopwatchSizeValuesNormal);
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
    String text;
    if (prefsSmartStopwatch) {
      long current = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST);
      long last =
          preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST_BACKUP, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST_BACKUP);

      text = String.format(Locale.getDefault(), resources.getString(R.string.text_smartStopWatchFormatted),
          TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(current, resources.getResources()),
          TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(last, resources.getResources()));
    } else {
      long last = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST);
      text = String.valueOf(TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(last, resources.getResources()));
    }
    getView().setStopwatchTimeLastTime(text);
  }

  private void play() {
    long stopwatch = System.currentTimeMillis();
    preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, stopwatch);

    setStopwatchLastTime();
    manageStopwatchState(stopwatch);
  }

  private void stop() {
    if (prefsSmartStopwatch) {
      long last = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST);
      if (DateUtils.isToday(stopwatchStartTime)) {
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, last + (System.currentTimeMillis() - stopwatchStartTime));
      } else {
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST_BACKUP, last + (System.currentTimeMillis() - stopwatchStartTime));
        preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST);
      }
    } else {
      preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, System.currentTimeMillis() - stopwatchStartTime);
    }
    preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);

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
      long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
      if (stopwatch == AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
        // stopwatch will be started
        checkVibrateOnClickState();
        play();
      } else {
        getView().showTooltip(R.id.button_stopwatchChangeState, R.string.text_longClickStopwatch);
      }
    }
  }

  public void onButtonStopwatchChangeStateLongClick() {
    if (!prefsSmartStopwatch) {
      long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
      if (stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
        //stopwatch will be stopped
        checkVibrateOnClickState();
        stop();
      }
    }
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPreferences, key) -> getPreferences();

  private final TimeManagerListener timeManagerListener = new TimeManagerListener() {
    @Override public void onStateChanged() {
      long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
      if (prefsSmartStopwatch && TimeManager.INSTANCE.isRunning() && (stopwatch == AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI)) {
        play();
      }
    }
  };

  private final BroadcastReceiver stopwatchReceiver = new BroadcastReceiver() {
    @Override public void onReceive(@NonNull Context context, Intent intent) {
      if (intent.getAction().equals(ACTION_SERVICE_SHUTDOWN)) {
        long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
        if (stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
          // stopwatch is running
          if (prefsSmartStopwatch | prefsAutoStopStopwatch) {
            stop();
          } else {
            getView().showToast(R.string.text_stopwatchStillRunning);
          }
        }
      }
    }
  };

  public interface Contract extends DRMVPView {
    void showToast(@StringRes int stringResId);

    void showTooltip(@IdRes int viewResId, @StringRes int stringResId);

    void updateStopwatch(long milli);

    void setStopwatchTimeLastTime(@NonNull String text);

    void setStopwatchTimeSize(@StopwatchTimeLayout.Size int size);

    void setStopwatchButtonChangeStateSmart(boolean smart, boolean playing);

    void setStopwatchButtonChangeStatePlaying(boolean playing);
  }
}
