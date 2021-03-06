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

package com.hrules.rest.presentation.models.comparators;

import com.hrules.rest.presentation.models.base.Favorite;
import java.util.Comparator;

public final class FavoriteSecondsAscendingComparator implements Comparator<Favorite> {
  @Override public int compare(Favorite lhs, Favorite rhs) {
    return compare(lhs.getSeconds(), rhs.getSeconds());
  }

  private int compare(long lhs, long rhs) {
    return (lhs < rhs) ? -1 : ((lhs == rhs) ? 0 : 1);
  }
}
