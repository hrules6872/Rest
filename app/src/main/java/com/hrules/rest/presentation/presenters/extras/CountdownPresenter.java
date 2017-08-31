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
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hrules.darealmvp.DRMVPPresenter;
import com.hrules.darealmvp.DRMVPView;
import com.hrules.rest.App;
import com.hrules.rest.AppConstants;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.alerts.VibratorHelper;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.core.time.TimeManagerListener;
import com.hrules.rest.presentation.commons.PreConditions;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.annotations.Visibility;
import com.hrules.rest.presentation.commons.resources.ResBoolean;
import com.hrules.rest.presentation.commons.resources.ResColor;
import com.hrules.rest.presentation.commons.resources.ResDrawableId;
import com.hrules.rest.presentation.commons.resources.ResId;
import com.hrules.rest.presentation.commons.resources.ResString;
import com.hrules.rest.presentation.commons.resources.base.ResWrapper;
import com.hrules.rest.presentation.commons.threads.UIHandler;
import com.hrules.rest.presentation.commons.threads.base.Handler;
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

public final class CountdownPresenter extends DRMVPPresenter<CountdownPresenter.Contract> {
  private static final long DEFAULT_EDIT_STATE_EMPTY = 0;

  private Preferences preferences;
  private boolean prefsVibrateButtons;

  private VibratorHelper vibratorHelper;

  private final Handler countdownHandler = new UIHandler();

  @Override public void bind(@NonNull Contract view) {
    super.bind(view);
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
    getView().setEditText(ResId.getEdit_minutes(), String.valueOf(minutes));

    long seconds = TimeUtils.getRemainderSecondsFromMilli(
        preferences.getLong(AppConstants.PREFS.COUNTDOWN_MILLI, AppConstants.PREFS.DEFAULTS.COUNTDOWN_MILLI));
    getView().setEditText(ResId.getEdit_seconds(), TimeUtils.getSecondsFormattedWithLeadingZeros(seconds));
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
    prefsVibrateButtons =
        preferences.getBoolean(ResString.getPrefs_controlVibrateButtonsKey(), ResBoolean.getPrefs_controlVibrateButtonsDefault());
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

      getView().setProgressViewAttributes(Visibility.INVISIBLE, TimeManager.INSTANCE.getCountdownTimeMilli(),
          TimeManager.INSTANCE.getElapsedTime());
      updateStopButtonColor(animate);
      getView().setButtonReplayVisibility(animate, Visibility.VISIBLE);
    } else {
      // stop countdown
      getView().setButtonChangeStateAttributes(animate, ResDrawableId.getIc_play_fab(), ResColor.getFab_playBackground());
      getView().setButtonReplayVisibility(animate, Visibility.INVISIBLE);
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
      getView().setButtonChangeStateAttributes(false, ResDrawableId.getIc_play_fab(), ResColor.getFab_playBackground());
    } else {
      getView().setButtonChangeStateAttributes(false, ResDrawableId.getIc_play_fab(), ResColor.getFab_playDisabledBackground());
    }
  }

  private void updateStopButtonColor(boolean animate) {
    if (TimeManager.INSTANCE.isCountdownOver()) {
      getView().setButtonChangeStateAttributes(false, ResDrawableId.getIc_stop_over_fab(), ResColor.getFab_stopOverBackground());
    } else {
      getView().setButtonChangeStateAttributes(animate, ResDrawableId.getIc_stop_fab(), ResColor.getFab_stopBackground());
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

  public void onEditorActionDone() {
    getView().hideSoftKeyboardAndClearEditFocus();
  }

  public void onEditFocusChange(@IdRes int editTextResId, @Nullable String text, boolean hasFocus) {
    if (!hasFocus) {
      if (PreConditions.isStringNullOrEmpty(text)) {
        if (editTextResId == ResId.getEdit_seconds()) {
          getView().setEditText(ResId.getEdit_seconds(), TimeUtils.getSecondsFormattedWithLeadingZeros(DEFAULT_EDIT_STATE_EMPTY));
        } else {
          getView().setEditText(ResId.getEdit_minutes(), String.valueOf(DEFAULT_EDIT_STATE_EMPTY));
        }
      } else {
        if (editTextResId == ResId.getEdit_seconds()) {
          getView().setEditText(ResId.getEdit_seconds(), TimeUtils.getSecondsFormattedWithLeadingZeros(Long.valueOf(text)));
        }
      }
    }
  }

  public void editAfterTextChanged(@NonNull String minutes, @NonNull String seconds) {
    long minutesL;
    long secondsL;

    try {
      if (PreConditions.isStringNullOrEmpty(minutes)) {
        minutesL = DEFAULT_EDIT_STATE_EMPTY;
      } else {
        minutesL = Long.parseLong(minutes);
      }

      if (PreConditions.isStringNullOrEmpty(seconds)) {
        secondsL = DEFAULT_EDIT_STATE_EMPTY;
      } else {
        secondsL = Long.parseLong(seconds);
        if (secondsL > TimeUnit.MINUTES.toSeconds(1) - 1) {
          secondsL = TimeUnit.MINUTES.toSeconds(1) - 1;
          getView().setEditText(ResId.getEdit_seconds(), String.valueOf(secondsL));
        }
      }

      preferences.save(AppConstants.PREFS.COUNTDOWN_MILLI, TimeUtils.getMilliFromMinutesSecond(minutesL, secondsL));
      TimeManager.INSTANCE.setCountdownTimeMilli(TimeUtils.getMilliFromMinutesSecond(minutesL, secondsL));
    } catch (NumberFormatException ignored) {
    }
  }

  public void onButtonFavoritesClick(@NonNull String minutes, @NonNull String seconds) {
    List<Favorite> favorites = new ArrayList<>();

    Set<String> favoritesSet = new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));
    for (String secondsFromStringSet : favoritesSet) {
      long milli = Integer.valueOf(secondsFromStringSet) * TimeUnit.SECONDS.toMillis(1);
      long secondsL = Integer.valueOf(secondsFromStringSet);
      favorites.add(new FavoriteSeconds(TimeUtils.milliToFavoriteMinutesSecondsString(milli, ResWrapper.getResources()), secondsL));
    }
    Collections.sort(favorites, new FavoriteSecondsAscendingComparator());

    try {
      if (!PreConditions.isStringNullOrEmpty(minutes) && !PreConditions.isStringNullOrEmpty(minutes)) {
        long minutesL = Long.parseLong(minutes);
        long secondsL = Long.parseLong(seconds);
        String totalSeconds = String.valueOf(TimeUnit.MINUTES.toSeconds(minutesL) + secondsL);
        if (!favoritesSet.contains(totalSeconds) && !"0".equals(totalSeconds)) {
          favorites.add(0, new FavoriteAdd(ResString.getText_addFavorite()));
        }
      }

      getView().showPopupFavorites(favorites);
    } catch (NumberFormatException ignored) {
    }
  }

  public void onFavoriteTitleClick(@NonNull Favorite favorite) {
    long milli = favorite.getSeconds() * TimeUnit.SECONDS.toMillis(1);

    long minutes = TimeUtils.getExactMinutesFromMilli(milli);
    getView().setEditText(ResId.getEdit_minutes(), String.valueOf(minutes));

    long seconds = TimeUtils.getRemainderSecondsFromMilli(milli);
    getView().setEditText(ResId.getEdit_seconds(), TimeUtils.getSecondsFormattedWithLeadingZeros(seconds));

    preferences.save(AppConstants.PREFS.COUNTDOWN_MILLI, TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
    TimeManager.INSTANCE.setCountdownTimeMilli(TimeUtils.getMilliFromMinutesSecond(minutes, seconds));
  }

  public void onFavoriteActionAddClick(@NonNull String minutes, @NonNull String seconds) {
    Set<String> favoritesSet = new HashSet<>(preferences.getStringSet(AppConstants.PREFS.FAVORITES, AppConstants.PREFS.DEFAULTS.FAVORITES));

    try {
      long minutesL = Long.parseLong(minutes);
      long secondsL = Long.parseLong(seconds);
      String secondsStringToSave =
          String.valueOf(Math.round(TimeUtils.getMilliFromMinutesSecond(minutesL, secondsL) / TimeUnit.SECONDS.toMillis(1)));
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

    void setButtonChangeStateAttributes(boolean animate, @DrawableRes int drawableResId, @ColorInt int color);

    void setProgressViewAttributes(@Visibility int visibility, long maxProgress, long currentProgress);

    void setButtonReplayVisibility(boolean animate, @Visibility int visibility);

    void hideSoftKeyboardAndClearEditFocus();

    void setEditTextsEnabled(boolean enabled);

    void setEditText(@IdRes int editTextResId, @NonNull String text);

    void showPopupFavorites(@NonNull List<Favorite> favorites);
  }
}
