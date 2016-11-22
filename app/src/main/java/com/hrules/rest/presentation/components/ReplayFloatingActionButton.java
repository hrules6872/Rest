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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import com.hrules.rest.presentation.commons.Visibility;

public class ReplayFloatingActionButton extends FloatingActionButton {
  private static final Interpolator DEFAULT_INTERPOLATOR = new AnticipateInterpolator();

  private static final float DEFAULT_SCALE = 1.0f;
  private static final float DEFAULT_SCALE_OUT = 0.0f;

  public ReplayFloatingActionButton(@NonNull Context context) {
    super(context);
  }

  public ReplayFloatingActionButton(@NonNull Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ReplayFloatingActionButton(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setVisibility(boolean animate, @Visibility final int visibility, final long animDurationMilli) {
    if (animate) {
      if (visibility == View.VISIBLE) {
        setScaleX(DEFAULT_SCALE_OUT);
        setScaleY(DEFAULT_SCALE_OUT);

        animate().scaleX(DEFAULT_SCALE).scaleY(DEFAULT_SCALE)
            .setInterpolator(DEFAULT_INTERPOLATOR)
            .setDuration(animDurationMilli)
            .setStartDelay(animDurationMilli)
            .start();
      } else if (visibility == View.INVISIBLE) {
        animate().scaleX(DEFAULT_SCALE_OUT).scaleY(DEFAULT_SCALE_OUT).setDuration(animDurationMilli).start();
      }
    } else {
      setScaleX(visibility == View.VISIBLE ? DEFAULT_SCALE : DEFAULT_SCALE_OUT);
      setScaleY(visibility == View.VISIBLE ? DEFAULT_SCALE : DEFAULT_SCALE_OUT);
    }
  }
}
