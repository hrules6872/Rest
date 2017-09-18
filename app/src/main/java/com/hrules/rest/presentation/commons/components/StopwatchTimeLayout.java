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
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.hrules.rest.R;
import com.hrules.rest.commons.SupportVersion;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.hrules.rest.R.id.text_stopwatch;

public final class StopwatchTimeLayout extends FrameLayout {
  @BindView(text_stopwatch) TextView textStopwatch;
  @BindView(R.id.text_stopwatchLast) TextView textStopwatchLast;

  public static final int STOPWATCH_SIZE_NORMAL = 0;
  public static final int STOPWATCH_SIZE_LARGE = 1;

  @Retention(RetentionPolicy.SOURCE) @IntDef({ STOPWATCH_SIZE_NORMAL, STOPWATCH_SIZE_LARGE }) public @interface Size {
  }

  private Unbinder unbinder;

  public StopwatchTimeLayout(Context context) {
    this(context, null);
  }

  public StopwatchTimeLayout(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StopwatchTimeLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    inflate(getContext(), R.layout.layout_stopwatch_time, this);
    unbinder = ButterKnife.bind(this);

    setSize(STOPWATCH_SIZE_NORMAL);
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  public void setText(@NonNull CharSequence charSequence) {
    textStopwatch.setText(charSequence);
  }

  public void setLastText(@NonNull CharSequence charSequence) {
    textStopwatchLast.setText(charSequence);
  }

  @SuppressWarnings("deprecation") public void setSize(@Size int size) {
    int primaryStyle = size == STOPWATCH_SIZE_NORMAL ? R.style.StopwatchPrimarySizeNormal : R.style.StopwatchPrimarySizeLarge;
    int secondaryStyle = size == STOPWATCH_SIZE_NORMAL ? R.style.StopwatchSecondarySizeNormal : R.style.StopwatchSecondarySizeLarge;

    if (SupportVersion.isMarshmallowOrAbove()) {
      textStopwatch.setTextAppearance(primaryStyle);
      textStopwatchLast.setTextAppearance(secondaryStyle);
    } else {
      textStopwatch.setTextAppearance(getContext(), primaryStyle);
      textStopwatchLast.setTextAppearance(getContext(), secondaryStyle);
    }
    invalidate();
  }
}
