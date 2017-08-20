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

package com.hrules.rest.presentation.presenters.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.hrules.darealmvp.DRMVPPresenter;
import com.hrules.darealmvp.DRMVPView;
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.commons.ZenModeHelper;
import com.hrules.rest.presentation.commons.ResUtils;
import com.hrules.rest.presentation.commons.annotations.Visibility;
import com.hrules.rest.presentation.models.base.Favorite;
import com.hrules.rest.presentation.presenters.activities.extras.CountdownPresenter;
import com.hrules.rest.presentation.presenters.activities.extras.StopwatchPresenter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MainActivityPresenter extends DRMVPPresenter<MainActivityPresenter.Contract> {
  public static final String ACTION_STOPWATCHSTOP = "com.hrules.rest.ACTION_STOPWATCHSTOP";

  private static final long DEFAULT_EDIT_STATE_EMPTY = 0;
  private static final boolean DEFAULT_KEEP_SCREEN_ON_STATE = false;

  private static final int DEFAULT_EXECUTOR_CORE_POOL_SIZE = 1;

  private static final long DEFAULT_STOPWATCH_DELAY_MILLI = 0;
  private static final long DEFAULT_STOPWATCH_PERIOD_MILLI = 33;

  private ResUtils resources;
  private Preferences preferences;
  private boolean prefsVibrateButtons;

  private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());
  private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(DEFAULT_EXECUTOR_CORE_POOL_SIZE);
  private ScheduledFuture scheduledFutureStopwatch;

  private long stopwatchStartTime;

  private VibratorHelper vibratorHelper;
  private ZenModeHelper zenModeHelper;

  private CountdownPresenter countdownPresenter;
  private StopwatchPresenter stopwatchPresenter;

  @Override public void bind(@NonNull Contract view) {
    super.bind(view);
    resources = new ResUtils(App.getAppContext());
    preferences = new Preferences(App.getAppContext());
    vibratorHelper = new VibratorHelper(App.getAppContext());
    zenModeHelper = new ZenModeHelper(App.getAppContext());

    // countdown
    countdownPresenter = new CountdownPresenter();
    countdownPresenter.bind((CountdownPresenter.Contract) view);

    // stopwatch
    stopwatchPresenter = new StopwatchPresenter();
    stopwatchPresenter.bind((StopwatchPresenter.Contract) view);
  }

  public void onViewReady() {
    getPreferences();
    preferences.addListener(sharedPreferenceChangeListener);

    getView().setZenModeAlertVisibility(zenModeHelper.isZenModeActive() ? View.VISIBLE : View.GONE);
    zenModeHelper.setListener(zenModeManagerListener);

    // countdown
    countdownPresenter.onViewReady();
  }

  public void onViewResume() {
    boolean keepScreenOn = preferences.getBoolean(resources.getString(R.string.prefs_displayKeepScreenOnKey),
        resources.getBoolean(R.bool.prefs_displayKeepScreenOnDefault));

    String orientationFromPrefs = preferences.getString(resources.getString(R.string.prefs_displayOrientationKey),
        String.valueOf(resources.getInteger(R.integer.prefs_displayOrientationDefault)));
    String portrait = resources.getString(R.string.prefs_displayOrientationValuesPortrait);
    String landscape = resources.getString(R.string.prefs_displayOrientationValuesLandscape);
    int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    if (orientationFromPrefs.equals(portrait)) {
      orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    } else if (orientationFromPrefs.equals(landscape)) {
      orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }
    getView().setDisplayOptions(keepScreenOn, orientation);

    // countdown
    countdownPresenter.onViewResume();

    // stopwatch
    setStopwatchLastTime();
    manageStopwatchState(preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI));

    registerStopwatchReceiver();
  }

  public void onViewStop() {
    // we will stop UI updates in onViewStop() instead of onViewResume()
    // in order to support Multi Window (API >= 24)
    getView().setDisplayOptions(DEFAULT_KEEP_SCREEN_ON_STATE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    // countdown
    countdownPresenter.onViewStop();

    // stopwatch
    stopStopwatchRunnable();
    unregisterStopwatchReceiver();
  }

  @Override public void unbind() {
    super.unbind();
    zenModeHelper.release();
    zenModeHelper = null;

    // countdown
    countdownPresenter.unbind();

    // stopwatch
    scheduledThreadPoolExecutor.shutdown();
    stopwatchHandler.removeCallbacksAndMessages(null);
  }

  private void getPreferences() {
    // stopwatch
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

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPreferences, key) -> getPreferences();

  private final ZenModeHelper.ZenModeManagerListener zenModeManagerListener =
      newState -> getView().setZenModeAlertVisibility(newState ? View.VISIBLE : View.GONE);

  // region COUNTDOWN
  public void onButtonChangeStateClick() {
    countdownPresenter.onButtonChangeStateClick();
  }

  public void onButtonReplayClick() {
    countdownPresenter.onButtonReplayClick();
  }

  public void onEditorAction(@NonNull TextView v, int actionId, KeyEvent event) {
    countdownPresenter.onEditorAction(v, actionId, event);
  }

  public void onEditFocusChange(@NonNull EditText editText, boolean hasFocus) {
    countdownPresenter.onEditFocusChange(editText, hasFocus);
  }

  public void editAfterTextChanged(@NonNull String editMinutes, @NonNull String editSeconds) {
    countdownPresenter.editAfterTextChanged(editMinutes, editSeconds);
  }

  public void onButtonFavoritesClick(@NonNull String editMinutes, @NonNull String editSeconds) {
    countdownPresenter.onButtonFavoritesClick(editMinutes, editSeconds);
  }

  public void onFavoriteTitleClick(@NonNull Favorite favorite) {
    countdownPresenter.onFavoriteTitleClick(favorite);
  }

  public void onFavoriteActionAddClick(@NonNull String editMinutes, @NonNull String editSeconds) {
    countdownPresenter.onFavoriteActionAddClick(editMinutes, editSeconds);
  }

  public void onFavoriteDeleteClick(@NonNull Favorite favorite) {
    countdownPresenter.onFavoriteDeleteClick(favorite);
  }
  //endregion

  // region STOPWATCH
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

  private final BroadcastReceiver stopwatchReceiver = new BroadcastReceiver() {
    @Override public void onReceive(@NonNull Context context, Intent intent) {
      if (intent.getAction().equals(ACTION_STOPWATCHSTOP)) {
        // update UI
        setStopwatchLastTime();
        manageStopwatchState(AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
      }
    }
  };
  //endregion

  public interface Contract extends DRMVPView {
    void setDisplayOptions(boolean keepScreenOn, int screenOrientationSensor);

    void showTooltip(@IdRes int viewResId, @StringRes int stringResId);

    void setZenModeAlertVisibility(@Visibility int visibility);

    // region STOPWATCH
    void setStopwatchTextSizes(@StyleRes int primaryStyle, @StyleRes int secondaryStyle);

    void updateStopwatch(long milli);

    void setStopwatchTextLastTime(long milli);

    void setStopwatchButtonChangeStateResource(@DrawableRes int resId);
    //endregion
  }
}