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

package com.hrules.rest.presentation.views.activities.support;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class AppCompatPreferenceActivity extends PreferenceActivity {
  private AppCompatDelegate appCompatDelegate;

  @Override protected void onCreate(Bundle savedInstanceState) {
    getDelegate().installViewFactory();
    getDelegate().onCreate(savedInstanceState);
    super.onCreate(savedInstanceState);
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    getDelegate().onPostCreate(savedInstanceState);
  }

  @Override public View onCreateView(String name, Context context, AttributeSet attrs) {
    final View result = super.onCreateView(name, context, attrs);
    if (result != null) {
      return result;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      switch (name) {
        case "Switch":
          return new SwitchCompat(this, attrs);
        case "TextView":
          return new AppCompatTextView(context, attrs);
        case "ImageView":
          return new AppCompatImageView(context, attrs);
        case "Button":
          return new AppCompatButton(context, attrs);
        case "EditText":
          return new AppCompatEditText(context, attrs);
        case "Spinner":
          return new AppCompatSpinner(context, attrs);
        case "ImageButton":
          return new AppCompatImageButton(context, attrs);
        case "CheckBox":
          return new AppCompatCheckBox(context, attrs);
        case "RadioButton":
          return new AppCompatRadioButton(context, attrs);
        case "CheckedTextView":
          return new AppCompatCheckedTextView(context, attrs);
        case "AutoCompleteTextView":
          return new AppCompatAutoCompleteTextView(context, attrs);
        case "MultiAutoCompleteTextView":
          return new AppCompatMultiAutoCompleteTextView(context, attrs);
        case "RatingBar":
          return new AppCompatRatingBar(context, attrs);
        case "SeekBar":
          return new AppCompatSeekBar(context, attrs);
      }
    }
    return null;
  }

  protected ActionBar getSupportActionBar() {
    return getDelegate().getSupportActionBar();
  }

  @NonNull @Override public MenuInflater getMenuInflater() {
    return getDelegate().getMenuInflater();
  }

  @Override public void setContentView(@LayoutRes int layoutResID) {
    getDelegate().setContentView(layoutResID);
  }

  @Override public void setContentView(View view) {
    getDelegate().setContentView(view);
  }

  @Override public void setContentView(View view, ViewGroup.LayoutParams params) {
    getDelegate().setContentView(view, params);
  }

  @Override public void addContentView(View view, ViewGroup.LayoutParams params) {
    getDelegate().addContentView(view, params);
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    getDelegate().onPostResume();
  }

  @Override protected void onTitleChanged(CharSequence title, int color) {
    super.onTitleChanged(title, color);
    getDelegate().setTitle(title);
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    getDelegate().onConfigurationChanged(newConfig);
  }

  @Override protected void onStop() {
    super.onStop();
    getDelegate().onStop();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    getDelegate().onDestroy();
  }

  @Override public void invalidateOptionsMenu() {
    getDelegate().invalidateOptionsMenu();
  }

  private AppCompatDelegate getDelegate() {
    if (appCompatDelegate == null) {
      appCompatDelegate = AppCompatDelegate.create(this, null);
    }
    return appCompatDelegate;
  }
}