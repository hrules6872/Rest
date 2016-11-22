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

package com.hrules.rest.presentation.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.hrules.rest.R;
import com.hrules.rest.presentation.models.base.Favorite;
import java.util.List;

public class FavoritesAdapter extends ArrayAdapter<Favorite> {
  private final FavoritesAdapterListener listener;

  public interface FavoritesAdapterListener {
    void onTitleClick(@NonNull Favorite favorite);

    void onDeleteClick(@NonNull Favorite favorite);
  }

  static class ViewHolder {
    @BindView(R.id.text_title) TextView textTitle;
    @Nullable @BindView(R.id.button_delete) ImageButton buttonDelete;

    ViewHolder(View convertView) {
      ButterKnife.bind(this, convertView);
    }
  }

  public FavoritesAdapter(@NonNull Context context, @NonNull List<Favorite> favorites,
      @NonNull FavoritesAdapterListener listener) {
    super(context, 0, favorites);
    this.listener = listener;
  }

  @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    final Favorite favorite = getItem(position);

    ViewHolder viewHolder;
    if (convertView == null) {
      int type = getItemViewType(position);
      convertView = getInflatedLayoutForType(type, parent);
      viewHolder = new ViewHolder(convertView);
      convertView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) convertView.getTag();
    }

    viewHolder.textTitle.setText(favorite.getTitle());
    viewHolder.textTitle.setOnClickListener(viewSource -> {
      if (listener != null) {
        listener.onTitleClick(favorite);
      }
    });

    if (viewHolder.buttonDelete != null) {
      viewHolder.buttonDelete.setOnClickListener(viewSource -> {
        if (listener != null) {
          listener.onDeleteClick(favorite);
        }
      });
    }

    return convertView;
  }

  @Override public int getItemViewType(int position) {
    return getItem(position).getType();
  }

  @Override public int getViewTypeCount() {
    return Favorite.Type.class.getMethods().length - 1;
  }

  private View getInflatedLayoutForType(int type, ViewGroup parent) {
    if (type == Favorite.Type.ADD) {
      return LayoutInflater.from(getContext()).inflate(R.layout.main_item_favorite_action, parent, false);
    } else if (type == Favorite.Type.SECONDS) {
      return LayoutInflater.from(getContext()).inflate(R.layout.main_item_favorite_seconds, parent, false);
    } else {
      throw new UnsupportedOperationException();
    }
  }
}
