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

package com.hrules.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class AppConstants {
  public static final long DEFAULT_COUNTDOWN_MILLI = TimeUnit.SECONDS.toMillis(90);

  private AppConstants() {
  }

  public static final class PREFS {
    public static final String COUNTDOWN_MILLI = "COUNTDOWN_MILLI";
    public static final String STOPWATCH_MILLI = "STOPWATCH_MILLI";
    public static final String STOPWATCH_MILLI_LAST = "STOPWATCH_MILLI_LAST";
    public static final String FAVORITES = "FAVORITES";

    public static final class DEFAULTS {
      public static final long COUNTDOWN_MILLI = DEFAULT_COUNTDOWN_MILLI;
      public static final long STOPWATCH_MILLI = 0;
      public static final long STOPWATCH_MILLI_LAST = 0;
      public static final Set<String> FAVORITES = new HashSet<>(Arrays.asList("60", "120", "90", "180"));
    }
  }
}
