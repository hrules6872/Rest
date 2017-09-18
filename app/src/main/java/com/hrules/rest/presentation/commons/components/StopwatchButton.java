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
import android.util.AttributeSet;
import android.view.View;
import com.hrules.rest.R;

public final class StopwatchButton extends android.support.v7.widget.AppCompatImageButton {
  private boolean smart = false;

  public StopwatchButton(Context context) {
    this(context, null);
  }

  public StopwatchButton(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StopwatchButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    setImageResource(R.drawable.ic_play_stopwatch);
  }

  public void setSmart(boolean smart, boolean playing) {
    this.smart = smart;
    if (smart) {
      setEnabled(false);
      setVisibility(View.GONE);
    } else {
      if (playing) {
        setPlaying();
      } else {
        setStopped();
      }
    }
  }

  public void setPlaying() {
    if (!smart) {
      setEnabled(true);
      setVisibility(View.VISIBLE);
      setImageResource(R.drawable.ic_stop_stopwatch);
    }
  }

  public void setStopped() {
    if (!smart) {
      setEnabled(true);
      setVisibility(View.VISIBLE);
      setImageResource(R.drawable.ic_play_stopwatch);
    }
  }
}
