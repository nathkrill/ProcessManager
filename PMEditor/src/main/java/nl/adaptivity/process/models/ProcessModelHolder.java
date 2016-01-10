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

package nl.adaptivity.process.models;

import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.processModel.ProcessModel;

/**
 * Created by pdvrieze on 15/11/15.
 */
public class ProcessModelHolder {

  public final DrawableProcessModel model;
  public final Long handle;
  private final boolean mLoading;

  public ProcessModelHolder() {
    mLoading = true;
    this.model = null;
    this.handle = null;
  }

  public ProcessModelHolder(DrawableProcessModel model, Long handle) {
    mLoading = false;
    this.model = model;
    this.handle = handle;
  }

  public String getName() {
    return model==null ? null : model.getName();
  }

  public boolean isLoading() {
    return mLoading;
  }

  public boolean isFavourite() {
    return model==null ? false : model.isFavourite();
  }

  public void setFavourite(final boolean favourite) {
    model.setFavourite(favourite);
  }
}