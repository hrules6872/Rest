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

package com.hrules.rest.core.time;

import android.support.annotation.NonNull;
import com.hrules.rest.AppConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public enum TimeManager {
  INSTANCE;

  private static final int STATE_STOPPED = 0;
  private static final int STATE_RUNNING = 1;

  private static final int DEFAULT_EXECUTOR_CORE_POOL_SIZE = 1;

  private static final long DEFAULT_DELAY_MILLI = 0;
  private static final long DEFAULT_PERIOD_MILLI = 33;

  private int currentState = STATE_STOPPED;
  private long startTimeMilli;
  private long countdownTimeMilli = AppConstants.DEFAULT_COUNTDOWN_MILLI;

  private final List<TimeManagerListener> listeners = new ArrayList<>();

  private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
      new ScheduledThreadPoolExecutor(DEFAULT_EXECUTOR_CORE_POOL_SIZE);
  private ScheduledFuture scheduledFutureCountdown;

  public void addListener(@NonNull TimeManagerListener listener) {
    removeListener(listener);
    listeners.add(listener);
  }

  public void removeListener(@NonNull TimeManagerListener listener) {
    int position = listeners.indexOf(listener);
    if (position != -1) {
      listeners.remove(position);
    }
  }

  @SuppressWarnings("Convert2streamapi") private void notifyStateChanged() {
    for (TimeManagerListener listener : listeners) {
      if (listener != null) {
        listener.onStateChanged();
      }
    }
  }

  @SuppressWarnings("Convert2streamapi") private void notifyCountdownTimeChanged() {
    for (TimeManagerListener listener : listeners) {
      if (listener != null) {
        listener.onCountdownTimeChanged();
      }
    }
  }

  @SuppressWarnings("Convert2streamapi") private void notifyCountdownTick() {
    for (TimeManagerListener listener : listeners) {
      if (listener != null) {
        listener.onCountdownTick();
      }
    }
  }

  public boolean isRunning() {
    return currentState == STATE_RUNNING;
  }

  public boolean isCountdownOver() {
    return getElapsedTime() >= countdownTimeMilli;
  }

  public long getElapsedTime() {
    return startTimeMilli == 0 ? 0 : System.currentTimeMillis() - startTimeMilli;
  }

  public long getCountdownTimeMilli() {
    return countdownTimeMilli;
  }

  public void setCountdownTimeMilli(long countDownMilli) {
    this.countdownTimeMilli = countDownMilli;
    notifyCountdownTimeChanged();
  }

  public synchronized void start() {
    if (currentState == STATE_STOPPED) {
      startTimeMilli = System.currentTimeMillis();
      currentState = STATE_RUNNING;

      startCountdownRunnable();

      notifyStateChanged();
    }
  }

  public synchronized void stop() {
    if (currentState == STATE_RUNNING) {
      startTimeMilli = 0;
      currentState = STATE_STOPPED;

      stopCountdownRunnable();

      notifyStateChanged();
    }
  }

  public synchronized void reStart() {
    currentState = STATE_STOPPED;
    start();
  }

  public synchronized void toggle() {
    if (currentState == STATE_RUNNING) {
      stop();
    } else {
      start();
    }
  }

  private void startCountdownRunnable() {
    stopCountdownRunnable();
    scheduledFutureCountdown =
        scheduledThreadPoolExecutor.scheduleAtFixedRate(updateCountdownRunnable, DEFAULT_DELAY_MILLI,
            DEFAULT_PERIOD_MILLI, TimeUnit.MILLISECONDS);
  }

  private void stopCountdownRunnable() {
    if (scheduledFutureCountdown != null) {
      scheduledFutureCountdown.cancel(false);
    }
  }

  private final Runnable updateCountdownRunnable = () -> {
    if (currentState == STATE_RUNNING) {
      notifyCountdownTick();
    }
  };
}
