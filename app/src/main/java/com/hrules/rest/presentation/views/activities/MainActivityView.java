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

package com.hrules.rest.presentation.views.activities;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
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
import com.hrules.darealmvp.DRAppCompatActivity;
import com.hrules.rest.R;
import com.hrules.rest.core.time.TimeManager;
import com.hrules.rest.presentation.adapters.FavoritesAdapter;
import com.hrules.rest.presentation.commons.TimeUtils;
import com.hrules.rest.presentation.commons.ViewUtils;
import com.hrules.rest.presentation.commons.Visibility;
import com.hrules.rest.presentation.components.ChangeStateFloatingActionButton;
import com.hrules.rest.presentation.components.ProgressCountdownView;
import com.hrules.rest.presentation.components.ReplayFloatingActionButton;
import com.hrules.rest.presentation.components.RevealBackgroundView;
import com.hrules.rest.presentation.components.ScaleAnimatedTextView;
import com.hrules.rest.presentation.components.ToolTipView;
import com.hrules.rest.presentation.models.base.Favorite;
import com.hrules.rest.presentation.presenters.activities.MainActivityPresenter;
import com.hrules.rest.services.TimeService;
import com.hrules.rest.services.TimeServiceReceiver;
import java.util.List;

public class MainActivityView extends DRAppCompatActivity<MainActivityPresenter, MainActivityPresenter.MainView>
    implements MainActivityPresenter.MainView {
  @BindView(R.id.layout_root) RelativeLayout layoutRoot;
  @BindView(R.id.progress_view) ProgressCountdownView progressView;
  @BindView(R.id.button_changeState) ChangeStateFloatingActionButton buttonChangeState;
  @BindView(R.id.button_replay) ReplayFloatingActionButton buttonReplay;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.message_alert) TextView messageAlert;
  @BindView(R.id.edit_minutes) EditText editMinutes;
  @BindView(R.id.edit_seconds) EditText editSeconds;
  @BindView(R.id.text_editSeparator) TextView textEditSeparator;
  @BindView(R.id.button_favorites) ImageButton buttonFavorites;
  @BindView(R.id.text_countDown) ScaleAnimatedTextView textCountdown;
  @BindView(R.id.text_stopwatch) TextView textStopwatch;
  @BindView(R.id.text_stopwatchLast) TextView textStopwatchLast;
  @BindView(R.id.button_stopwatchChangeState) ImageButton buttonStopwatchChangeState;
  @BindView(R.id.reveal_background) RevealBackgroundView revealBackgroundView;

  private long defaultAnimDurationMilli;

  private ListPopupWindow listPopupWindow;

  @Override protected int getLayoutResource() {
    return R.layout.main_activity;
  }

  @Override protected void initializeViews() {
    ButterKnife.bind(this);
    setSupportActionBar(toolbar);
    try {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    } catch (Exception ignored) {
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
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_preferences:
      case R.id.menu_closeNotification:
        getPresenter().onMenuItemClick(item);
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private boolean isNotificationVisible() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

  @Override public void launchPreferencesActivity() {
    startActivity(new Intent(this, PreferenceActivityView.class));
  }

  @Override public void closeNotification() {
    sendBroadcast(new Intent(TimeServiceReceiver.ACTION_EXIT));
  }

  @Override public void setDisplayOptions(boolean keepScreenOn, final int screenOrientationSensor) {
    if (keepScreenOn) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    new Handler(Looper.getMainLooper()).post(() -> setRequestedOrientation(screenOrientationSensor));
  }

  @Override public void showTooltip(@IdRes int viewResId, @StringRes int stringResId) {
    ToolTipView.show(findViewById(viewResId), getString(stringResId), ToolTipView.LENGTH_SHORT);
  }

  @Override public void setMessageAlertVisibility(@Visibility int visibility) {
    messageAlert.setVisibility(visibility);
  }

  //region COUNTDOWN
  @Override public void startServiceIfNotRunning() {
    if (!isServiceRunning()) {
      startService(new Intent(this, TimeService.class));
    }
  }

  private boolean isServiceRunning() {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (TimeService.class.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  @Override public void updateCountdown(boolean animate) {
    textCountdown.setText(
        TimeUtils.milliToMinutesSecondsMilliString(TimeUtils.getCountdownMilliUnsigned(), getResources()), animate,
        TimeManager.INSTANCE.isRunning() ? ScaleAnimatedTextView.ANIM_TYPE_SCALE_OUT
            : ScaleAnimatedTextView.ANIM_TYPE_SCALE_IN);
    textCountdown.setTextColor(TimeUtils.getTextColorFromMilli(MainActivityView.this));

    if (TimeManager.INSTANCE.isRunning() && !TimeManager.INSTANCE.isCountdownOver()
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

  @Override public void setButtonChangeStateAttributes(boolean animate, @DrawableRes int drawableResId,
      @ColorRes int colorResId) {
    if (animate) {
      doRevealBackground();
    } else {
      if (TimeManager.INSTANCE.isRunning()) {
        revealBackgroundView.setState(RevealBackgroundView.STATE_FINISHED);
      }
    }
    buttonChangeState.setState(animate, drawableResId, colorResId, defaultAnimDurationMilli);
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
    long forecastHeight =
        ((progressView.getCurrentProgress() + revealBackgroundView.getAnimDuration()) * progressView.getHeight())
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
          getPresenter().onFavoriteActionAddClick(editMinutes, editSeconds);
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
    EditText editText = (EditText) findViewById(editTextResId);
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
    getPresenter().onButtonFavoritesClick(editMinutes, editSeconds);
  }

  @OnClick(R.id.text_countDown) void onTextCountDownClick() {
    if (buttonFavorites.isEnabled()) {
      getPresenter().onButtonFavoritesClick(editMinutes, editSeconds);
    }
  }

  private final TextView.OnEditorActionListener editActionListener = (v, actionId, event) -> {
    if (actionId == EditorInfo.IME_ACTION_DONE) {
      getPresenter().onEditorAction(v, actionId, event);
      return true;
    }
    return false;
  };

  private final View.OnFocusChangeListener editFocusChangeListener = (v, hasFocus) -> {
    if (v instanceof EditText) {
      getPresenter().onEditFocusChange((EditText) v, hasFocus);
    }
  };

  private final TextWatcher editTextChangedListener = new TextWatcher() {
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override public void afterTextChanged(Editable s) {
      getPresenter().editAfterTextChanged(editMinutes, editSeconds);
    }
  };
  //endregion

  //region STOPWATCH
  @Override public void updateStopwatch(long milli) {
    textStopwatch.setText(TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(milli, getResources()));
  }

  @OnClick(R.id.button_stopwatchChangeState) void onButtonStopwatchChangeStateClick() {
    getPresenter().onButtonStopwatchChangeStateClick();
  }

  @OnLongClick(R.id.button_stopwatchChangeState) boolean onButtonStopwatchChangeStateLongClick() {
    getPresenter().onButtonStopwatchChangeStateLongClick();
    return true;
  }

  @Override public void setStopwatchButtonChangeStateResource(@DrawableRes int resId) {
    buttonStopwatchChangeState.setImageResource(resId);
  }

  @Override public void setStopwatchTextLastTime(long milli) {
    textStopwatchLast.setText(TimeUtils.milliToStopwatchHoursMinutesSecondsMilliString(milli, getResources()));
  }
  //endregion
}