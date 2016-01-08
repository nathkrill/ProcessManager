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

package nl.adaptivity.gwt.ext.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.ListBox;
import nl.adaptivity.gwt.base.client.IWidgetController;

import java.util.ArrayList;
import java.util.Collection;


/**
 * A ListBox extension that controls widget enablement based on whether an item
 * is selected.
 * 
 * @author Paul de Vrieze
 */
public class ControllingListBox extends ListBox implements ChangeHandler, IWidgetController {

  private final Collection<FocusWidget> mWidgetsToEnable;

  public ControllingListBox() {
    setVisibleItemCount(10);
    addChangeHandler(this);
    mWidgetsToEnable = new ArrayList<FocusWidget>();
  }

  @Override
  public void onChange(final ChangeEvent event) {
    final boolean enabled = getSelectedIndex() >= 0;
    for (final FocusWidget widget : mWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * nl.adaptivity.gwt.ext.WidgetController#addControlledWidget(com.google.gwt
   * .user.client.ui.FocusWidget)
   */
  @Override
  public void addControlledWidget(final FocusWidget widget) {
    widget.setEnabled(getSelectedIndex() >= 0);
    mWidgetsToEnable.add(widget);
  }

  /*
   * (non-Javadoc)
   * @see
   * nl.adaptivity.gwt.ext.WidgetController#removeControlledWidget(com.google
   * .gwt.user.client.ui.FocusWidget)
   */
  @Override
  public boolean removeControlledWidget(final FocusWidget widget) {
    return mWidgetsToEnable.remove(widget);
  }

  @Override
  public void setSelectedIndex(final int index) {
    super.setSelectedIndex(index);
    final boolean enabled = index >= 0;
    for (final FocusWidget widget : mWidgetsToEnable) {
      widget.setEnabled(enabled);
    }
  }

}
