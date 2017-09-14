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

package com.hrules.rest.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import static com.hrules.rest.AppConstants.ACTIONS.CHANGE_STATE;
import static com.hrules.rest.AppConstants.ACTIONS.EXIT;
import static com.hrules.rest.AppConstants.ACTIONS.REPLAY;

public final class TimeServiceReceiver extends BroadcastReceiver {
  private final TimeServiceReceiverListener listener;

  interface TimeServiceReceiverListener {
    void onActionStateChanged();

    void onActionReplay();

    void onActionExit();
  }

  public TimeServiceReceiver(@NonNull TimeServiceReceiverListener listener) {
    this.listener = listener;
  }

  @SuppressWarnings("ConstantConditions") @Override public void onReceive(@NonNull Context context, Intent intent) {
    switch (intent.getAction()) {
      case CHANGE_STATE:
        if (listener != null) {
          listener.onActionStateChanged();
        }
        break;

      case REPLAY:
        if (listener != null) {
          listener.onActionReplay();
        }
        break;

      case EXIT:
        if (listener != null) {
          listener.onActionExit();
        }
        break;
    }
  }
}
