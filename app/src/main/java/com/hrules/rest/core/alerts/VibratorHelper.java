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

package com.hrules.rest.core.alerts;

import android.content.Context;
import android.os.Vibrator;
import android.support.annotation.NonNull;

public final class VibratorHelper {
  private static final long VIBRATE_NO_DELAY_MILLI = 0;
  private static final long VIBRATE_DELAY_MILLI = 150;
  private static final int VIBRATE_NO_REPEAT = -1;

  private static final long VIBRATE_CLICK_MILLI = 50;
  private static final long VIBRATE_SHORT_MILLI = 300;

  private static final long[] VIBRATE_SHORT2_PATTERN = {
      VIBRATE_NO_DELAY_MILLI, VIBRATE_SHORT_MILLI, VIBRATE_DELAY_MILLI, VIBRATE_SHORT_MILLI
  };
  private static final long[] VIBRATE_LONG_PATTERN = {
      VIBRATE_NO_DELAY_MILLI, VIBRATE_SHORT_MILLI, VIBRATE_DELAY_MILLI, VIBRATE_SHORT_MILLI, VIBRATE_DELAY_MILLI,
      VIBRATE_SHORT_MILLI
  };

  public static void vibrateShort(@NonNull Context context) {
    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(VIBRATE_SHORT_MILLI);
  }

  public static void vibrateShort2(@NonNull Context context) {
    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(VIBRATE_SHORT2_PATTERN, VIBRATE_NO_REPEAT);
  }

  public static void vibrateLong(@NonNull Context context) {
    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(VIBRATE_LONG_PATTERN, VIBRATE_NO_REPEAT);
  }

  public static void vibrateClick(@NonNull Context context) {
    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(VIBRATE_CLICK_MILLI);
  }
}
