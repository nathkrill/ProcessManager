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

package nl.adaptivity.process.engine

import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import org.w3c.dom.Node
import java.net.URI
import java.security.Principal
import java.util.*
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 30/12/16.
 */
class TestControlPatterns: Spek({
  val localEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine", URI.create("http://localhost/"))
  val stubMessageService = StubMessageService(localEndpoint)
  val stubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
    override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
      return StubProcessTransaction(engineData)
    }
  }
  val principal = SimplePrincipal("pdvrieze")

  beforeEachTest {
    stubMessageService.clear()
  }

  val processEngine = ProcessEngine.newTestInstance(
      stubMessageService,
      stubTransactionFactory,
      TestProcessEngine.cacheModels<Any>(MemProcessModelMap(), 3),
      TestProcessEngine.cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
      TestProcessEngine.cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>(TestProcessEngine.PNI_SET_HANDLE), 2), true)

  describe("WFP1: A sequential process") {

    val model = ExecutableProcessModel.build {
      owner = principal
      val start = startNode { id="start" }
      val ac1 = activity { id="ac1"; predecessor = start }
      val ac2 = activity { id="ac2"; predecessor = ac1 }
      endNode { id="end"; predecessor = ac2 }
    }

    it("should have 4 children") {
      assertEquals(4, model.modelNodes.size)
    }

    testTraces(processEngine, model, principal, valid=listOf(trace("start", "ac1" , "ac2", "end")), invalid=listOf(trace("ac1", "ac2", "end"), trace("start", "ac2", "ac1", "end")))

  }
})

private inline fun <R> ProcessEngine<StubProcessTransaction>.testProcess(model: ExecutableProcessModel, owner: Principal, payload: Node? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R):R {
  startTransaction().use { transaction ->

    val modelHandle = addProcessModel(transaction, model, owner)
    val instanceHandle = startProcess(transaction, owner, modelHandle, "testInstance", UUID.randomUUID(), payload)

    return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
  }
}
