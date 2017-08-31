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

import android.support.annotation.ColorInt;
import com.hrules.rest.R;
import com.hrules.rest.presentation.commons.resources.base.ResWrapper;

public final class ResColor {
  private ResColor() {
  }

  public static @ColorInt int getNotification_countDownText() {
    return ResWrapper.getColor(R.color.notification_countDownText);
  }

  public static @ColorInt int getNotification_countDownTextOver() {
    return ResWrapper.getColor(R.color.notification_countDownTextOver);
  }

  public static @ColorInt int getCountDownText() {
    return ResWrapper.getColor(R.color.countDownText);
  }

  public static @ColorInt int getCountDownTextOver() {
    return ResWrapper.getColor(R.color.countDownTextOver);
  }

  public static @ColorInt int getFab_playBackground() {
    return ResWrapper.getColor(R.color.fab_playBackground);
  }

  public static @ColorInt int getFab_playDisabledBackground() {
    return ResWrapper.getColor(R.color.fab_playDisabledBackground);
  }

  public static @ColorInt int getFab_stopBackground() {
    return ResWrapper.getColor(R.color.fab_stopBackground);
  }

  public static @ColorInt int getFab_stopOverBackground() {
    return ResWrapper.getColor(R.color.fab_stopOverBackground);
  }
}
