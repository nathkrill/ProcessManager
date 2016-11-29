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
import nl.adaptivity.process.processModel.ProcessModelBase
import nl.adaptivity.process.processModel.Split
import nl.adaptivity.process.processModel.SplitBase
import nl.adaptivity.process.util.Identifiable
import nl.adaptivity.xml.*
import java.sql.SQLException


@XmlDeserializer(ExecutableSplit.Factory::class)
class ExecutableSplit : SplitBase<ExecutableProcessNode, ExecutableProcessModel>, ExecutableProcessNode {


  class ExecutableSplitFactory : ProcessModelBase.SplitFactory<ExecutableProcessNode, ExecutableProcessModel> {

    override fun createSplit(ownerModel: ExecutableProcessModel,
                             successors: Collection<Identifiable>): ExecutableSplit {
      val result = ExecutableSplit(ownerModel)
      result.setSuccessors(successors)
      return result
    }
  }


  class Factory : XmlDeserializerFactory<ExecutableSplit> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): ExecutableSplit {
      return ExecutableSplit.deserialize(null, reader)
    }
  }

  constructor(ownerModel: ExecutableProcessModel?, predecessor: ExecutableProcessNode, min: Int, max: Int)
        : super(ownerModel, listOf(predecessor), max, min)

  constructor(ownerModel: ExecutableProcessModel?) : super(ownerModel)

  constructor(orig: Split<*, *>) : super(orig)

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

    fun andSplit(ownerModel: ExecutableProcessModel, predecessor: ExecutableProcessNode): ExecutableSplit {
      return ExecutableSplit(ownerModel, predecessor, Integer.MAX_VALUE, Integer.MAX_VALUE)
    }


    @Throws(XmlException::class)
    fun deserialize(ownerModel: ExecutableProcessModel?, `in`: XmlReader): ExecutableSplit {
      return ExecutableSplit(ownerModel).deserializeHelper(`in`)
    }
  }

}