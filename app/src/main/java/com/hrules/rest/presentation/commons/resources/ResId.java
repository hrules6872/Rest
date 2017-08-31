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

import android.support.annotation.IdRes;
import com.hrules.rest.R;

public final class ResId {
  private ResId() {
  }

  public static @IdRes int getButton_stopwatchChangeState() {
    return R.id.button_stopwatchChangeState;
  }

  public static @IdRes int getEdit_seconds() {
    return R.id.edit_seconds;
  }

  public static @IdRes int getEdit_minutes() {
    return R.id.edit_minutes;
  }
}
