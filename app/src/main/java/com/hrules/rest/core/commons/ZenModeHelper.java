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

package com.hrules.rest.core.commons;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

public final class ZenModeHelper {
  private static final String ZEN_MODE = "zen_mode";
  private static final int ZEN_MODE_OFF = 0;

  private final ContentResolver resolver;
  private boolean state = false;

  private ZenModeObserver observer;
  private ZenModeManagerListener listener;

  public interface ZenModeManagerListener {
    void onStateChanged(boolean newState);
  }

  public ZenModeHelper(@NonNull Context context) {
    resolver = context.getContentResolver();
    checkZenModeState();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      observer = new ZenModeObserver(newState -> checkZenModeState());
      resolver.registerContentObserver(Settings.Global.getUriFor(ZEN_MODE), false, observer);
    }
  }

  @SuppressWarnings("SuspiciousGetterSetter") public boolean isZenModeActive() {
    return state;
  }

  private void checkZenModeState() {
    state = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      try {
        state = Settings.Global.getInt(resolver, ZEN_MODE) != ZEN_MODE_OFF;
      } catch (Settings.SettingNotFoundException ignored) {
      }

      if (listener != null) {
        listener.onStateChanged(state);
      }
    }
  }

  public void release() {
    this.listener = null;

    if (resolver != null) {
      resolver.unregisterContentObserver(observer);
      observer = null;
    }
  }

  public void setListener(@NonNull ZenModeManagerListener listener) {
    this.listener = listener;
  }
}
