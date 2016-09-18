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

package com.hrules.rest.presentation.commons;

import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;

public class ViewUtils {
  private static int getStatusBarHeight(@NonNull Activity activity) {
    Rect rectangle = new Rect();
    Window window = activity.getWindow();
    window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
    int statusBarHeight = rectangle.top;
    int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
    return contentViewTop - statusBarHeight;
  }

  public static int[] getRevealStartPosition(@NonNull Activity activity, @NonNull View view) {
    final int[] startingLocation = new int[2];
    view.getLocationOnScreen(startingLocation);
    startingLocation[0] += view.getWidth() / 2;
    startingLocation[1] += (view.getHeight() / 2) + ViewUtils.getStatusBarHeight(activity);
    return startingLocation;
  }
}
