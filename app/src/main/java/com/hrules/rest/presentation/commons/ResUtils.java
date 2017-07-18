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

package com.hrules.rest.presentation.commons;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.BoolRes;
import android.support.annotation.ColorRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

public final class ResUtils {
  private final Context context;

  public ResUtils(@NonNull Context context) {
    this.context = context;
  }

  public boolean getBoolean(@BoolRes int resId) {
    return context.getResources().getBoolean(resId);
  }

  public @NonNull String getString(@StringRes int resId) {
    return context.getResources().getString(resId);
  }

  public @NonNull Resources getResources() {
    return context.getResources();
  }

  public int getInteger(@IntegerRes int resId) {
    return context.getResources().getInteger(resId);
  }

  int getColor(@ColorRes int resId) {
    return ContextCompat.getColor(context, resId);
  }
}
