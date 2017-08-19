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
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/*
 * +info: https://gist.github.com/romannurik/3982005
 */

public final class ToolTipView {
  public static final int LENGTH_SHORT = Toast.LENGTH_SHORT;
  public static final int LENGTH_LONG = Toast.LENGTH_LONG;

  @Retention(RetentionPolicy.SOURCE) @IntDef({ LENGTH_SHORT, LENGTH_LONG }) @interface Duration {
  }

  private static final int DEFAULT_ESTIMATED_TOAST_HEIGHT_DIPS = 48;

  private ToolTipView() {
  }

  public static void show(@NonNull View view, @NonNull CharSequence text, @Duration int duration) {
    if (TextUtils.isEmpty(text)) {
      return;
    }

    final int[] screenPos = new int[2];
    final Rect displayFrame = new Rect();
    view.getLocationOnScreen(screenPos);
    view.getWindowVisibleDisplayFrame(displayFrame);

    final Context context = view.getContext();
    final Resources resources = context.getResources();
    final int viewWidth = view.getWidth();
    final int viewHeight = view.getHeight();
    final int viewCenterX = screenPos[0] + viewWidth / 2;
    final int screenWidth = resources.getDisplayMetrics().widthPixels;
    final int estimatedToastHeight = (int) (DEFAULT_ESTIMATED_TOAST_HEIGHT_DIPS * resources.getDisplayMetrics().density);

    Toast toast = Toast.makeText(context, text, duration);
    boolean showBelow = screenPos[1] < estimatedToastHeight;
    if (showBelow) {
      toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, viewCenterX - screenWidth / 2,
          screenPos[1] - displayFrame.top + viewHeight);
    } else {
      toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, viewCenterX - screenWidth / 2,
          screenPos[1] - displayFrame.top - estimatedToastHeight);
    }
    toast.show();
  }
}