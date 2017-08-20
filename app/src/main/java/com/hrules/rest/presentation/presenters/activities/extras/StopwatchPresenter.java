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

package com.hrules.rest.presentation.presenters.activities.extras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import com.hrules.darealmvp.DRMVPPresenter;
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.presentation.commons.ResUtils;
import com.hrules.rest.presentation.presenters.activities.MainActivityPresenter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StopwatchPresenter extends DRMVPPresenter<StopwatchPresenter.Contract> {
  public static final String ACTION_STOPWATCHSTOP = "com.hrules.rest.ACTION_STOPWATCHSTOP";

  private static final long DEFAULT_STOPWATCH_DELAY_MILLI = 0;
  private static final long DEFAULT_STOPWATCH_PERIOD_MILLI = 33;

  private static final int DEFAULT_EXECUTOR_CORE_POOL_SIZE = 1;

  private ResUtils resources;
  private Preferences preferences;
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
  }

  @Override public void unbind() {
    super.unbind();
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

    registerStopwatchReceiver();
  }

  public void onViewStop() {
    stopStopwatchRunnable();
    unregisterStopwatchReceiver();
  }

  private void getPreferences() {
    prefsVibrateButtons = preferences.getBoolean(resources.getString(R.string.prefs_controlVibrateButtonsKey),
        resources.getBoolean(R.bool.prefs_controlVibrateButtonsDefault));

    String stopwatchSize = preferences.getString(resources.getString(R.string.prefs_stopwatchSizeKey),
        String.valueOf(resources.getInteger(R.integer.prefs_stopwatchSizeDefault)));
    String stopwatchSizeNormal = resources.getString(R.string.prefs_stopwatchSizeValuesNormal);
    if (stopwatchSize.equals(stopwatchSizeNormal)) {
      getView().setStopwatchTextSizes(R.style.StopwatchPrimarySizeNormal, R.style.StopwatchSecondarySizeNormal);
    } else {
      getView().setStopwatchTextSizes(R.style.StopwatchPrimarySizeLarge, R.style.StopwatchSecondarySizeLarge);
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
      getView().setStopwatchButtonChangeStateResource(R.drawable.ic_stop_stopwatch);

      stopwatchStartTime = stopwatch;
      startStopwatchRunnable();
    } else {
      // stop countdown
      getView().setStopwatchButtonChangeStateResource(R.drawable.ic_play_stopwatch);

      stopwatchStartTime = AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI;
      stopStopwatchRunnable();
      getView().updateStopwatch(AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
    }
  }

  private void setStopwatchLastTime() {
    getView().setStopwatchTextLastTime(
        preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST));
  }

  private void registerStopwatchReceiver() {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ACTION_STOPWATCHSTOP);
    App.getAppContext().registerReceiver(stopwatchReceiver, intentFilter);
  }

  private void unregisterStopwatchReceiver() {
    try {
      App.getAppContext().unregisterReceiver(stopwatchReceiver);
    } catch (IllegalArgumentException ignored) {
    }
  }

  public void onButtonStopwatchChangeStateClick() {
    long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
    if (stopwatch == AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
      // stopwatch will be started
      checkVibrateOnClickState();

      stopwatch = System.currentTimeMillis();
      preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, stopwatch);

      setStopwatchLastTime();
      manageStopwatchState(stopwatch);
    } else {
      getView().showTooltip(R.id.button_stopwatchChangeState, R.string.text_longClickStopwatch);
    }
  }

  public void onButtonStopwatchChangeStateLongClick() {
    long stopwatch = preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
    if (stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI) {
      //stopwatch will be stopped
      checkVibrateOnClickState();

      preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, System.currentTimeMillis() - stopwatch);

      stopwatch = AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI;
      preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, stopwatch);

      setStopwatchLastTime();
      manageStopwatchState(stopwatch);
    }
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPreferences, key) -> getPreferences();

  private final BroadcastReceiver stopwatchReceiver = new BroadcastReceiver() {
    @Override public void onReceive(@NonNull Context context, Intent intent) {
      if (intent.getAction().equals(ACTION_STOPWATCHSTOP)) {
        // update UI
        setStopwatchLastTime();
        manageStopwatchState(AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
      }
    }
  };

  public interface Contract extends MainActivityPresenter.Contract {
    void setStopwatchTextSizes(@StyleRes int primaryStyle, @StyleRes int secondaryStyle);

    void updateStopwatch(long milli);

    void setStopwatchTextLastTime(long milli);

    void setStopwatchButtonChangeStateResource(@DrawableRes int resId);
  }
}
