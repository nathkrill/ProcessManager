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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel.engine;

import nl.adaptivity.process.processModel.*;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;


@XmlDeserializer(XmlJoin.Factory.class)
public class XmlJoin extends JoinBase<XmlProcessNode,XmlProcessModel> implements XmlProcessNode {

  public static class Builder extends JoinBase.Builder<XmlProcessNode, XmlProcessModel> implements XmlProcessNode.Builder {

    public Builder() {}

    public Builder(@NotNull final Join<?, ?> node) {
      super(node);
    }

    public Builder(@NotNull final Collection<? extends Identified> predecessors, @NotNull final Identified successor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results, final int min, final int max) {
      super(id, predecessors, successor, label, defines, results, min, max, x, y);
    }

    @NotNull
    @Override
    public XmlJoin build(@NotNull final ModelCommon<XmlProcessNode, XmlProcessModel> newOwner) {
      return new XmlJoin(this, newOwner);
    }
  }

  public static class Factory implements XmlDeserializerFactory<XmlJoin> {

    @NotNull
    @Override
    public XmlJoin deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlJoin.deserialize(null, reader);
    }
  }

  @NotNull
  public static XmlJoin deserialize(final XmlProcessModel ownerModel, @NotNull final XmlReader in) throws
          XmlException {
    return deserialize(in).build(ownerModel);
  }

  @NotNull
  public static XmlJoin.Builder deserialize(@NotNull final XmlReader in) throws
          XmlException {
    return XmlUtil.deserializeHelper(new XmlJoin.Builder(), in);
  }

  public XmlJoin(final XmlProcessModel ownerModel) {
    super(ownerModel);
  }

  public XmlJoin(@NotNull final Join.Builder<?, ?> builder, @NotNull final XmlProcessModel newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder builder() {
    return new Builder(this);
  }

  @Deprecated
  @Nullable
  Set<? extends Identifiable> getXmlPrececessors() {
    return getPredecessors();
  }

  @Deprecated
  void setXmlPrececessors(final List<? extends XmlProcessNode> pred) {
    swapPredecessors(pred);
  }

  @Override
  public void setOwnerModel(@NotNull final ModelCommon<XmlProcessNode, XmlProcessModel> newOwnerModel) {
    super.setOwnerModel(newOwnerModel);
  }

  @Override
  public void setPredecessors(@NotNull final Collection<? extends Identifiable> predecessors) {
    super.setPredecessors(predecessors);
  }

  @Override
  public void removePredecessor(@NotNull final Identified node) {
    super.removePredecessor(node);
  }

  @Override
  public void addPredecessor(final Identified nodeId) {
    super.addPredecessor(nodeId);
  }

  @Override
  public void addSuccessor(final Identified node) {
    super.addSuccessor(node);
  }

  @Override
  public void removeSuccessor(@NotNull final Identified node) {
    super.removeSuccessor(node);
  }

  @Override
  public void setSuccessors(@NotNull final Collection<? extends Identified> successors) {
    super.setSuccessors(successors);
  }

}
