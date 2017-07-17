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

package com.hrules.rest.presentation.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import com.hrules.rest.App;
import com.hrules.rest.R;
import com.hrules.rest.commons.Preferences;
import com.hrules.rest.presentation.commons.AppUtils;
import com.hrules.rest.presentation.views.activities.AboutActivityView;

public class PreferenceFragmentView extends PreferenceFragment
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initializeViews();
  }

  private void initializeViews() {
    bindPreference(findPreference(getString(R.string.prefs_appAboutKey)));
    bindPreference(findPreference(getString(R.string.prefs_alertSoundKey)));
    bindPreference(findPreference(getString(R.string.prefs_alertVibrateKey)));
    bindPreference(findPreference(getString(R.string.prefs_appSendFeedbackKey)));

    // bind summary to value
    bindPreferenceSummaryToValue(findPreference(getString(R.string.prefs_displayOrientationKey)));

    // check states
    checkSoundAndVibrateState();
  }

  private void bindPreference(@NonNull Preference preference) {
    preference.setOnPreferenceClickListener(this);
  }

  private void bindPreferenceSummaryToValue(@NonNull Preference preference) {
    preference.setOnPreferenceChangeListener(this);
    onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(preference.getKey(), ""));
  }

  @Override public boolean onPreferenceClick(Preference preference) {
    String key = preference.getKey();
    if (getString(R.string.prefs_appAboutKey).equals(key)) {
      launchAboutActivity();
    } else if (getString(R.string.prefs_appSendFeedbackKey).equals(key)) {
      sendFeedbackByEmail();
    } else if (getString(R.string.prefs_alertSoundKey).equals(key) || getString(R.string.prefs_alertVibrateKey).equals(key)) {
      checkSoundAndVibrateState();
    } else {
      return false;
    }
    return true;
  }

  @Override public boolean onPreferenceChange(Preference preference, Object value) {
    String stringValue = value.toString();

    if (preference instanceof ListPreference) {
      ListPreference listPreference = (ListPreference) preference;
      int index = listPreference.findIndexOfValue(stringValue);

      preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
    }
    return true;
  }

  private void checkSoundAndVibrateState() {
    Preferences preferences = new Preferences(App.getAppContext());
    boolean prefsSound =
        preferences.getBoolean(getString(R.string.prefs_alertSoundKey), getResources().getBoolean(R.bool.prefs_alertSoundDefault));
    boolean prefsVibrate =
        preferences.getBoolean(getString(R.string.prefs_alertVibrateKey), getResources().getBoolean(R.bool.prefs_alertVibrateDefault));

    if (!prefsSound && !prefsVibrate) {
      disableAllAlerts();
    } else {
      enableAllAlerts();
    }
  }

  private void launchAboutActivity() {
    startActivity(new Intent(getActivity(), AboutActivityView.class));
  }

  private void sendFeedbackByEmail() {
    AppUtils.sendFeedbackByEmail(getActivity());
  }

  private void disableAllAlerts() {
    findPreference(getString(R.string.prefs_alertHalfwayKey)).setEnabled(false);
    findPreference(getString(R.string.prefs_alertTenSecondsKey)).setEnabled(false);
    findPreference(getString(R.string.prefs_alertThreeSecondsKey)).setEnabled(false);
  }

  private void enableAllAlerts() {
    findPreference(getString(R.string.prefs_alertHalfwayKey)).setEnabled(true);
    findPreference(getString(R.string.prefs_alertTenSecondsKey)).setEnabled(true);
    findPreference(getString(R.string.prefs_alertThreeSecondsKey)).setEnabled(true);
  }
}