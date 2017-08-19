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

package com.hrules.rest.presentation.commons.components;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ScaleAnimatedTextView extends AppCompatTextView {
  private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateInterpolator();
  private static final float SCALE_FACTOR_OUT = 1.2f;
  private static final float SCALE_FACTOR_IN = 0.8f;

  private static final float DEFAULT_PIVOT_X = 0.5f;
  private static final float DEFAULT_PIVOT_Y = 0.5f;

  public static final int ANIM_TYPE_SCALE_OUT = 1;
  public static final int ANIM_TYPE_SCALE_IN = 0;

  @Retention(RetentionPolicy.SOURCE) @IntDef({ ANIM_TYPE_SCALE_OUT, ANIM_TYPE_SCALE_IN }) @interface Type {
  }

  public ScaleAnimatedTextView(@NonNull Context context) {
    super(context);
  }

  public ScaleAnimatedTextView(@NonNull Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ScaleAnimatedTextView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setText(@Nullable Spannable spannable, boolean animate, @Type int type) {
    if (animate) {
      float scaleFactor = type == ANIM_TYPE_SCALE_OUT ? SCALE_FACTOR_OUT : SCALE_FACTOR_IN;
      Animation animation =
          new ScaleAnimation(1.0f, scaleFactor, 1.0f, scaleFactor, Animation.RELATIVE_TO_SELF, DEFAULT_PIVOT_X, Animation.RELATIVE_TO_SELF,
              DEFAULT_PIVOT_Y);
      animation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
      animation.setRepeatMode(Animation.REVERSE);
      animation.setRepeatCount(1);
      animation.setInterpolator(DEFAULT_INTERPOLATOR);
      startAnimation(animation);
    }
    setText(spannable);
  }
}
