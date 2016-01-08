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

import android.support.v7.widget.RecyclerView.ViewHolder;


/**
 * Created by pdvrieze on 04/01/16.
 */
public interface ClickableAdapter {

  public interface OnItemClickListener {

    /**
     * Callback to handle item clicks. This method can stop processing of the click by returning true.
     * @param adapter The adapter that caused the event
     * @param viewHolder The underlying viewHolder for the event.
     * @return true if the click has been absorbed, false if not.
     */
    boolean onClickItem(ClickableAdapter adapter, ViewHolder viewHolder);
  }

  void onClickView(ViewHolder viewHolder);

  OnItemClickListener getOnItemClickListener();

  void setOnItemClickListener(OnItemClickListener itemClickListener);
}
