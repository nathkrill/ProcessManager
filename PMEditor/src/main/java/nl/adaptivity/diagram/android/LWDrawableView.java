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

package nl.adaptivity.diagram.android;

import nl.adaptivity.diagram.Drawable;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.diagram.Theme;
import android.graphics.Canvas;
import android.graphics.RectF;


public class LWDrawableView implements LightView{

  private Drawable mItem;
  /** Cached canvas */
  private AndroidCanvas mAndroidCanvas;

  public LWDrawableView(final Drawable item) {
    mItem = item;
  }

  @Override
  public void setFocussed(final boolean focussed) {
    if (focussed) {
      mItem.setState(mItem.getState()|Drawable.STATE_FOCUSSED);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_FOCUSSED);
    }
  }

  @Override
  public boolean isFocussed() {
    return (mItem.getState()&Drawable.STATE_FOCUSSED)!=0;
  }

  @Override
  public void setSelected(final boolean selected) {
    if (selected) {
      mItem.setState(mItem.getState()|Drawable.STATE_SELECTED);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_SELECTED);
    }
  }

  @Override
  public boolean isSelected() {
    return (mItem.getState()&Drawable.STATE_SELECTED)!=0;
  }

  @Override
  public void setTouched(final boolean touched) {
    if (touched) {
      mItem.setState(mItem.getState()|Drawable.STATE_TOUCHED);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_TOUCHED);
    }
  }

  @Override
  public boolean isTouched() {
    return (mItem.getState()&Drawable.STATE_TOUCHED)!=0;
  }

  @Override
  public void setActive(final boolean active) {
    if (active) {
      mItem.setState(mItem.getState()|Drawable.STATE_ACTIVE);
    } else {
      mItem.setState(mItem.getState()& ~Drawable.STATE_ACTIVE);
    }
  }

  @Override
  public boolean isActive() {
    return (mItem.getState()&Drawable.STATE_ACTIVE)!=0;
  }

  @Override
  public void getBounds(final RectF rect) {
    final Rectangle bounds = mItem.getBounds();
    rect.set(bounds.leftf(), bounds.topf(), bounds.rightf(), bounds.bottomf());
  }


  /**
   * Craw this drawable onto an android canvas. The canvas has an ofset
   * preapplied so the top left of the drawing is 0,0. This method itself,
   * just creates the canvas and passes the work to {@link #onDraw(IAndroidCanvas, Rectangle)}
   */
  @Override
  public final void draw(final Canvas canvas, final Theme<AndroidStrategy, AndroidPen, AndroidPath> theme, final double scale) {
    if (mAndroidCanvas==null) {
      mAndroidCanvas=new AndroidCanvas(canvas, theme);
    } else {
      mAndroidCanvas.setCanvas(canvas);
    }
    onDraw(mAndroidCanvas.scale(scale), null);
  }

  protected void onDraw(final IAndroidCanvas androidCanvas, final Rectangle clipBounds) {
    mItem.draw(androidCanvas, clipBounds);
  }


  @Override
  public void move(final float x, final float y) {
    mItem.translate(x, y);
  }

  @Override
  public void setPos(final float left, final float top) {
    mItem.setPos(left, top);
  }

  public Drawable getItem() {
    return mItem;
  }
}
