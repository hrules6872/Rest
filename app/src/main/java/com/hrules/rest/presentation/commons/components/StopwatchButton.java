package com.hrules.rest.presentation.commons.components;

import android.content.Context;
import android.util.AttributeSet;
import com.hrules.rest.R;

public class StopwatchButton extends android.support.v7.widget.AppCompatImageButton {
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
      setImageResource(R.drawable.ic_smart_stopwatch);
    } else {
      if (playing) {
        setPlaying();
      } else {
        setStopped();
      }
    }
    setEnabled(!smart);
  }

  public void setPlaying() {
    if (!smart) {
      setImageResource(R.drawable.ic_stop_stopwatch);
    }
  }

  public void setStopped() {
    if (!smart) {
      setImageResource(R.drawable.ic_play_stopwatch);
    }
  }
}
