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

package com.hrules.rest.presentation.commons.resources;

import android.support.annotation.NonNull;
import com.hrules.rest.R;
import com.hrules.rest.presentation.commons.resources.base.ResWrapper;

public final class ResString {
  private ResString() {
  }

  // region TEXT
  public static @NonNull String getText_smartStopWatchFormatted() {
    return ResWrapper.getString(R.string.text_smartStopWatchFormatted);
  }

  public static @NonNull String getText_longClickStopwatch() {
    return ResWrapper.getString(R.string.text_longClickStopwatch);
  }

  public static @NonNull String getText_addFavorite() {
    return ResWrapper.getString(R.string.text_addFavorite);
  }
  //endregion

  // region PREFS
  public static @NonNull String getPrefs_displayKeepScreenOnKey() {
    return ResWrapper.getString(R.string.prefs_displayKeepScreenOnKey);
  }

  public static @NonNull String getPrefs_displayOrientationKey() {
    return ResWrapper.getString(R.string.prefs_displayOrientationKey);
  }

  public static @NonNull String getPrefs_displayOrientationValuesPortrait() {
    return ResWrapper.getString(R.string.prefs_displayOrientationValuesPortrait);
  }

  public static @NonNull String getPrefs_displayOrientationValuesLandscape() {
    return ResWrapper.getString(R.string.prefs_displayOrientationValuesLandscape);
  }

  public static @NonNull String getPrefs_controlVibrateButtonsKey() {
    return ResWrapper.getString(R.string.prefs_controlVibrateButtonsKey);
  }

  public static @NonNull String getPrefs_stopwatchSmartKey() {
    return ResWrapper.getString(R.string.prefs_stopwatchSmartKey);
  }

  public static @NonNull String getPrefs_stopwatchAutoStopKey() {
    return ResWrapper.getString(R.string.prefs_stopwatchAutoStopKey);
  }

  public static @NonNull String getPrefs_stopwatchSizeKey() {
    return ResWrapper.getString(R.string.prefs_stopwatchSizeKey);
  }

  public static @NonNull String getPrefs_stopwatchSizeValuesNormal() {
    return ResWrapper.getString(R.string.prefs_stopwatchSizeValuesNormal);
  }
  //endregion
}
