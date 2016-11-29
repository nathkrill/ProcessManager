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

package nl.adaptivity.process.processModel.engine

import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.ProcessTransaction
import nl.adaptivity.process.engine.processModel.IExecutableProcessNodeInstance
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.JoinBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.*
import java.sql.SQLException
import java.util.*


@XmlDeserializer(ExecutableJoin.Factory::class)
class ExecutableJoin : JoinBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {

  class Factory : XmlDeserializerFactory<ExecutableJoin> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableJoin {
      return ExecutableJoin.deserialize(null, reader)
    }
  }

  constructor(orig: Join<*, *>) : super(orig)

  constructor(ownerModel: ExecutableProcessModel?, predecessors: Collection<Identifiable>, min: Int, max: Int)
        : super(ownerModel, predecessors, max, min)

  constructor(ownerModel: ExecutableProcessModel?) : super(ownerModel)

  @Deprecated("")
  internal fun getXmlPrececessors(): Set<Identifiable>? {
    return predecessors
  }

  @Deprecated("")
  internal fun setXmlPrececessors(pred: List<ExecutableProcessNode>) {
    swapPredecessors(pred)
  }

  override fun <T : ProcessTransaction> condition(transaction: T,
                                                  instance: IExecutableProcessNodeInstance<*>): Boolean {
    return true
  }

  @Throws(SQLException::class)
  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> provideTask(transaction: T,
                                                                                              messageService: IMessageService<V, T, in U>,
                                                                                              instance: U): Boolean {
    return true
  }

  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> takeTask(messageService: IMessageService<V, T, in U>,
                                                                                           instance: U): Boolean {
    return true
  }

  override fun <V, T : ProcessTransaction, U : IExecutableProcessNodeInstance<U>> startTask(messageService: IMessageService<V, T, in U>,
                                                                                            instance: U): Boolean {
    return true
  }

  companion object {

    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel?, reader: XmlReader): ExecutableJoin {
      return ExecutableJoin(ownerModel).deserializeHelper(reader)
    }

    fun andJoin(ownerModel: ExecutableProcessModel, vararg predecessors: ExecutableProcessNode): ExecutableJoin {
      return ExecutableJoin(ownerModel, Arrays.asList(*predecessors), Integer.MAX_VALUE, Integer.MAX_VALUE)
    }
  }

}