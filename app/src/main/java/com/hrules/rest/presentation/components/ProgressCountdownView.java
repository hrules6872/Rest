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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import com.hrules.rest.R;

public class ProgressCountdownView extends View {
  private int height;
  private int width;

  private long maxProgress;
  private long currentProgress;

  private Paint paintProgress;
  private Paint paintBackground;

  public ProgressCountdownView(@NonNull Context context) {
    this(context, null, 0);
  }

  public ProgressCountdownView(@NonNull Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ProgressCountdownView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(@NonNull final Context context, final AttributeSet attrs) {
    final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressCountdownView);
    Resources res = getResources();
    currentProgress = typedArray.getInt(R.styleable.ProgressCountdownView_p_currentProgress,
        res.getInteger(R.integer.default_ProgressCountdownView_currentProgress));
    maxProgress = typedArray.getInt(R.styleable.ProgressCountdownView_p_maxProgress,
        res.getInteger(R.integer.default_ProgressCountdownView_maxProgress));
    int paintBackgroundColor = typedArray.getColor(R.styleable.ProgressCountdownView_p_backgroundColor,
        ContextCompat.getColor(context, R.color.default_ProgressCountdownView_backgroundColor));
    int paintProgressColor = typedArray.getColor(R.styleable.ProgressCountdownView_p_progressColor,
        ContextCompat.getColor(context, R.color.default_ProgressCountdownView_progressColor));
    typedArray.recycle();

    paintBackground = new Paint();
    paintBackground.setColor(paintBackgroundColor);
    paintBackground.setStyle(Paint.Style.FILL);

    paintProgress = new Paint();
    paintProgress.setColor(paintProgressColor);
    paintProgress.setStyle(Paint.Style.FILL);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    width = MeasureSpec.getSize(widthMeasureSpec);
    height = MeasureSpec.getSize(heightMeasureSpec);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawRect(0, 0, width, height, paintBackground);
    canvas.drawRect(0, getTopPosition(currentProgress), width, height, paintProgress);
  }

  public int getTopPosition(long currentProgress) {
    return (int) ((currentProgress * height) / maxProgress);
  }

  public long getMaxProgress() {
    return maxProgress;
  }

  public void setMaxProgress(long maxProgress) {
    this.maxProgress = maxProgress;
    if (maxProgress < currentProgress) {
      currentProgress = maxProgress;
    }
    invalidate();
  }

  public long getCurrentProgress() {
    return currentProgress;
  }

  public void setCurrentProgress(long currentProgress) {
    this.currentProgress = (currentProgress <= maxProgress) ? currentProgress : maxProgress;
    invalidate();
  }

  public void setPaintBackground(@ColorRes int color) {
    paintBackground.setColor(ContextCompat.getColor(getContext(), color));
    invalidate();
  }

  public void setPaintProgress(@ColorRes int color) {
    paintProgress.setColor(ContextCompat.getColor(getContext(), color));
    invalidate();
  }
}
