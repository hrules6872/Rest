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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import com.hrules.rest.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RevealBackgroundView extends View {
  public static final int STATE_NOT_STARTED = 0;
  public static final int STATE_FILL_STARTED = 1;
  public static final int STATE_FINISHED = 2;

  @Retention(RetentionPolicy.SOURCE) @IntDef({ STATE_NOT_STARTED, STATE_FILL_STARTED, STATE_FINISHED })
  @interface State {
  }

  private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateInterpolator();

  private int state = STATE_NOT_STARTED;

  private Paint paintReveal;
  private int currentRadius;
  private ObjectAnimator revealAnimator;

  private int startLocationX;
  private int startLocationY;
  private int revealHeight;

  private int animDurationMilli;
  private Interpolator interpolator = DEFAULT_INTERPOLATOR;

  private OnStateChangeListener listener;

  public interface OnStateChangeListener {
    void onStateChange(int state);
  }

  public RevealBackgroundView(@NonNull Context context) {
    this(context, null, 0);
  }

  public RevealBackgroundView(@NonNull Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RevealBackgroundView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(@NonNull Context context, AttributeSet attrs) {
    final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RevealBackgroundView);
    int painRevealColor = typedArray.getColor(R.styleable.RevealBackgroundView_r_revealColor,
        ContextCompat.getColor(context, R.color.default_ProgressCountdownView_progressColor));
    typedArray.recycle();

    animDurationMilli = getResources().getInteger(android.R.integer.config_mediumAnimTime);

    paintReveal = new Paint();
    paintReveal.setStyle(Paint.Style.FILL);
    paintReveal.setColor(painRevealColor);
  }

  public void startFromLocation(int[] location) {
    startFromLocation(location, getHeight());
  }

  public void startFromLocation(int[] location, int revealHeight) {
    startFromLocation(location, revealHeight, 0);
  }

  public void startFromLocation(int[] location, int revealHeight, int startRadius) {
    changeState(STATE_FILL_STARTED);

    this.revealHeight = revealHeight;
    startLocationX = location[0];
    startLocationY = location[1];
    revealAnimator =
        ObjectAnimator.ofInt(this, "currentRadius", startRadius, revealHeight).setDuration(animDurationMilli);
    revealAnimator.setInterpolator(interpolator);
    revealAnimator.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animation) {
        changeState(STATE_FINISHED);
        revealAnimator.removeAllListeners();
      }
    });
    revealAnimator.start();
    setVisibility(VISIBLE);
  }

  public void startReverseFromLocation(int[] location) {
    startReverseFromLocation(location, getHeight());
  }

  public void startReverseFromLocation(int[] location, int revealHeight) {
    startReverseFromLocation(location, revealHeight, 0);
  }

  public void startReverseFromLocation(int[] location, int revealHeight, int startRadius) {
    changeState(STATE_FILL_STARTED);

    this.revealHeight = 0;
    startLocationX = location[0];
    startLocationY = location[1];
    revealAnimator =
        ObjectAnimator.ofInt(this, "currentRadius", revealHeight, startRadius).setDuration(animDurationMilli);
    revealAnimator.setInterpolator(interpolator);
    revealAnimator.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animation) {
        changeState(STATE_FINISHED);
        revealAnimator.removeAllListeners();
      }
    });
    revealAnimator.start();
    setVisibility(VISIBLE);
  }

  @Override protected void onDraw(Canvas canvas) {
    if (state == STATE_FINISHED) {
      canvas.drawRect(0, getHeight() - revealHeight, getWidth(), getHeight(), paintReveal);
    } else {
      canvas.drawCircle(startLocationX, startLocationY, currentRadius, paintReveal);
    }
  }

  private void changeState(int state) {
    if (this.state == state) {
      return;
    }

    this.state = state;
    if (listener != null) {
      listener.onStateChange(state);
    }
  }

  public void setOnStateChangeListener(@Nullable OnStateChangeListener listener) {
    this.listener = listener;
  }

  public @State int getState() {
    return state;
  }

  public void setState(@State int state) {
    this.state = state;
  }

  public int getCurrentRadius() {
    return currentRadius;
  }

  public void setCurrentRadius(int radius) {
    // do NOT delete! relative to ObjectAnimator
    this.currentRadius = radius;
    invalidate();
  }

  public void setRevealColor(@ColorRes int color) {
    paintReveal.setColor(ContextCompat.getColor(getContext(), color));
  }

  public int getAnimDurationMilli() {
    return animDurationMilli;
  }

  public void setAnimDurationMilli(int milli) {
    this.animDurationMilli = milli;
  }

  public Interpolator getInterpolator() {
    return interpolator;
  }

  public void setInterpolator(@NonNull Interpolator interpolator) {
    this.interpolator = interpolator;
  }
}