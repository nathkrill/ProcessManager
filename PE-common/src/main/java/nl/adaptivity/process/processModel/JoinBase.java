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

package nl.adaptivity.process.processModel;

import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import nl.adaptivity.util.xml.XmlUtil;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.Collection;


/**
 * Created by pdvrieze on 26/11/15.
 */
public class JoinBase<T extends ProcessNode<T, M>, M extends ProcessModelBase<T,M>> extends JoinSplitBase<T, M> implements Join<T, M> {

  public static final String IDBASE = "join";

  public JoinBase(final M ownerModel, final Collection<? extends Identifiable> predecessors, final int max, final int min) {super(ownerModel, predecessors, max, min);}

  public JoinBase(final M ownerModel) {super(ownerModel);}

  public JoinBase(final Join<?, ?> orig) {
    super(orig);
  }

  @Override
  public final String getIdBase() {
    return IDBASE;
  }

  @Override
  public void serialize(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
    serializeAttributes(out);
    serializeChildren(out);
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  protected void serializeChildren(@NotNull final XmlWriter out) throws XmlException {
    super.serializeChildren(out);
    for(final Identifiable pred: getPredecessors()) {
      XmlUtil.writeStartElement(out, PREDELEMNAME);
      out.text(pred.getId());
      XmlUtil.writeEndElement(out, PREDELEMNAME);
    }
  }

  @Override
  public boolean deserializeChild(@NotNull final XmlReader in) throws XmlException {
    if (XmlUtil.isElement(in, PREDELEMNAME)) {
      final String id = XmlUtil.readSimpleElement(in).toString();
      addPredecessor(new Identifier(id));
      return true;
    }
    return super.deserializeChild(in);
  }

  @Override
  public final <R> R visit(@NotNull final Visitor<R> visitor) {
    return visitor.visitJoin(this);
  }

  @NotNull
  @Override
  public final QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public final int getMaxPredecessorCount() {
    return Integer.MAX_VALUE;
  }
}