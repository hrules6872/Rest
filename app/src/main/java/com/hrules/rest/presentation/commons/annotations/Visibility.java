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

package com.hrules.rest.presentation.commons.annotations;

import android.support.annotation.IntDef;
import android.view.View;
import java.lang.annotation.Retention;

import static android.view.View.VISIBLE;
import static com.hrules.rest.presentation.commons.annotations.Visibility.GONE;
import static com.hrules.rest.presentation.commons.annotations.Visibility.INVISIBLE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE) @IntDef({ VISIBLE, INVISIBLE, GONE }) public @interface Visibility {
  int VISIBLE = View.VISIBLE;
  int INVISIBLE = View.INVISIBLE;
  int GONE = View.GONE;
}
