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

import net.devrieze.util.ComparableHandle
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.processModel.EndNode
import nl.adaptivity.process.processModel.StartNode
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.util.Identifier
import nl.adaptivity.util.Gettable
import org.jetbrains.spek.api.dsl.Dsl
import org.jetbrains.spek.api.dsl.Pending
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.w3c.dom.Node
import java.security.Principal
import kotlin.reflect.KProperty

/**
 * Created by pdvrieze on 30/12/16.
 */

class ProcessTestingDsl(private val delegate:Dsl, val transaction:StubProcessTransaction, val instanceHandle: HProcessInstance) : Dsl by delegate {

  fun <R> processGroup(description: String, pending: Pending = Pending.No, lazy: Boolean = false, body: ProcessTestingDsl.() -> R):R {
    var result: R? = null
    delegate.group(description, pending, lazy) { result = ProcessTestingDsl(this, transaction, instanceHandle).body() }
    return result!!
  }


  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun pdescribe(description: String, body: ProcessTestingDsl.() -> Unit) {
    processGroup("describe $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun pcontext(description: String, body: ProcessTestingDsl.() -> Unit) {
    processGroup("context $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun pgiven(description: String, body: ProcessTestingDsl.() -> Unit) {
    processGroup("given $description", body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun pon(description: String, body: ProcessTestingDsl.() -> Unit) {
    processGroup("on $description", lazy = true, body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun xdescribe(description: String, reason: String? = null, body: ProcessTestingDsl.() -> Unit) {
    processGroup("describe $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun xcontext(description: String, reason: String? = null, body: ProcessTestingDsl.() -> Unit) {
    processGroup("context $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun xgiven(description: String, reason: String? = null, body: ProcessTestingDsl.() -> Unit) {
    processGroup("given $description", Pending.Yes(reason), body = body)
  }

  /**
   * Creates a pending [group][Dsl.group].
   *
   * @author Ranie Jade Ramiso
   * @since 1.0
   */
  fun xon(description: String, reason: String? = null, body: ProcessTestingDsl.() -> Unit = {}) {
    processGroup("on $description", Pending.Yes(reason), lazy = true, body = body)
  }


  val instance: ProcessInstance get() {
    return transaction.readableEngineData.instance(instanceHandle).mustExist(instanceHandle).withPermission()
  }

  fun ProcessNodeInstance.take(): ProcessNodeInstance {
    return this.update(transaction.writableEngineData, instance) { state= NodeInstanceState.Taken }.node
  }

  fun ProcessNodeInstance.start(): ProcessNodeInstance {
    return startTask(transaction.writableEngineData, instance).node
  }

  fun ProcessNodeInstance.finish(payload: Node? = null): ProcessNodeInstance {
    return instance.finishTask(transaction.writableEngineData, this, payload).node
  }

  fun ProcessInstance.assertFinished() {
    Assertions.assertTrue(this.finished.isEmpty(), { "The list of finished nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertFinished(vararg nodeInstances: ProcessNodeInstance) {
    assertFinished(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertFinished(vararg nodeIds: String) {
    val finished = this.finished.map {
      val nodeInstance = transaction.readableEngineData.nodeInstance(it).withPermission()
      Assertions.assertTrue(nodeInstance.state.isFinal, { "The node instance state should be final (but is ${nodeInstance.state})" })
      Assertions.assertTrue(nodeInstance.node !is EndNode<*, *>, { "Completed nodes should not be endnodes" })
      nodeInstance.node.id
    }.sorted()
    Assertions.assertEquals(nodeIds.sorted(), finished, { "The list of finished nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertComplete() {
    Assertions.assertTrue(this.completedEndnodes.isEmpty(), { "The list of completed nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertComplete(vararg nodeInstances: ProcessNodeInstance) {
    assertComplete(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertComplete(vararg nodeIds: String) {
    val complete = completedEndnodes.map {
      val nodeInstance = transaction.readableEngineData.nodeInstance(it).withPermission()
      Assertions.assertTrue(nodeInstance.state.isFinal, { "The node instance state should be final (but is ${nodeInstance.state})" })
      Assertions.assertTrue(nodeInstance.node is EndNode<*, *>, "Completion nodes should be EndNodes")
      nodeInstance.node.id
    }.sorted()
    Assertions.assertEquals(nodeIds.sorted(), complete, { "The list of completed nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${complete.joinToString()}])" })
  }

  fun ProcessInstance.assertActive() {
    Assertions.assertTrue(this.active.isEmpty(), { "The list of active nodes is not empty (Expected: [], found: [${finished.joinToString()}])" })
  }

  fun ProcessInstance.assertActive(vararg nodeInstances: ProcessNodeInstance) {
    assertActive(*Array(nodeInstances.size, { nodeInstances[it].node.id }))
  }

  fun  ProcessInstance.assertActive(vararg nodeIds: String) {
    val active = active.map {
      val nodeInstance = transaction.readableEngineData.nodeInstance(it).withPermission()
      Assertions.assertTrue(nodeInstance.state.isActive, { "The node instance state should be active (but is ${nodeInstance.state})" })
      Assertions.assertFalse(nodeInstance.state.isFinal, { "The node instance state should not be final (but is ${nodeInstance.state})" })
      nodeInstance.node.id
    }.sorted()
    Assertions.assertEquals(nodeIds.sorted(), active, { "The list of active nodes does not match (Expected: [${nodeIds.joinToString()}], found: [${active.joinToString()}])" })
  }


  inner class ProcessNodeInstanceDelegate(val instanceHandle: ComparableHandle<out SecureObject<ProcessInstance>>, val nodeId: Identifier) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ProcessNodeInstance {
      return instance.getNodeInstance(nodeId)
          ?: kfail("The process node instance for node id $nodeId could not be found")
    }
  }

  val ProcessInstance.nodeInstance get() = object: Gettable<String, ProcessNodeInstanceDelegate> {
    operator override fun get(key:String): ProcessNodeInstanceDelegate {
      return ProcessNodeInstanceDelegate(getHandle(), Identifier(key))
    }
  }

  fun tracePossible(trace:Trace): Boolean {
    return instance.trace.mapIndexed { idx, id -> trace[idx]==id }.all { it }
  }

  fun assertTracePossible(trace: Trace) {
    instance.trace.forEachIndexed { idx, id -> assertEquals(trace[idx], id) }
  }


}

fun Dsl.trace(vararg trace:String):Trace {
  return trace
}

fun Dsl.testTraces(engine:ProcessEngine<StubProcessTransaction>, model:ExecutableProcessModel, owner: Principal, valid: List<Trace>, invalid:List<Trace>) {
  fun addStartedNodeContext(dsl: ProcessTestingDsl, trace: nl.adaptivity.process.engine.Trace, i: kotlin.Int):ProcessTestingDsl {
    val nodeId = trace[i]
    val nodeInstance by with(dsl) { instance.nodeInstance[nodeId] }
    dsl.test("$nodeId should not be in a final state if not an end node") {
      if (nodeInstance.node !is EndNode<*, *>) {
        assertFalse(nodeInstance.state.isFinal) { "The node ${nodeInstance.node.id} is in final state ${nodeInstance.state}" }
      }
    }
    dsl.it("should still be a possible trace") {
      dsl.assertTracePossible(trace)
    }
    dsl.test("$nodeId should be committed after starting") {
      if (nodeInstance.node !is EndNode<*, *>) {
        with(dsl) { nodeInstance.start() }
        assertTrue(nodeInstance.state.isCommitted)
        assertEquals(NodeInstanceState.Started, nodeInstance.state)
      }
    }

    return dsl.processGroup("After Finishing ${nodeId}") {
      beforeEachTest {
        if (nodeInstance.node !is EndNode<*, *>) {
          if (nodeInstance.state!=NodeInstanceState.Complete) {
            nodeInstance.finish()
          }
        }
      }
      if (i + 1 < trace.size) {
        addStartedNodeContext(this, trace, i + 1)
      } else {
        this
      }
    }
  }

  for(validTrace in valid) {
    givenProcess(engine, model, owner, description = validTrace.joinToString(prefix = "For trace: [", postfix = "]")) {
      it("should start in a valid state") {
        assertTrue(instance.finishedNodes.all { it.node is StartNode<*, *> })
        assertTracePossible(validTrace)
      }

      val startPos = instance.childNodes.size - 1
      addStartedNodeContext(this, validTrace, startPos).apply {
        it("should be finished correctly") {
          assertTracePossible(validTrace)
          val expectedCompletedNodes = validTrace.asSequence().map { model.getNode(it)!! }.filterIsInstance(EndNode::class.java).map { it.id!! }.toList().toTypedArray()
          val expectedFinishedNodes = validTrace.asSequence().map { model.getNode(it)!! }.filterNot { it is EndNode<*,*> }.map { it.id }.toList().toTypedArray()
          instance.assertFinished(*expectedFinishedNodes)
          instance.assertActive()
          instance.assertComplete(*expectedCompletedNodes)
        }
      }

    }
  }
  for(trace in invalid) {
    givenProcess(engine, model, owner, description = "For invalid trace: ${trace.joinToString(prefix = "[", postfix = "]")}") {
      test("Executing the trace should fail") {
        var success = false
        try {
          assertTracePossible(trace)
          val startPos = instance.childNodes.size - 1
          for( i in startPos until trace.size) {
            val nodeInstance by instance.nodeInstance[trace[i]]
            instance.finishTask(transaction.writableEngineData, nodeInstance, null)
          }
        } catch (e: AssertionError) {
          success = true
        }
        if (! success) kfail("The invalid trace $trace could be executed")
      }
    }
  }
}

val ProcessInstance.trace:Trace get(){
  return childNodes
      .sortedBy { it.withPermission().getHandle().handleValue }
      .map { it.withPermission().node.id }
      .toTypedArray()
}

typealias Trace = Array<out String>

fun Dsl.givenProcess(engine: ProcessEngine<StubProcessTransaction>, processModel: ExecutableProcessModel, principal: Principal, payload: Node? = null, description: String="Given a process instance", body: ProcessTestingDsl.() -> Unit) {
  val transaction = engine.startTransaction()
  val instance = with(transaction) {
    engine.testProcess(processModel, principal, payload)
  }

  group(description, body = { ProcessTestingDsl(this, transaction, instance.instanceHandle).body() })
}

fun kfail(message:String):Nothing {
  fail(message)
  throw UnsupportedOperationException("This code should not be reachable")
}