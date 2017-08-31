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

import android.support.annotation.NonNull;
import android.widget.EditText;
import com.hrules.darealmvp.DRMVPPresenter;
import com.hrules.darealmvp.DRMVPView;
import com.hrules.rest.App;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.core.commons.ZenModeHelper;
import com.hrules.rest.presentation.commons.ResUtils;
import com.hrules.rest.presentation.commons.annotations.Orientation;
import com.hrules.rest.presentation.commons.annotations.Visibility;
import com.hrules.rest.presentation.models.base.Favorite;
import com.hrules.rest.presentation.presenters.extras.CountdownPresenter;
import com.hrules.rest.presentation.presenters.extras.StopwatchPresenter;

public final class MainActivityPresenter extends DRMVPPresenter<MainActivityPresenter.Contract> {
  private static final boolean DEFAULT_KEEP_SCREEN_ON_STATE = false;

  private ResUtils resources;
  private Preferences preferences;

  private ZenModeHelper zenModeHelper;

  private CountdownPresenter countdownPresenter;
  private StopwatchPresenter stopwatchPresenter;

  @Override public void bind(@NonNull Contract view) {
    super.bind(view);
    resources = new ResUtils(App.getAppContext());
    preferences = new Preferences(App.getAppContext());
    zenModeHelper = new ZenModeHelper(App.getAppContext());

    // countdown
    countdownPresenter = new CountdownPresenter();
    countdownPresenter.bind((CountdownPresenter.Contract) view);

    // stopwatch
    stopwatchPresenter = new StopwatchPresenter();
    stopwatchPresenter.bind((StopwatchPresenter.Contract) view);
  }

  @Override public void unbind() {
    super.unbind();
    zenModeHelper.release();
    zenModeHelper = null;

    // countdown
    countdownPresenter.unbind();

    // stopwatch
    stopwatchPresenter.unbind();
  }

  public void onViewReady() {
    getView().setZenModeAlertVisibility(zenModeHelper.isZenModeActive() ? Visibility.VISIBLE : Visibility.GONE);
    zenModeHelper.setListener(zenModeManagerListener);

    // countdown
    countdownPresenter.onViewReady();

    // stopwatch
    stopwatchPresenter.onViewReady();
  }

  public void onViewResume() {
    boolean keepScreenOn = preferences.getBoolean(resources.getString(R.string.prefs_displayKeepScreenOnKey),
        resources.getBoolean(R.bool.prefs_displayKeepScreenOnDefault));

    String orientationFromPrefs = preferences.getString(resources.getString(R.string.prefs_displayOrientationKey),
        String.valueOf(resources.getInteger(R.integer.prefs_displayOrientationDefault)));
    String portrait = resources.getString(R.string.prefs_displayOrientationValuesPortrait);
    String landscape = resources.getString(R.string.prefs_displayOrientationValuesLandscape);
    @Orientation int orientation = Orientation.UNSPECIFIED;
    if (orientationFromPrefs.equals(portrait)) {
      orientation = Orientation.PORTRAIT;
    } else if (orientationFromPrefs.equals(landscape)) {
      orientation = Orientation.LANDSCAPE;
    }
    getView().setDisplayOptions(keepScreenOn, orientation);

    // countdown
    countdownPresenter.onViewResume();

    // stopwatch
    stopwatchPresenter.onViewResume();
  }

  public void onViewStop() {
    // we will stop UI updates in onViewStop() instead of onViewResume()
    // in order to support Multi Window (API >= 24)
    getView().setDisplayOptions(DEFAULT_KEEP_SCREEN_ON_STATE, Orientation.UNSPECIFIED);

    // countdown
    countdownPresenter.onViewStop();

    // stopwatch
    stopwatchPresenter.onViewStop();
  }

  private final ZenModeHelper.ZenModeManagerListener zenModeManagerListener =
      newState -> getView().setZenModeAlertVisibility(newState ? Visibility.VISIBLE : Visibility.GONE);

  // region COUNTDOWN
  public void onButtonChangeStateClick() {
    countdownPresenter.onButtonChangeStateClick();
  }

  public void onButtonReplayClick() {
    countdownPresenter.onButtonReplayClick();
  }

  public void onEditorActionDone() {
    countdownPresenter.onEditorActionDone();
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
  public void onButtonStopwatchChangeStateClick() {
    stopwatchPresenter.onButtonStopwatchChangeStateClick();
  }

  public void onButtonStopwatchChangeStateLongClick() {
    stopwatchPresenter.onButtonStopwatchChangeStateLongClick();
  }
  //endregion

  public interface Contract extends DRMVPView {
    void setDisplayOptions(boolean keepScreenOn, @Orientation int screenOrientationSensor);

    void setZenModeAlertVisibility(@Visibility int visibility);
  }
}