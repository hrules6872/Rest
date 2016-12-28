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

package com.hrules.rest.core;

import android.os.Build;
import android.provider.Settings;
import com.hrules.rest.App;

public final class AudioUtils {
  private static final int ZEN_MODE_OFF = 0;

  private AudioUtils() {
  }

  public static boolean isDoNotDisturbActive() {
    boolean result = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      try {
        result = Settings.Global.getInt(App.getAppContext().getContentResolver(), "zen_mode") != ZEN_MODE_OFF;
      } catch (Settings.SettingNotFoundException e) {
        result = false;
      }
    }
    return result;
  }
}
