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

package com.hrules.rest.presentation.commons.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import com.hrules.rest.presentation.presenters.extras.StopwatchPresenter;

import static com.hrules.rest.services.TimeService.ACTION_SERVICE_SHUTDOWN;

public final class StopwatchReceiver extends BroadcastReceiver {
  private final StopwatchPresenter presenter;

  public StopwatchReceiver(@NonNull StopwatchPresenter presenter) {
    this.presenter = presenter;
  }

  public void register(@NonNull Context context) {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ACTION_SERVICE_SHUTDOWN);
    context.registerReceiver(this, intentFilter);
  }

  public void unregister(@NonNull Context context) {
    try {
      context.unregisterReceiver(this);
    } catch (IllegalArgumentException ignored) {
    }
  }

  @SuppressWarnings("ConstantConditions") @Override public void onReceive(Context context, Intent intent) {
    if (presenter != null) {
      presenter.onStopwatchReceiverReceive(intent.getAction());
    }
  }
}
