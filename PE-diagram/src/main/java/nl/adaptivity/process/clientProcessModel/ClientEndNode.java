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

package nl.adaptivity.process.clientProcessModel;

import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.EndNodeBase;
import nl.adaptivity.process.processModel.IXmlDefineType;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class ClientEndNode<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends EndNodeBase<T, M> implements EndNode<T, M>, ClientProcessNode<T, M> {

  public static class Builder<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends EndNodeBase.Builder<T,M> implements ClientProcessNode.Builder<T,M> {

    public Builder() {}

    public Builder(@Nullable final Identified predecessor, @Nullable final String id, @Nullable final String label, final double x, final double y, @NotNull final Collection<? extends IXmlDefineType> defines, @NotNull final Collection<? extends IXmlResultType> results) {
      super(predecessor, id, label, x, y, defines, results);
    }

    public Builder(@NotNull final EndNode<?, ?> node) {
      super(node);
    }

    @NotNull
    @Override
    public ClientEndNode<T, M> build(@Nullable final M newOwner) {
      return new ClientEndNode<T, M>(this, newOwner);
    }

    @Override
    public boolean isCompat() {
      return false;
    }

    @Override
    public void setCompat(final boolean compat) {
      if (compat) throw new IllegalArgumentException("Compatibility not supported on end nodes.");
    }

  }

  public ClientEndNode(final M ownerModel) {
    super(ownerModel);
  }

  public ClientEndNode(final M ownerModel, String id) {
    super(ownerModel);
    setId(id);
  }

  protected ClientEndNode(EndNode<?, ?> orig) {
    super(orig, null);
  }

  public ClientEndNode(@NotNull final EndNode.Builder<?, ?> builder, @NotNull final M newOwnerModel) {
    super(builder, newOwnerModel);
  }

  @NotNull
  @Override
  public Builder<T, M> builder() {
    return new Builder<>(this);
  }


  @Override
  public boolean isCompat() {
    return false;
  }


  @Override
  public void setOwnerModel(@NotNull final M ownerModel) {
    super.setOwnerModel(ownerModel);
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
