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

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import nl.adaptivity.android.recyclerview.ClickableCursorAdapter.ClickableViewHolder;
import nl.adaptivity.android.util.CursorRecyclerViewAdapter;


/**
 * Created by pdvrieze on 28/12/15.
 */
public abstract class ClickableCursorAdapter<VH extends ClickableViewHolder> extends CursorRecyclerViewAdapter<VH> implements ClickableAdapter {

  public abstract class ClickableViewHolder extends ViewHolder implements OnClickListener {

    public ClickableViewHolder(final View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
    }

    public void onClick(final View v) {
      doClickView(this);
    }
  }

  private OnItemClickListener mItemClickListener;

  public ClickableCursorAdapter(final Context context, final Cursor cursor) {super(context, cursor);}

  private final void doClickView(final ViewHolder viewHolder) {
    if (mItemClickListener==null || (! mItemClickListener.onClickItem(this, viewHolder))) {
      onClickView(viewHolder);
    }
  }

  @Override
  public void onClickView(ViewHolder viewHolder) {};

  @Override
  public OnItemClickListener getOnItemClickListener() {
    return mItemClickListener;
  }

  @Override
  public void setOnItemClickListener(final OnItemClickListener itemClickListener) {
    mItemClickListener = itemClickListener;
  }
}
