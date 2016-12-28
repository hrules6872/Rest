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

package com.hrules.rest.presentation.commons;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import com.hrules.rest.R;
import com.hrules.rest.core.time.TimeManager;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {
  private static final float RELATIVESIZESPAN_MILLI = 0.8f;
  private static final String FORMAT_SECONDS_TWO_LEADING_ZEROS = "%02d";

  private TimeUtils() {
  }

  public static String milliToFavoriteMinutesSecondsString(long milli, @NonNull Resources resources) {
    long minutes = TimeUnit.MILLISECONDS.toMinutes(milli);
    long seconds =
        TimeUnit.MILLISECONDS.toSeconds(milli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli));
    return String.format(resources.getString(R.string.text_favoriteMinutesSecondsFormatted), minutes, seconds);
  }

  public static String milliToMinutesSecondsString(long milli, @NonNull Resources resources) {
    long minutes = TimeUnit.MILLISECONDS.toMinutes(milli);
    long seconds =
        TimeUnit.MILLISECONDS.toSeconds(milli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli));
    return String.format(resources.getString(R.string.text_timeMinutesSecondsFormatted), minutes, seconds);
  }

  public static Spannable milliToMinutesSecondsMilliString(long milli, @NonNull Resources resources) {
    long minutes = TimeUnit.MILLISECONDS.toMinutes(milli);
    long seconds =
        TimeUnit.MILLISECONDS.toSeconds(milli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli));
    long milliseconds = (milli % TimeUnit.SECONDS.toMillis(1)) / 10;

    String formatted =
        String.format(resources.getString(R.string.text_timeMinutesSecondsMilliFormatted), minutes, seconds,
            milliseconds);
    Spannable spannable = new SpannableString(formatted);
    spannable.setSpan(new RelativeSizeSpan(RELATIVESIZESPAN_MILLI), formatted.length() - 2, formatted.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  public static Spannable milliToStopwatchHoursMinutesSecondsMilliString(long milli, @NonNull Resources resources) {
    long hours = TimeUnit.MILLISECONDS.toHours(milli);
    long minutes =
        TimeUnit.MILLISECONDS.toMinutes(milli) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milli));
    long seconds =
        TimeUnit.MILLISECONDS.toSeconds(milli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli));
    long milliseconds = (milli % TimeUnit.SECONDS.toMillis(1)) / 10;

    String formatted =
        String.format(resources.getString(R.string.text_stopwatchHoursMinutesSecondsMilliFormatted), hours, minutes,
            seconds, milliseconds);
    Spannable spannable = new SpannableString(formatted);
    spannable.setSpan(new RelativeSizeSpan(RELATIVESIZESPAN_MILLI), formatted.length() - 2, formatted.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  public static long getExactMinutesFromMilli(long milli) {
    return TimeUnit.MILLISECONDS.toMinutes(milli);
  }

  public static long getRemainderSecondsFromMilli(long milli) {
    return TimeUnit.MILLISECONDS.toSeconds(milli) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli));
  }

  public static long getMilliFromMinutesSecond(long minutes, long seconds) {
    return TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds);
  }

  public static long getCountdownMilliUnsigned() {
    return Math.abs(TimeManager.INSTANCE.getCountdownTime() - TimeManager.INSTANCE.getElapsedTime());
  }

  public static String getSecondsFormattedWithLeadingZeros(long seconds) {
    return String.format(Locale.getDefault(), FORMAT_SECONDS_TWO_LEADING_ZEROS, seconds);
  }

  public static @ColorInt int getNotificationTextColorFromMilli(@NonNull Context context) {
    if (TimeManager.INSTANCE.isCountdownOver()) {
      return ContextCompat.getColor(context, R.color.notification_countDownTextOver);
    } else {
      return ContextCompat.getColor(context, R.color.notification_countDownText);
    }
  }

  public static @ColorInt int getTextColorFromMilli(@NonNull Context context) {
    if (TimeManager.INSTANCE.isCountdownOver()) {
      return ContextCompat.getColor(context, R.color.countDownTextOver);
    } else {
      return ContextCompat.getColor(context, R.color.countDownText);
    }
  }
}
