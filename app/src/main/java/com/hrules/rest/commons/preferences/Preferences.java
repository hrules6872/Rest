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

package com.hrules.rest.commons.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import java.util.Set;

public class Preferences implements PreferencesInterface {
  private final SharedPreferences preferences;

  public Preferences(Context context) {
    preferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public String getString(@NonNull String key, String defaultValue) {
    return preferences.getString(key, defaultValue);
  }

  public long getLong(@NonNull String key, long defaultValue) {
    return preferences.getLong(key, defaultValue);
  }

  @Override public boolean getBoolean(@NonNull String key, boolean defaultValue) {
    return preferences.getBoolean(key, defaultValue);
  }

  @Override public Set<String> getStringSet(@NonNull String key, Set<String> defValues) {
    return preferences.getStringSet(key, defValues);
  }

  @Override public void save(@NonNull String key, long value) {
    preferences.edit().putLong(key, value).apply();
  }

  @Override public void save(@NonNull String key, Set<String> values) {
    preferences.edit().putStringSet(key, values).apply();
  }

  @Override public void addListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
    preferences.registerOnSharedPreferenceChangeListener(listener);
  }

  @Override public void removeListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
    preferences.unregisterOnSharedPreferenceChangeListener(listener);
  }
}
