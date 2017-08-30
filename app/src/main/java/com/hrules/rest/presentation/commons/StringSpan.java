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

package com.hrules.rest.presentation.commons;

import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * +info: https://gist.github.com/hrules6872/f214f0391f20610f502c085c54b3079f
 */

public class StringSpan {
  private static final Pattern PATTERN = Pattern.compile("%([^a-zA-z%]*)([[a-zA-Z%]&&[^tT]]|[tT][a-zA-Z])");

  private StringSpan() {
  }

  public static CharSequence format(@NonNull CharSequence format, Object... args) {
    return format(Locale.getDefault(), format, args);
  }

  @SuppressWarnings("ConstantConditions")
  public static CharSequence format(@NonNull Locale locale, @NonNull CharSequence format, Object... args) {
    if (locale == null || format == null) {
      return format;
    }

    SpannableStringBuilder result = new SpannableStringBuilder(format);
    int startPosition = 0;
    int argPosition = -1;
    while (startPosition < result.length()) {
      Matcher matcher = PATTERN.matcher(result);
      if (!matcher.find(startPosition)) {
        break;
      }

      startPosition = matcher.start();
      int endPosition = matcher.end();
      String modifier = matcher.group(1);
      String type = matcher.group(2);

      argPosition++;
      CharSequence charSequence;
      if (type.equals("%")) {
        charSequence = "%";
      } else if (type.equals("n")) {
        charSequence = "\n";
      } else {
        Object arg = args[argPosition];
        charSequence = arg instanceof Spanned && type.equals("s") ? (Spanned) arg : String.format(locale, "%" + modifier + type, arg);
      }
      result.replace(startPosition, endPosition, charSequence);
      startPosition += charSequence.length();
    }
    return result;
  }
}
