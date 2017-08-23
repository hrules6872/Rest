package com.hrules.rest.core.time;

import com.hrules.rest.core.time.base.TimeManagerListenerInterface;

public abstract class TimeManagerListener implements TimeManagerListenerInterface {
  public void onStateChanged() {
  }

  public void onCountdownTimeChanged() {
  }

  public void onCountdownTick() {
  }
}