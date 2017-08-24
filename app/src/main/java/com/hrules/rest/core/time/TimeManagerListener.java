package com.hrules.rest.core.time;

import com.hrules.rest.core.time.base.TimeManagerListenerInterface;

public abstract class TimeManagerListener implements TimeManagerListenerInterface {
  @Override public void onStateChanged() {
  }

  @Override public void onCountdownTimeChanged() {
  }

  @Override public void onCountdownTick() {
  }
}