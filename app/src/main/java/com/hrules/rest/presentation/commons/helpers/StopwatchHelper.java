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

package com.hrules.rest.presentation.commons.helpers;

import android.support.annotation.NonNull;
import com.hrules.rest.AppConstants;
import com.hrules.rest.commons.Preferences;

public final class StopwatchHelper {
  private final Preferences preferences;

  public StopwatchHelper(@NonNull Preferences preferences) {
    this.preferences = preferences;
  }

  public void start() {
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
