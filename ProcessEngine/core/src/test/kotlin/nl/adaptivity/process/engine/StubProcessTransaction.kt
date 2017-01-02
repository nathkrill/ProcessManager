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

package nl.adaptivity.process.engine

import nl.adaptivity.process.StubTransaction
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import org.w3c.dom.Node
import java.security.Principal
import java.util.*
import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 20/11/16.
 */
class StubProcessTransaction(private val engineData: IProcessEngineData<StubProcessTransaction>) : StubTransaction(), ProcessTransaction {
  override val readableEngineData: ProcessEngineDataAccess
    get() = engineData.createReadDelegate(this)
  override val writableEngineData: MutableProcessEngineDataAccess
    get() = engineData.createWriteDelegate(this)

  internal inner class InstanceWrapper(val instanceHandle: HProcessInstance) {

    operator fun invoke(): ProcessInstance { return readableEngineData.instance(instanceHandle).mustExist(instanceHandle).withPermission() }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) : ProcessInstance {
      return this()
    }

  }

  internal fun ProcessEngine<StubProcessTransaction>.testProcess(model: ExecutableProcessModel, owner: Principal, payload: Node? = null): InstanceWrapper {
    val modelHandle = addProcessModel(this@StubProcessTransaction, model, owner)
    val instanceHandle = startProcess(this@StubProcessTransaction, owner, modelHandle, "TestInstance", UUID.randomUUID(), payload)
    return InstanceWrapper(instanceHandle)
  }

}