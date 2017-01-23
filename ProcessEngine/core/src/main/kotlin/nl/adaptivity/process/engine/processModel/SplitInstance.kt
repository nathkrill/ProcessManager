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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.ComparableHandle
import net.devrieze.util.Handles
import net.devrieze.util.collection.replaceByNotNull
import net.devrieze.util.overlay
import net.devrieze.util.security.SecureObject
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.processModel.NodeInstanceState
import nl.adaptivity.process.processModel.Join
import nl.adaptivity.process.processModel.engine.ConditionResult
import nl.adaptivity.process.processModel.engine.ExecutableSplit
import org.w3c.dom.Node
import java.security.Principal

/**
 * Specialisation of process node instance for splits
 */
class SplitInstance : ProcessNodeInstance {

  interface Builder : ProcessNodeInstance.Builder<ExecutableSplit> {
    override fun build(): SplitInstance
    var predecessor: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>?
      get() = predecessors.firstOrNull()
      set(value) = predecessors.replaceByNotNull(value)
  }

  class ExtBuilder(instance:SplitInstance) : ProcessNodeInstance.ExtBuilderBase<ExecutableSplit>(instance), Builder {
    override var node: ExecutableSplit by overlay { instance.node }
    override fun build() = SplitInstance(this)
  }

  class BaseBuilder(
      node: ExecutableSplit,
      predecessor: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>,
      hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
      owner: Principal,
      handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
      entryNo: Int,
      state: NodeInstanceState = NodeInstanceState.Pending)
    : ProcessNodeInstance.BaseBuilder<ExecutableSplit>(node, listOf(predecessor), hProcessInstance, owner, entryNo,
                                                       handle, state), Builder {
    override fun build() = SplitInstance(this)
  }

  override val node: ExecutableSplit
    get() = super.node as ExecutableSplit


  val isFinished: Boolean
    get() = state == NodeInstanceState.Complete || state == NodeInstanceState.Failed

  @Suppress("UNCHECKED_CAST")
  override fun getHandle(): ComparableHandle<out SecureObject<SplitInstance>>
      = super.getHandle() as ComparableHandle<out SecureObject<SplitInstance>>

  constructor(node: ExecutableSplit,
              predecessor: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>>,
              hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>>,
              owner: Principal,
              handle: net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance>> = Handles.getInvalid(),
              state: NodeInstanceState = NodeInstanceState.Pending,
              results: Iterable<ProcessData> = emptyList(),
              entryNo: Int) :
      super(node, listOf(predecessor), hProcessInstance, owner, entryNo, handle, state, results) {
  }

  constructor(builder: SplitInstance.Builder): this(builder.node, builder.predecessor?: throw NullPointerException("Missing predecessor node instance"), builder.hProcessInstance, builder.owner, builder.handle, builder.state, builder.results, builder.entryNo)

  override fun builder(): ExtBuilder {
    return ExtBuilder(this)
  }

  override fun update(writableEngineData: MutableProcessEngineDataAccess,
                      body: ProcessNodeInstance.Builder<*>.() -> Unit): ProcessInstance.PNIPair<SplitInstance> {
    val instance = writableEngineData.instance(hProcessInstance).withPermission()
    val origHandle = getHandle()
    val builder = builder().apply(body)
    if (builder.changed) {
      if (origHandle.valid && getHandle().valid) {
        return instance.updateNode(writableEngineData, builder.build())
      } else {
        return ProcessInstance.PNIPair(instance, this)
      }
    } else {
      return ProcessInstance.PNIPair(instance, this)
    }
  }

  @JvmName("updateSplit")
  fun update(writableEngineData: MutableProcessEngineDataAccess, instance: ProcessInstance, body: Builder.() -> Unit): ProcessInstance.PNIPair<SplitInstance> {
    val origHandle = getHandle()
    val builder = builder().apply(body)
    if (builder.changed) {
      if (origHandle.valid && getHandle().valid) {
        return instance.updateNode(writableEngineData, builder.build())
      } else {
        return ProcessInstance.PNIPair(instance, this)
      }
    } else {
      return ProcessInstance.PNIPair(instance, this)
    }
  }

  private fun successorInstances(engineData: ProcessEngineDataAccess): Sequence<ProcessNodeInstance> {
    val instance = engineData.instance(hProcessInstance).withPermission()
    return node.successors
        .asSequence()
        .mapNotNull { instance.getChild(it.id)?.withPermission() }
  }

  private fun isActiveOrCompleted(it: ProcessNodeInstance): Boolean {
    return when (it.state) {
      NodeInstanceState.Started,
      NodeInstanceState.Acknowledged,
      NodeInstanceState.Skipped,
      NodeInstanceState.Failed,
      NodeInstanceState.Complete -> true
      else                                                                 -> false
    }
  }

  override fun startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): ProcessInstance.PNIPair<ProcessNodeInstance> {
    return update(engineData){ state= NodeInstanceState.Started }.let {
      it.node.updateState(engineData, it.instance)
    }
  }

  override fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node?): ProcessInstance.PNIPair<ProcessNodeInstance> {
    val committedSuccessors = successorInstances(engineData).filter { it.state.isCommitted }
    if (committedSuccessors.count()<node.min) {
      throw ProcessException("A split can only be finished once the minimum amount of children is committed")
    }
    return super.finishTask(engineData, processInstance, resultPayload)
  }

  internal fun updateState(engineData: MutableProcessEngineDataAccess, _processInstance: ProcessInstance): ProcessInstance.PNIPair<SplitInstance> {
    if (state.isFinal) { return ProcessInstance.PNIPair(_processInstance, this) }
    // XXX really needs fixing
    var processInstance = _processInstance
    val successorNodes = node.successors.map { node.ownerModel.getNode(it).mustExist(it) }
    var viableNodes: Int = 0

    var canStartMore = successorInstances(engineData).filter { isActiveOrCompleted(it) }.count() < node.max


    for (successor in successorNodes) {
      if (canStartMore) {
        if (successor is Join<*, *>) {
          throw IllegalStateException("Splits cannot be immediately followed by joins")
        }

        val nonRegisteredSuccessor = successor.createOrReuseInstance(engineData, processInstance, this, entryNo )
        // TODO Make this respond to MAYBEs
        if (nonRegisteredSuccessor.state== NodeInstanceState.Pending) {
          val conditionResult = nonRegisteredSuccessor.condition(engineData)
          when (conditionResult) {
            ConditionResult.TRUE -> { // only if it can be executed, otherwise just drop it.
              val pnipair = processInstance.addChild(engineData, nonRegisteredSuccessor)

              engineData.commit()

              processInstance = pnipair.node.provideTask(engineData, pnipair.instance).instance

              canStartMore = successorInstances(engineData).filter { isActiveOrCompleted(it) }.count() < node.max
            }
            ConditionResult.NEVER -> {
              val pnipair = processInstance.addChild(engineData, nonRegisteredSuccessor)
              processInstance = pnipair.instance.skipTask(engineData, pnipair.node).instance
            }
          }
        }
      }
      // in any case
      if (state == NodeInstanceState.Complete || ! state.isFinal) {
        viableNodes+=1
      }

    }
    if (viableNodes<node.min) { // No way to succeed, try to cancel anything that is not in a final state
      processInstance = successorInstances(engineData)
          .filter { ! it.state.isFinal }
          .fold(processInstance) { processInstance, it -> it.tryCancelTask(engineData, processInstance).instance }

      return update(engineData) { state = NodeInstanceState.Failed }
    }

    if (successorInstances(engineData).filter { isActiveOrCompleted(it) }.count()>=node.max) {
      // We have a maximum amount of successors
      processInstance = successorInstances(engineData)
          .filter { !isActiveOrCompleted(it) }
          .fold(processInstance) { processInstance, successor -> successor.cancelAndSkip(engineData, processInstance).instance }

      return update(engineData) { state = NodeInstanceState.Complete }
    }

    return ProcessInstance.PNIPair(processInstance, this) // the state is whatever it should be
  }
}