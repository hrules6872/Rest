/*
 * Copyright (c) 2016. Héctor de Isidro - hrules6872
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
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.hrules.darealmvp.DRPresenter;
import com.hrules.darealmvp.DRView;
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.AudioUtils;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.Visibility;
import com.hrules.rest.presentation.models.FavoriteAdd;
import com.hrules.rest.presentation.models.FavoriteSeconds;
import com.hrules.rest.presentation.models.base.Favorite;
import com.hrules.rest.presentation.models.comparators.FavoriteSecondsAscendingComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivityPresenter extends DRPresenter<MainActivityPresenter.MainView> {
  public static final String ACTION_STOPWATCHSTOP = "com.hrules.rest.ACTION_STOPWATCHSTOP";

  private static final long DEFAULT_EDIT_STATE_EMPTY = 0;
  private static final boolean DEFAULT_KEEP_SCREEN_ON_STATE = false;

  private static final int DEFAULT_EXECUTOR_CORE_POOL_SIZE = 1;

  private static final long DEFAULT_STOPWATCH_DELAY_MILLI = 0;
  private static final long DEFAULT_STOPWATCH_PERIOD_MILLI = 33;

  private Preferences preferences;
  private boolean prefsVibrateButtons;

  private final Handler countdownHandler = new Handler(Looper.getMainLooper());
  private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());

  private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
      new ScheduledThreadPoolExecutor(DEFAULT_EXECUTOR_CORE_POOL_SIZE);
  private ScheduledFuture scheduledFutureStopwatch;
  private long stopwatchStartTime;

  private Context appContext;

  @Override public void bind(@NonNull MainView view) {
    super.bind(view);
    appContext = App.getAppContext();
  }

  @Override protected void onViewReady() {
    preferences = new Preferences(appContext);
    getPreferences();
    preferences.addListener(sharedPreferenceChangeListener);

    // countdown
    TimeManager.INSTANCE.setCountdownTime(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));

    long minutes = TimeUtils.getExactMinutesFromMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));
    getView().setEditText(R.id.edit_minutes, String.valueOf(minutes));

    long seconds = TimeUtils.getRemainderSecondsFromMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));
    getView().setEditText(R.id.edit_seconds, TimeUtils.getSecondsFormattedWithLeadingZeros(seconds));
  }

  @Override protected void onResume() {
    Resources resources = appContext.getResources();

    boolean keepScreenOn = preferences.getBoolean(resources.getString(R.string.prefs_displayKeepScreenOnKey),
        resources.getBoolean(R.bool.prefs_displayKeepScreenOnDefault));

    String orientationFromPrefs = preferences.getString(resources.getString(R.string.prefs_displayOrientationKey),
        String.valueOf(resources.getInteger(R.integer.prefs_displayOrientationDefault)));
    String portrait = resources.getString(R.string.prefs_displayOrientationValuesPortrait);
    String landscape = resources.getString(R.string.prefs_displayOrientationValuesLandscape);
    int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    if (orientationFromPrefs.equals(portrait)) {
      // portrait
      orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    } else if (orientationFromPrefs.equals(landscape)) {
      // landscape
      orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }
    getView().setDisplayOptions(keepScreenOn, orientation);

    // countdown
    getView().startServiceIfNotRunning();
    internalOnStateChanged(false);
    updateButtonChangeStateEnabled();

    TimeManager.INSTANCE.addListener(timeManagerListener);

    // stopwatch
    setStopwatchLastTime();
    manageStopwatchState(
        preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI));

    registerStopwatchReceiver();
  }

  @Override protected void onPause() {
    // we will stop UI updates in onStop() instead of onResume()
    // in order to support Multi Window (API >= 24)
  }

  @Override protected void onStop() {
    getView().setDisplayOptions(DEFAULT_KEEP_SCREEN_ON_STATE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    // countdown
    getView().hideSoftKeyboardAndClearEditFocus();
    TimeManager.INSTANCE.removeListener(timeManagerListener);

    // stopwatch
    stopStopwatchRunnable();
    unregisterStopwatchReceiver();
  }

  @Override protected void onDestroy() {
    TimeManager.INSTANCE.removeListener(timeManagerListener);
    preferences.removeListener(sharedPreferenceChangeListener);

    scheduledThreadPoolExecutor.shutdown();

    countdownHandler.removeCallbacksAndMessages(null);
    stopwatchHandler.removeCallbacksAndMessages(null);

    super.onDestroy();
  }

  public void onMenuItemClick(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_preferences:
        getView().launchPreferencesActivity();
        break;

      case R.id.menu_closeNotification:
        getView().closeNotification();
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  private void getPreferences() {
    Resources resources = appContext.getResources();
    prefsVibrateButtons = preferences.getBoolean(resources.getString(R.string.prefs_controlVibrateButtonsKey),
        resources.getBoolean(R.bool.prefs_controlVibrateButtonsDefault));
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPreferences, key) -> getPreferences();

  // region COUNTDOWN
  private void checkVibrateOnClickState() {
    if (prefsVibrateButtons) {
      VibratorHelper.vibrateClick(appContext);
    }
  }

  private void internalOnStateChanged(boolean animate) {
    getView().updateCountdown(animate);
    getView().setEditTextsEnabled(!TimeManager.INSTANCE.isRunning());
    getView().setMessageAlertVisibility(AudioUtils.isDoNotDisturbActive() ? View.VISIBLE : View.GONE);

    if (TimeManager.INSTANCE.isRunning()) {
      // start countdown
      getView().startServiceIfNotRunning();

      getView().setProgressViewAttributes(View.INVISIBLE, TimeManager.INSTANCE.getCountdownTime(),
          TimeManager.INSTANCE.getElapsedTime());
      updateStopButtonColor(animate);
      getView().setButtonReplayVisibility(animate, View.VISIBLE);
    } else {
      // stop countdown
      getView().setButtonChangeStateAttributes(animate, R.drawable.ic_play_fab, R.color.fab_playBackground);
      getView().setButtonReplayVisibility(animate, View.INVISIBLE);
    }
  }

  public void onButtonChangeStateClick() {
    getView().hideSoftKeyboardAndClearEditFocus();
    checkVibrateOnClickState();
    TimeManager.INSTANCE.toggle();
  }

  public void onButtonReplayClick() {
    checkVibrateOnClickState();
    TimeManager.INSTANCE.reStart();
  }

  private void internalOnCountdownTimeChanged() {
    updateButtonChangeStateEnabled();
    getView().updateCountdown(false);
  }

  private void updateButtonChangeStateEnabled() {
    boolean state = TimeManager.INSTANCE.getCountdownTime() != 0;
    getView().setButtonChangeStateEnabled(state);
    if (state) {
      getView().setButtonChangeStateAttributes(false, R.drawable.ic_play_fab, R.color.fab_playBackground);
    } else {
      getView().setButtonChangeStateAttributes(false, R.drawable.ic_play_fab, R.color.fab_playDisabledBackground);
    }
  }

  private void updateStopButtonColor(boolean animate) {
    if (TimeManager.INSTANCE.isCountdownOver()) {
      getView().setButtonChangeStateAttributes(false, R.drawable.ic_stop_over_fab, R.color.fab_stopOverBackground);
    } else {
      getView().setButtonChangeStateAttributes(animate, R.drawable.ic_stop_fab, R.color.fab_stopBackground);
    }
  }

  @SuppressWarnings("UnusedParameters") public void onEditorAction(@NonNull TextView v, int actionId, KeyEvent event) {
    getView().hideSoftKeyboardAndClearEditFocus();
  }

  public void onEditFocusChange(@NonNull EditText editText, boolean hasFocus) {
    if (!hasFocus) {
      String text = editText.getText().toString();

      if (text.isEmpty()) {
        if (editText.getId() == R.id.edit_seconds) {
          getView().setEditText(R.id.edit_seconds,
              TimeUtils.getSecondsFormattedWithLeadingZeros(DEFAULT_EDIT_STATE_EMPTY));
        } else {
          getView().setEditText(R.id.edit_minutes, String.valueOf(DEFAULT_EDIT_STATE_EMPTY));
        }
      } else {
        if (editText.getId() == R.id.edit_seconds) {
          getView().setEditText(R.id.edit_seconds, TimeUtils.getSecondsFormattedWithLeadingZeros(Long.valueOf(text)));
        }
      }
    }
  }

  public void editAfterTextChanged(@NonNull EditText editMinutes, @NonNull EditText editSeconds) {
    long minutes;
    long seconds;

    if (editMinutes.getText().toString().isEmpty()) {
      minutes = DEFAULT_EDIT_STATE_EMPTY;
    } else {
      minutes = Long.parseLong(editMinutes.getText().toString());
    }

    if (editSeconds.getText().toString().isEmpty()) {
      seconds = DEFAULT_EDIT_STATE_EMPTY;
    } else {
      seconds = Long.parseLong(editSeconds.getText().toString());
      if (seconds > TimeUnit.MINUTES.toSeconds(1) - 1) {
        seconds = TimeUnit.MINUTES.toSeconds(1) - 1;
        getView().setEditText(R.id.edit_seconds, String.valueOf(seconds));
      }
    }

    preferences.save(AppConstants.PREFS.COUNTDOWN_MILLI, TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
    TimeManager.INSTANCE.setCountdownTime(TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
  }

  public void onButtonFavoritesClick(@NonNull EditText editMinutes, @NonNull EditText editSeconds) {
    Resources resources = appContext.getResources();
    List<Favorite> favorites = new ArrayList<>();

    Set<String> favoritesSet =
        new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));
    for (String secondsFromStringSet : favoritesSet) {
      long milli = Integer.valueOf(secondsFromStringSet) * TimeUnit.SECONDS.toMillis(1);
      int seconds = Integer.valueOf(secondsFromStringSet);
      favorites.add(new FavoriteSeconds(TimeUtils.milliToFavoriteMinutesSecondsString(milli, resources), seconds));
    }
    Collections.sort(favorites, new FavoriteSecondsAscendingComparator());

    if (!editMinutes.getText().toString().isEmpty() && !editSeconds.getText().toString().isEmpty()) {
      long minutes = Long.parseLong(editMinutes.getText().toString());
      long seconds = Long.parseLong(editSeconds.getText().toString());
      String totalSeconds = String.valueOf(TimeUnit.MINUTES.toSeconds(minutes) + seconds);
      if (!favoritesSet.contains(totalSeconds) && !"0".equals(totalSeconds)) {
        favorites.add(0, new FavoriteAdd(resources.getString(R.string.text_addFavorite)));
      }
    }

    getView().showPopupFavorites(favorites);
  }

  public void onFavoriteTitleClick(@NonNull Favorite favorite) {
    long milli = favorite.getSeconds() * TimeUnit.SECONDS.toMillis(1);

    long minutes = TimeUtils.getExactMinutesFromMilli(milli);
    getView().setEditText(R.id.edit_minutes, String.valueOf(minutes));

    long seconds = TimeUtils.getRemainderSecondsFromMilli(milli);
    getView().setEditText(R.id.edit_seconds, TimeUtils.getSecondsFormattedWithLeadingZeros(seconds));

    preferences.save(AppConstants.PREFS.COUNTDOWN_MILLI, TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
    TimeManager.INSTANCE.setCountdownTime(TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
  }

  public void onFavoriteDeleteClick(@NonNull Favorite favorite) {
    Set<String> favoritesSet =
        new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));
    favoritesSet.remove(String.valueOf(favorite.getSeconds()));

    preferences.save(AppConstants.PREFS.FAVORITES, favoritesSet);
  }

  public void onFavoriteActionAddClick(@NonNull EditText editMinutes, @NonNull EditText editSeconds) {
    Set<String> favoritesSet =
        new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));

    long minutes = Long.parseLong(editMinutes.getText().toString());
    long seconds = Long.parseLong(editSeconds.getText().toString());
    String secondsStringToSave = String.valueOf(
        Math.round(TimeUtils.getMilliFromMinutesSecond(minutes, seconds) / TimeUnit.SECONDS.toMillis(1)));
    favoritesSet.add(secondsStringToSave);

    preferences.save(AppConstants.PREFS.FAVORITES, favoritesSet);
  }

  private final TimeManager.TimeManagerListener timeManagerListener = new TimeManager.TimeManagerListener() {
    @Override public void onStateChanged() {
      internalOnStateChanged(true);
    }

    @Override public void onCountdownTimeChanged() {
      internalOnCountdownTimeChanged();
    }

    @Override public void onCountdownTick() {
      countdownHandler.post(() -> {
        // animation workaround
        if (TimeManager.INSTANCE.getElapsedTime() >= TimeUnit.SECONDS.toMillis(1)) {
          updateStopButtonColor(false);
        }
        getView().updateCountdown(false);
      });
    }
  };
  //endregion

  // region STOPWATCH
  private void startStopwatchRunnable() {
    stopStopwatchRunnable();
    scheduledFutureStopwatch =
        scheduledThreadPoolExecutor.scheduleAtFixedRate(updateStopwatchRunnable, DEFAULT_STOPWATCH_DELAY_MILLI,
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
          stopwatchStartTime != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI ? stopwatch
              : AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI));
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
    long stopwatch =
        preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
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
    long stopwatch =
        preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
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
    appContext.registerReceiver(stopwatchReceiver, intentFilter);
  }

  private void unregisterStopwatchReceiver() {
    try {
      appContext.unregisterReceiver(stopwatchReceiver);
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

  public interface MainView extends DRView {
    void launchPreferencesActivity();

    void closeNotification();

    void setDisplayOptions(boolean keepScreenOn, int screenOrientationSensor);

    void showTooltip(@IdRes int viewResId, @StringRes int stringResId);

    void setMessageAlertVisibility(@Visibility int visibility);

    // COUNTDOWN
    void startServiceIfNotRunning();

    void updateCountdown(boolean animate);

    void setButtonChangeStateAttributes(boolean animate, @DrawableRes int drawableResId, @ColorRes int colorResId);

    void setButtonChangeStateEnabled(boolean enabled);

    void setProgressViewAttributes(@Visibility int visibility, long maxProgress, long currentProgress);

    void setButtonReplayVisibility(boolean animate, @Visibility int visibility);

    void hideSoftKeyboardAndClearEditFocus();

    void setEditTextsEnabled(boolean enabled);

    void setEditText(@IdRes int editTextResId, @NonNull String text);

    void showPopupFavorites(@NonNull List<Favorite> favorites);

    // STOPWATCH
    void updateStopwatch(long milli);

    void setStopwatchTextLastTime(long milli);

    void setStopwatchButtonChangeStateResource(@DrawableRes int resId);
  }
}