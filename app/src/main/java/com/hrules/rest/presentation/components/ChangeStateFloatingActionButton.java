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

package com.hrules.rest.presentation.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;

public final class ChangeStateFloatingActionButton extends FloatingActionButton {
  private static final Interpolator DEFAULT_INTERPOLATOR = new AnticipateInterpolator();

  private static final float DEFAULT_SCALE = 1.0f;
  private static final float DEFAULT_SCALE_OUT = 0.2f;

  public ChangeStateFloatingActionButton(@NonNull Context context) {
    super(context);
  }

  public ChangeStateFloatingActionButton(@NonNull Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ChangeStateFloatingActionButton(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setState(boolean animate, @DrawableRes final int drawableResId, @ColorRes final int colorResId,
      final long animDurationMilli) {
    if (animate) {
      animate().scaleX(DEFAULT_SCALE_OUT)
          .scaleY(DEFAULT_SCALE_OUT)
          .setDuration(animDurationMilli)
          .setInterpolator(DEFAULT_INTERPOLATOR)
          .withEndAction(() -> {
            internalSetState(drawableResId, colorResId);
            animate().scaleX(DEFAULT_SCALE).scaleY(DEFAULT_SCALE).setDuration(animDurationMilli).withEndAction(() -> {
              setScaleX(DEFAULT_SCALE);
              setScaleY(DEFAULT_SCALE);
            }).start();
          })
          .start();
    } else {
      internalSetState(drawableResId, colorResId);
    }
  }

  private void internalSetState(@DrawableRes int drawableResId, @ColorRes int colorResId) {
    setImageResource(drawableResId);
    setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), colorResId)));
  }
}
