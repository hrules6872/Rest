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

package com.hrules.rest.presentation.models.base;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Favorite {
  private final String title;
  protected long seconds = 0;
  private final @Type int type;

  @IntDef({
      Type.ADD, Type.SECONDS
  }) @Retention(RetentionPolicy.RUNTIME) public @interface Type {
    int ADD = 0;
    int SECONDS = 1;
  }

  protected Favorite(@NonNull String title, @Type int type) {
    this.title = title;
    this.type = type;
  }

  public @NonNull String getTitle() {
    return title;
  }

  public long getSeconds() {
    return seconds;
  }

  public @Type int getType() {
    return type;
  }
}
