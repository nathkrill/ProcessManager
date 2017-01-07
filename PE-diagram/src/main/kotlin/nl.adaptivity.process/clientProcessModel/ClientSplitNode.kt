/*
 * Copyright (c) 2017.
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

package nl.adaptivity.process.clientProcessModel

import nl.adaptivity.process.processModel.*
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.process.util.Identified


/**
 * A split node for clients. Note that ClientSplitNodes don't have a compat mode. They have multiple succesors
 * and compatibility concerns their absense.

 * @param <NodeT> The type of ProcessNode used.
</NodeT> */
abstract class ClientSplitNode<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?>(builder: Split.Builder<*, *>, newOwnerModel: ModelT) : SplitBase<NodeT, ModelT>(builder, newOwnerModel), ClientJoinSplit<NodeT, ModelT> {

  abstract class Builder<NodeT : ClientProcessNode<NodeT, ModelT>, ModelT : ClientProcessModel<NodeT, ModelT>?> : SplitBase.Builder<NodeT, ModelT>, ClientJoinSplit.Builder<NodeT, ModelT> {

    constructor() {}

    constructor(predecessors: Collection<Identified>, successors: Collection<Identified>, id: String?, label: String?, x: Double, y: Double, defines: Collection<IXmlDefineType>, results: Collection<IXmlResultType>, min: Int, max: Int) : super(id, predecessors, successors, label, defines, results, min, max, x, y) {}

    constructor(node: Split<*, *>) : super(node) {}

    abstract override fun build(newOwner: ModelT): ClientSplitNode<NodeT, ModelT>

    override var isCompat:Boolean
      get() = false
      set(compat: Boolean) {
        if (compat) throw IllegalArgumentException("Split nodes cannot be compatible with their own absense")
      }
  }

  abstract override fun builder(): Builder<NodeT, ModelT> /* {
    return new Builder<>(this);
  }*/

  override val maxSuccessorCount: Int
    get() = Integer.MAX_VALUE

  override val isCompat: Boolean
    get() = false

  override fun setId(id: String) = super.setId(id)

  override fun setLabel(label: String?) = super.setLabel(label)

  override fun setOwnerModel(newOwnerModel: ModelT) = super.setOwnerModel(newOwnerModel)

  override fun setPredecessors(predecessors: Collection<Identifiable>) = super.setPredecessors(predecessors)

  override fun removePredecessor(predecessorId: Identified) = super.removePredecessor(predecessorId)

  override fun addPredecessor(predecessorId: Identified) = super.addPredecessor(predecessorId)

  override fun addSuccessor(successorId: Identified) = super.addSuccessor(successorId)

  override fun removeSuccessor(successorId: Identified) = super.removeSuccessor(successorId)

  override fun setSuccessors(successors: Collection<Identified>) = super.setSuccessors(successors)

}