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

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import java.util.Set;

interface PreferencesInterface {
  String getString(@NonNull String key, String defaultValue);

  long getLong(@NonNull String key, long defaultValue);

  boolean getBoolean(@NonNull String key, boolean defaultValue);

  Set<String> getStringSet(@NonNull String key, Set<String> defValues);

  void save(@NonNull String key, long value);

  void save(@NonNull String key, Set<String> values);

  void addListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener);

  void removeListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener);
}
