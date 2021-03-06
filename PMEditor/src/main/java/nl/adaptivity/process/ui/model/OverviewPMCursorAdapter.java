/*
 * Copyright (c) 2018.
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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.ui.model;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import nl.adaptivity.diagram.android.AndroidStrategy;
import nl.adaptivity.diagram.android.AndroidTheme;
import nl.adaptivity.diagram.android.DrawableDrawable;
import nl.adaptivity.process.diagram.AbstractLayoutStepper;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.RootDrawableProcessModel;
import nl.adaptivity.process.diagram.RootDrawableProcessModel.Builder;
import nl.adaptivity.process.editor.android.R;
import nl.adaptivity.process.editor.android.databinding.OverviewModelListitemBinding;
import nl.adaptivity.process.models.ProcessModelProvider.ProcessModels;
import nl.adaptivity.process.ui.model.OverviewPMCursorAdapter.OverviewPMViewHolder;
import nl.adaptivity.xmlutil.XmlException;
import nl.adaptivity.xmlutil.XmlStreaming;

import java.io.StringReader;


/**
 * Created by pdvrieze on 28/12/15.
 */
public final class OverviewPMCursorAdapter extends BasePMCursorAdapter<OverviewPMViewHolder> {

  private static final String TAG = "OverviewPMCursorAdapter";

  public class OverviewPMViewHolder extends BasePMCursorAdapter<OverviewPMViewHolder>.BasePMViewHolder<OverviewModelListitemBinding> {

    public OverviewPMViewHolder(final LayoutInflater inflater, final ViewGroup parent) {
      super(R.layout.overview_model_listitem, inflater, parent);
    }
  }

  private int mCountColumn;
  private int mModelColumn;

  public OverviewPMCursorAdapter(final Context context, final Cursor c) {
    super(context, c, false);
  }

  @Override
  public OverviewPMViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
    return new OverviewPMViewHolder(mInflater, parent);
  }

  @Override
  protected void updateColumnIndices(final Cursor c) {
    super.updateColumnIndices(c);
    mCountColumn = c==null ? -1 : c.getColumnIndex(ProcessModels.COLUMN_INSTANCECOUNT);
    mModelColumn = c==null ? -1 : c.getColumnIndex(ProcessModels.COLUMN_MODEL);
  }

  protected void addPendingBindings(final OverviewPMViewHolder viewHolder, final Cursor cursor) {
    viewHolder.binding.setName(mNameColumn >= 0 ? cursor.getString(mNameColumn) : null);
    viewHolder.binding.setInstanceCount(mCountColumn>=0 ? cursor.getInt(mCountColumn): 0);
    if (mModelColumn>=0) {
      RootDrawableProcessModel.Builder model = RootDrawableProcessModel.deserialize(XmlStreaming.INSTANCE.newReader(new StringReader(cursor.getString(mModelColumn)))).builder();
      if (model.hasUnpositioned()) {
          model.layout(new AbstractLayoutStepper<>());
      }
      final Drawable d = new DrawableDrawable(model, new AndroidTheme(AndroidStrategy.INSTANCE), true);
      viewHolder.binding.setThumbnail(d);
    }
  }

}
