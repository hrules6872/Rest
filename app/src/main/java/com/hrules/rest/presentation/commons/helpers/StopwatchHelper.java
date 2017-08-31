package com.hrules.rest.presentation.commons.helpers;

import android.support.annotation.NonNull;
import com.hrules.rest.AppConstants;
import com.hrules.rest.commons.Preferences;

public final class StopwatchHelper {
  private final Preferences preferences;

  public StopwatchHelper(@NonNull Preferences preferences) {
    this.preferences = preferences;
  }

  public void play() {
    long stopwatchStartTime = System.currentTimeMillis();
    preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, stopwatchStartTime);
    preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_BACKUP, stopwatchStartTime);
  }

  public void stop(boolean smart) {
    long last = 0;
    if (smart) {
      last = getStopwatchMilliLast();
    }
    preferences.save(AppConstants.PREFS.STOPWATCH_MILLI_LAST, last + (System.currentTimeMillis() - getStopwatchMilli()));
    preferences.save(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
  }

  public boolean isRunning() {
    long stopwatch = getStopwatchMilli();
    return stopwatch != AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI;
  }

  public long getStopwatchMilli() {
    return preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI);
  }

  public long getStopwatchMilliLast() {
    return preferences.getLong(AppConstants.PREFS.STOPWATCH_MILLI_LAST, AppConstants.PREFS.DEFAULTS.STOPWATCH_MILLI_LAST);
  }
}
