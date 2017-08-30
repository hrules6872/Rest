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

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
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
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.core.time.TimeManagerListener;
import com.hrules.rest.presentation.commons.ResUtils;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.annotations.Visibility;
import com.hrules.rest.presentation.models.FavoriteAdd;
import com.hrules.rest.presentation.models.FavoriteSeconds;
import com.hrules.rest.presentation.models.base.Favorite;
import com.hrules.rest.presentation.models.comparators.FavoriteSecondsAscendingComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CountdownPresenter extends DRMVPPresenter<CountdownPresenter.Contract> {
  private static final long DEFAULT_EDIT_STATE_EMPTY = 0;

  private ResUtils resources;
  private Preferences preferences;
  private boolean prefsVibrateButtons;

  private VibratorHelper vibratorHelper;

  private final Handler countdownHandler = new Handler(Looper.getMainLooper());

  @Override public void bind(@NonNull Contract view) {
    super.bind(view);
    resources = new ResUtils(App.getAppContext());
    preferences = new Preferences(App.getAppContext());
    vibratorHelper = new VibratorHelper(App.getAppContext());
  }

  @Override public void unbind() {
    super.unbind();
    TimeManager.INSTANCE.removeListener(timeManagerListener);
    preferences.removeListener(sharedPreferenceChangeListener);

    countdownHandler.removeCallbacksAndMessages(null);
  }

  public void onViewReady() {
    getPreferences();
    preferences.addListener(sharedPreferenceChangeListener);

    TimeManager.INSTANCE.setCountdownTimeMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));

    long minutes = TimeUtils.getExactMinutesFromMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));
    getView().setEditText(R.id.edit_minutes, String.valueOf(minutes));

    long seconds = TimeUtils.getRemainderSecondsFromMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));
    getView().setEditText(R.id.edit_seconds, TimeUtils.getSecondsFormattedWithLeadingZeros(seconds));
  }

  public void onViewResume() {
    getView().startService();
    internalOnStateChanged(false);
    updateButtonChangeStateEnabled();

    TimeManager.INSTANCE.addListener(timeManagerListener);
  }

  public void onViewStop() {
    getView().hideSoftKeyboardAndClearEditFocus();
    TimeManager.INSTANCE.removeListener(timeManagerListener);
  }

  private void getPreferences() {
    prefsVibrateButtons = preferences.getBoolean(resources.getString(R.string.prefs_controlVibrateButtonsKey),
        resources.getBoolean(R.bool.prefs_controlVibrateButtonsDefault));
  }

  private void checkVibrateOnClickState() {
    if (prefsVibrateButtons) {
      vibratorHelper.vibrateClick();
    }
  }

  private void internalOnStateChanged(boolean animate) {
    getView().updateCountdown(animate);
    getView().setEditTextsEnabled(!TimeManager.INSTANCE.isRunning());

    if (TimeManager.INSTANCE.isRunning()) {
      // start countdown
      getView().startService();

      getView().setProgressViewAttributes(View.INVISIBLE, TimeManager.INSTANCE.getCountdownTimeMilli(),
          TimeManager.INSTANCE.getElapsedTime());
      updateStopButtonColor(animate);
      getView().setButtonReplayVisibility(animate, View.VISIBLE);
    } else {
      // stop countdown
      getView().setButtonChangeStateAttributes(animate, R.drawable.ic_play_fab, R.color.fab_playBackground);
      getView().setButtonReplayVisibility(animate, View.INVISIBLE);
    }
  }

  private void internalOnCountdownTimeChanged() {
    updateButtonChangeStateEnabled();
    getView().updateCountdown(false);
  }

  private void updateButtonChangeStateEnabled() {
    boolean state = TimeManager.INSTANCE.getCountdownTimeMilli() != 0;
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

  public void onButtonChangeStateClick() {
    getView().hideSoftKeyboardAndClearEditFocus();
    checkVibrateOnClickState();
    TimeManager.INSTANCE.toggle();
  }

  public void onButtonReplayClick() {
    checkVibrateOnClickState();
    TimeManager.INSTANCE.reStart();
  }

  @SuppressWarnings("UnusedParameters") public void onEditorAction(@NonNull TextView v, int actionId, KeyEvent event) {
    getView().hideSoftKeyboardAndClearEditFocus();
  }

  public void onEditFocusChange(@NonNull EditText editText, boolean hasFocus) {
    if (!hasFocus) {
      String text = editText.getText().toString();

      if (text.isEmpty()) {
        if (editText.getId() == R.id.edit_seconds) {
          getView().setEditText(R.id.edit_seconds, TimeUtils.getSecondsFormattedWithLeadingZeros(DEFAULT_EDIT_STATE_EMPTY));
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

  public void editAfterTextChanged(@NonNull String editMinutes, @NonNull String editSeconds) {
    long minutes;
    long seconds;

    try {
      if (TextUtils.isEmpty(editMinutes)) {
        minutes = DEFAULT_EDIT_STATE_EMPTY;
      } else {
        minutes = Long.parseLong(editMinutes);
      }

      if (TextUtils.isEmpty(editSeconds)) {
        seconds = DEFAULT_EDIT_STATE_EMPTY;
      } else {
        seconds = Long.parseLong(editSeconds);
        if (seconds > TimeUnit.MINUTES.toSeconds(1) - 1) {
          seconds = TimeUnit.MINUTES.toSeconds(1) - 1;
          getView().setEditText(R.id.edit_seconds, String.valueOf(seconds));
        }
      }

      preferences.save(AppConstants.PREFS.COUNTDOWN_MILLI, TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
      TimeManager.INSTANCE.setCountdownTimeMilli(TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
    } catch (NumberFormatException ignored) {
    }
  }

  public void onButtonFavoritesClick(@NonNull String editMinutes, @NonNull String editSeconds) {
    List<Favorite> favorites = new ArrayList<>();

    Set<String> favoritesSet = new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));
    for (String secondsFromStringSet : favoritesSet) {
      long milli = Integer.valueOf(secondsFromStringSet) * TimeUnit.SECONDS.toMillis(1);
      int seconds = Integer.valueOf(secondsFromStringSet);
      favorites.add(new FavoriteSeconds(TimeUtils.milliToFavoriteMinutesSecondsString(milli, resources.getResources()), seconds));
    }
    Collections.sort(favorites, new FavoriteSecondsAscendingComparator());

    try {
      if (!TextUtils.isEmpty(editMinutes) && !TextUtils.isEmpty(editMinutes)) {
        long minutes = Long.parseLong(editMinutes);
        long seconds = Long.parseLong(editSeconds);
        String totalSeconds = String.valueOf(TimeUnit.MINUTES.toSeconds(minutes) + seconds);
        if (!favoritesSet.contains(totalSeconds) && !"0".equals(totalSeconds)) {
          favorites.add(0, new FavoriteAdd(resources.getString(R.string.text_addFavorite)));
        }
      }

      getView().showPopupFavorites(favorites);
    } catch (NumberFormatException ignored) {
    }
  }

  public void onFavoriteTitleClick(@NonNull Favorite favorite) {
    long milli = favorite.getSeconds() * TimeUnit.SECONDS.toMillis(1);

    long minutes = TimeUtils.getExactMinutesFromMilli(milli);
    getView().setEditText(R.id.edit_minutes, String.valueOf(minutes));

    long seconds = TimeUtils.getRemainderSecondsFromMilli(milli);
    getView().setEditText(R.id.edit_seconds, TimeUtils.getSecondsFormattedWithLeadingZeros(seconds));

    preferences.save(AppConstants.PREFS.COUNTDOWN_MILLI, TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
    TimeManager.INSTANCE.setCountdownTimeMilli(TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
  }

  public void onFavoriteActionAddClick(@NonNull String editMinutes, @NonNull String editSeconds) {
    Set<String> favoritesSet = new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));

    try {
      long minutes = Long.parseLong(editMinutes);
      long seconds = Long.parseLong(editSeconds);
      String secondsStringToSave =
          String.valueOf(Math.round(TimeUtils.getMilliFromMinutesSecond(minutes, seconds) / TimeUnit.SECONDS.toMillis(1)));
      favoritesSet.add(secondsStringToSave);

      preferences.save(AppConstants.PREFS.FAVORITES, favoritesSet);
    } catch (Exception ignored) {
    }
  }

  public void onFavoriteDeleteClick(@NonNull Favorite favorite) {
    Set<String> favoritesSet = new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));
    favoritesSet.remove(String.valueOf(favorite.getSeconds()));

    preferences.save(AppConstants.PREFS.FAVORITES, favoritesSet);
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (sharedPreferences, key) -> getPreferences();

  private final TimeManagerListener timeManagerListener = new TimeManagerListener() {
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

  public interface Contract extends DRMVPView {
    void startService();

    void updateCountdown(boolean animate);

    void setButtonChangeStateEnabled(boolean enabled);

    void setButtonChangeStateAttributes(boolean animate, @DrawableRes int drawableResId, @ColorRes int colorResId);

    void setProgressViewAttributes(@Visibility int visibility, long maxProgress, long currentProgress);

    void setButtonReplayVisibility(boolean animate, @Visibility int visibility);

    void hideSoftKeyboardAndClearEditFocus();

    void setEditTextsEnabled(boolean enabled);

    void setEditText(@IdRes int editTextResId, @NonNull String text);

    void showPopupFavorites(@NonNull List<Favorite> favorites);
  }
}
