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

package com.hrules.rest.presentation.views.activities;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.hrules.rest.AppConstants;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.commons.SupportVersion;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.presentation.adapters.FavoritesAdapter;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.ViewUtils;
import com.hrules.rest.presentation.commons.annotations.Orientation;
import com.hrules.rest.presentation.commons.annotations.Visibility;
import com.hrules.rest.presentation.commons.components.ChangeStateFloatingActionButton;
import com.hrules.rest.presentation.commons.components.ProgressCountdownView;
import com.hrules.rest.presentation.commons.components.ReplayFloatingActionButton;
import com.hrules.rest.presentation.commons.components.RevealBackgroundView;
import com.hrules.rest.presentation.commons.components.ScaleAnimatedTextView;
import com.hrules.rest.presentation.commons.components.StopwatchButton;
import com.hrules.rest.presentation.commons.components.StopwatchTimeLayout;
import com.hrules.rest.presentation.commons.components.ToolTipView;
import com.hrules.rest.presentation.commons.helpers.StopwatchHelper;
import com.hrules.rest.presentation.commons.threads.UIHandler;
import com.hrules.rest.presentation.models.base.Favorite;
import com.hrules.rest.presentation.presenters.activities.MainActivityPresenter;
import com.hrules.rest.presentation.presenters.extras.CountdownPresenter;
import com.hrules.rest.presentation.presenters.extras.StopwatchPresenter;
import com.hrules.rest.presentation.views.activities.base.DRMVPAppCompatActivity;
import com.hrules.rest.services.TimeService;
import java.util.List;

import static com.hrules.rest.presentation.commons.resources.base.ResWrapper.getBoolean;

public final class MainActivityView extends DRMVPAppCompatActivity<MainActivityPresenter, MainActivityPresenter.Contract>
    implements MainActivityPresenter.Contract, CountdownPresenter.Contract, StopwatchPresenter.Contract {
  @BindView(R.id.layout_root) RelativeLayout layoutRoot;
  @BindView(R.id.progress_view) ProgressCountdownView progressView;
  @BindView(R.id.button_changeState) ChangeStateFloatingActionButton buttonChangeState;
  @BindView(R.id.button_replay) ReplayFloatingActionButton buttonReplay;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.zenmode_alert) TextView messageAlert;
  @BindView(R.id.edit_minutes) EditText editMinutes;
  @BindView(R.id.edit_seconds) EditText editSeconds;
  @BindView(R.id.text_editSeparator) TextView textEditSeparator;
  @BindView(R.id.button_favorites) ImageButton buttonFavorites;
  @BindView(R.id.text_countDown) ScaleAnimatedTextView textCountdown;
  @BindView(R.id.layout_stopwatchTime) StopwatchTimeLayout layoutStopwatchTime;
  @BindView(R.id.button_stopwatchChangeState) StopwatchButton buttonStopwatchChangeState;
  @BindView(R.id.reveal_background) RevealBackgroundView revealBackgroundView;

  private long defaultAnimDurationMilli;

  private ListPopupWindow listPopupWindow;

  @Override protected int getLayoutResId() {
    return R.layout.activity_main;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeViews();

    getPresenter().onViewReady();
  }

  private void initializeViews() {
    ButterKnife.bind(this);

    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    defaultAnimDurationMilli = getResources().getInteger(android.R.integer.config_shortAnimTime);

    listPopupWindow = new ListPopupWindow(this);

    editMinutes.setOnFocusChangeListener(editFocusChangeListener);
    editSeconds.setOnFocusChangeListener(editFocusChangeListener);

    editMinutes.setOnEditorActionListener(editActionListener);
    editSeconds.setOnEditorActionListener(editActionListener);

    editMinutes.addTextChangedListener(editTextChangedListener);
    editSeconds.addTextChangedListener(editTextChangedListener);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.menu_closeNotification).setVisible(isNotificationVisible());
    menu.findItem(R.id.menu_startSmartStopwatch).setVisible(isSmartStopwatch() && !isSmartStopwatchRunning());
    menu.findItem(R.id.menu_stopSmartStopwatch).setVisible(isSmartStopwatch() && isSmartStopwatchRunning());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_closeNotification:
        closeNotification();
        return true;

      case R.id.menu_startSmartStopwatch:
        startSmartWatch();
        return true;

      case R.id.menu_stopSmartStopwatch:
        stopSmartWatch();
        return true;

      case R.id.menu_preferences:
        launchPreferencesActivity();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override protected void onResume() {
    super.onResume();
    getPresenter().onViewResume();
  }

  @Override protected void onStop() {
    super.onStop();
    getPresenter().onViewStop();
  }

  private boolean isNotificationVisible() {
    if (SupportVersion.isMarshmallowOrAbove()) {
      NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
      for (StatusBarNotification notification : notifications) {
        if (notification.getId() == TimeService.NOTIFICATION_ID) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean isSmartStopwatch() {
    Preferences preferences = new Preferences(getApplicationContext());
    return preferences.getBoolean(getString(R.string.prefs_stopwatchSmartKey), getBoolean(R.bool.prefs_stopwatchSmartDefault));
  }

  private boolean isSmartStopwatchRunning() {
    Preferences preferences = new Preferences(getApplicationContext());
    StopwatchHelper stopwatchHelper = new StopwatchHelper(preferences);
    return stopwatchHelper.isRunning();
  }

  private void launchPreferencesActivity() {
    startActivity(new Intent(this, PreferenceActivityView.class));
  }

  private void closeNotification() {
    sendBroadcast(new Intent(AppConstants.ACTIONS.EXIT));
  }

  private void startSmartWatch() {
    getPresenter().startSmartWatch();
  }

  private void stopSmartWatch() {
    getPresenter().stopSmartWatch();
  }

  @Override public void setDisplayOptions(boolean keepScreenOn, @Orientation final int screenOrientationSensor) {
    if (keepScreenOn) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    new UIHandler().post(() -> setRequestedOrientation(screenOrientationSensor));
  }

  @Override public void setZenModeAlertVisibility(@Visibility int visibility) {
    messageAlert.setVisibility(visibility);
  }

  //region COUNTDOWN
  @Override public void startService() {
    startService(new Intent(this, TimeService.class));
  }

  @Override public void updateCountdown(boolean animate) {
    textCountdown.setText(TimeUtils.milliToMinutesSecondsMilliString(TimeUtils.getCountdownMilliUnsigned(), getResources()), animate,
        TimeManager.INSTANCE.isRunning() ? ScaleAnimatedTextView.ANIM_TYPE_SCALE_OUT : ScaleAnimatedTextView.ANIM_TYPE_SCALE_IN);
    textCountdown.setTextColor(TimeUtils.getTextColorFromMilli());

    if (TimeManager.INSTANCE.isRunning()
        && !TimeManager.INSTANCE.isCountdownOver()
        && revealBackgroundView.getState() == RevealBackgroundView.STATE_FINISHED) {
      progressView.setVisibility(View.VISIBLE);
      progressView.setCurrentProgress(TimeManager.INSTANCE.getElapsedTime());

      revealBackgroundView.setVisibility(View.INVISIBLE);
    } else {
      if (TimeManager.INSTANCE.isRunning() && TimeManager.INSTANCE.isCountdownOver()) {
        revealBackgroundView.setVisibility(View.INVISIBLE);
      }
      progressView.setVisibility(View.INVISIBLE);
    }
  }

  @Override public void setButtonChangeStateAttributes(boolean animate, @DrawableRes int drawableResId, @ColorInt int color) {
    if (animate) {
      doRevealBackground();
    } else {
      if (TimeManager.INSTANCE.isRunning()) {
        revealBackgroundView.setState(RevealBackgroundView.STATE_FINISHED);
      }
    }
    buttonChangeState.setState(animate, drawableResId, color, defaultAnimDurationMilli);
  }

  @Override public void setButtonChangeStateEnabled(boolean enabled) {
    buttonChangeState.setEnabled(enabled);
  }

  @Override public void setProgressViewAttributes(@Visibility int visibility, long maxProgress, long currentProgress) {
    progressView.setVisibility(visibility);
    progressView.setMaxProgress(maxProgress);
    progressView.setCurrentProgress(currentProgress);
  }

  @Override public void setButtonReplayVisibility(boolean animate, @Visibility int visibility) {
    buttonReplay.setVisibility(animate, visibility, defaultAnimDurationMilli);
    buttonReplay.setEnabled(visibility == View.VISIBLE);
  }

  private void doRevealBackground() {
    int height = revealBackgroundView.getHeight();
    long forecastHeight = ((progressView.getCurrentProgress() + revealBackgroundView.getAnimDurationMilli()) * progressView.getHeight())
        / progressView.getMaxProgress();
    int heightReveal = (int) (height - forecastHeight);
    int[] revealStartPosition = ViewUtils.getRevealStartPosition(this, buttonChangeState);

    if (TimeManager.INSTANCE.isRunning()) {
      revealBackgroundView.setOnStateChangeListener(null);
      revealBackgroundView.startFromLocation(revealStartPosition, heightReveal);
    } else {
      revealBackgroundView.setOnStateChangeListener(state -> {
        if (state == RevealBackgroundView.STATE_FINISHED) {
          revealBackgroundView.setVisibility(View.INVISIBLE);
        }
      });
      revealBackgroundView.startReverseFromLocation(revealStartPosition, heightReveal);
    }
  }

  @Override public void showPopupFavorites(@NonNull List<Favorite> favorites) {
    listPopupWindow.setAnchorView(findViewById(R.id.layout_time));
    listPopupWindow.setAdapter(new FavoritesAdapter(this, favorites, new FavoritesAdapter.FavoritesAdapterListener() {
      @Override public void onTitleClick(@NonNull Favorite favorite) {
        if (favorite.getType() == Favorite.Type.SECONDS) {
          getPresenter().onFavoriteTitleClick(favorite);
        } else if (favorite.getType() == Favorite.Type.ADD) {
          getPresenter().onFavoriteActionAddClick(editMinutes.getText().toString(), editSeconds.getText().toString());
        }
        listPopupWindow.dismiss();
      }

      @Override public void onDeleteClick(@NonNull Favorite favorite) {
        getPresenter().onFavoriteDeleteClick(favorite);
        listPopupWindow.dismiss();
      }
    }));
    listPopupWindow.setModal(true);
    listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT); // whole anchor view width
    listPopupWindow.show();
  }

  @Override public void setEditTextsEnabled(boolean enabled) {
    editMinutes.setEnabled(enabled);
    editSeconds.setEnabled(enabled);
    textEditSeparator.setEnabled(enabled);
    buttonFavorites.setEnabled(enabled);
  }

  @Override public void setEditText(@IdRes int editTextResId, @NonNull String text) {
    EditText editText = findViewById(editTextResId);
    editText.removeTextChangedListener(editTextChangedListener);
    editText.setText(text);
    editText.addTextChangedListener(editTextChangedListener);
  }

  @Override public void hideSoftKeyboardAndClearEditFocus() {
    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(layoutRoot.getWindowToken(), 0);
    layoutRoot.requestFocus();
  }

  @OnClick(R.id.button_changeState) void onButtonChangeStateClick() {
    getPresenter().onButtonChangeStateClick();
  }

  @OnClick(R.id.button_replay) void onButtonReplayClick() {
    getPresenter().onButtonReplayClick();
  }

  @OnClick(R.id.button_favorites) void onButtonFavoritesClick() {
    getPresenter().onButtonFavoritesClick(editMinutes.getText().toString(), editSeconds.getText().toString());
  }

  @OnClick(R.id.text_countDown) void onTextCountDownClick() {
    if (buttonFavorites.isEnabled()) {
      getPresenter().onButtonFavoritesClick(editMinutes.getText().toString(), editSeconds.getText().toString());
    }
  }

  private final TextView.OnEditorActionListener editActionListener = (v, actionId, event) -> {
    if (actionId == EditorInfo.IME_ACTION_DONE) {
      getPresenter().onEditorActionDone();
      return true;
    }
    return false;
  };

  private final View.OnFocusChangeListener editFocusChangeListener = (v, hasFocus) -> {
    if (v instanceof EditText) {
      getPresenter().onEditFocusChange(v.getId(), ((EditText) v).getText().toString(), hasFocus);
    }
  };

  private final TextWatcher editTextChangedListener = new TextWatcher() {
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override public void afterTextChanged(Editable s) {
      getPresenter().editAfterTextChanged(editMinutes.getText().toString(), editSeconds.getText().toString());
    }
  };
  //endregion

  //region STOPWATCH
  @Override public void showTooltip(@IdRes int viewResId, @NonNull String message) {
    ToolTipView.show(findViewById(viewResId), message, ToolTipView.LENGTH_SHORT);
  }

  @Override public void updateStopwatch(long milli) {
    layoutStopwatchTime.setText(TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(milli, getResources()));
  }

  @Override public void setStopwatchTimeLastTime(@NonNull CharSequence text) {
    layoutStopwatchTime.setLastText(text);
  }

  @Override public void setStopwatchTimeSize(@StopwatchTimeLayout.Size int size) {
    layoutStopwatchTime.setSize(size);
  }

  @Override public void setStopwatchButtonChangeStateSmart(boolean smart, boolean playing) {
    buttonStopwatchChangeState.setSmart(smart, playing);
  }

  @Override public void setStopwatchButtonChangeStatePlaying(boolean state) {
    if (state) {
      buttonStopwatchChangeState.setPlaying();
    } else {
      buttonStopwatchChangeState.setStopped();
    }
  }

  @OnClick(R.id.button_stopwatchChangeState) void onButtonStopwatchChangeStateClick() {
    getPresenter().onButtonStopwatchChangeStateClick();
  }

  @OnLongClick(R.id.button_stopwatchChangeState) boolean onButtonStopwatchChangeStateLongClick() {
    getPresenter().onButtonStopwatchChangeStateLongClick();
    return true;
  }
  //endregion
}