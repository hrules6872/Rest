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

package com.hrules.rest.presentation.presenters.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.preference.Preference;
import android.support.annotation.NonNull;
import com.hrules.darealmvp.DRPresenter;
import com.hrules.darealmvp.DRView;
import com.hrules.rest.App;
import com.hrules.rest.R;
import com.hrules.rest.commons.preferences.Preferences;

public class PreferenceFragmentPresenter extends DRPresenter<PreferenceFragmentPresenter.PreferenceView> {
  private Context appContext;

  @Override public void bind(@NonNull PreferenceView view) {
    super.bind(view);
    appContext = App.getAppContext();
  }

  public void onPreferenceClick(Preference preference) {
    Resources resources = appContext.getResources();
    String key = preference.getKey();
    if (resources.getString(R.string.prefs_appAboutKey).equals(key)) {
      getView().launchAboutActivity();
    } else if (resources.getString(R.string.prefs_appSendFeedbackKey).equals(key)) {
      getView().sendFeedbackByEmail();
    } else if (resources.getString(R.string.prefs_alertSoundKey).equals(key) || resources.getString(R.string.prefs_alertVibrateKey)
        .equals(key)) {
      checkSoundAndVibrateState();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  public void checkSoundAndVibrateState() {
    Preferences preferences = new Preferences(appContext);
    Resources resources = appContext.getResources();
    boolean prefsSound =
        preferences.getBoolean(resources.getString(R.string.prefs_alertSoundKey), resources.getBoolean(R.bool.prefs_alertSoundDefault));
    boolean prefsVibrate =
        preferences.getBoolean(resources.getString(R.string.prefs_alertVibrateKey), resources.getBoolean(R.bool.prefs_alertVibrateDefault));

    if (!prefsSound && !prefsVibrate) {
      getView().disableAllAlerts();
    } else {
      getView().enableAllAlerts();
    }
  }

  public interface PreferenceView extends DRView {
    void launchAboutActivity();

    void sendFeedbackByEmail();

    void disableAllAlerts();

    void enableAllAlerts();
  }
}
