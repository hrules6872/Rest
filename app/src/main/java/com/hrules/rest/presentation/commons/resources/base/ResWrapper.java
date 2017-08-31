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

package com.hrules.rest.presentation.commons.resources.base;

import android.content.res.Resources;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import com.hrules.rest.App;

public final class ResWrapper {
  private ResWrapper() {
  }

  public static boolean getBoolean(@BoolRes int resId) {
    return App.getAppContext().getResources().getBoolean(resId);
  }

  public static @NonNull String getString(@StringRes int resId) {
    return App.getAppContext().getResources().getString(resId);
  }

  public static @NonNull Resources getResources() {
    return App.getAppContext().getResources();
  }

  public static int getInteger(@IntegerRes int resId) {
    return App.getAppContext().getResources().getInteger(resId);
  }

  public static int getColor(@ColorRes int resId) {
    return ContextCompat.getColor(App.getAppContext(), resId);
  }
}
