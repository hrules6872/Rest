package com.hrules.rest.core.time.base;

public interface TimeManagerListenerInterface {
  void onStateChanged();

  void onCountdownTimeChanged();

  void onCountdownTick();
}
