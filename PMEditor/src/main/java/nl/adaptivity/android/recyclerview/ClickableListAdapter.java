/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.android.recyclerview;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import nl.adaptivity.process.tasks.TaskItem;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by pdvrieze on 04/02/16.
 */
public abstract class ClickableListAdapter<T, VH extends ClickableViewHolder> extends Adapter<VH> implements ClickableAdapter<VH> {

  private final List<T> mContent = new ArrayList<>();
  private OnItemClickListener<? super VH> mItemClickListener;

  public ClickableListAdapter(@NonNull final List<T> content) {
    setItems(content);
  }

  @Override
  public void doClickView(final VH viewHolder) {
    if (mItemClickListener==null || (! mItemClickListener.onClickItem(this, viewHolder))) {
      onClickView(viewHolder);
    }
  }

  private void onClickView(final VH viewHolder) {}

  public boolean addItem(T item) {
    if (mContent.add(item)) {
      notifyItemInserted(mContent.size() - 1);
      return true;
    }
    return false;
  }

  @Override
  public OnItemClickListener getOnItemClickListener() {
    return mItemClickListener;
  }

  @Override
  public void setOnItemClickListener(final OnItemClickListener<? super VH> itemClickListener) {
    mItemClickListener = itemClickListener;
  }

  @Override
  public int getItemCount() {
    return mContent==null ? 0 : mContent.size();
  }

  public T getItem(int position) {
    return mContent.get(position);
  }

  public List<T> getContent() {
    return mContent;
  }

  public void setItems(@NonNull final List<? extends T> items) {
    mContent.clear();
    mContent.addAll(items);
    // The entire dataset changed
    notifyDataSetChanged();
  }

  public void setItem(final int itemPos, final T newItem) {
    mContent.set(itemPos, newItem);
    notifyItemChanged(itemPos);
  }

}