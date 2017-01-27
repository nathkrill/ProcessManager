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

package nl.adaptivity.process.engine.processModel

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.process.IMessageService
import nl.adaptivity.process.engine.*
import nl.adaptivity.process.engine.ProcessInstance.PNIPair
import nl.adaptivity.process.processModel.Activity
import nl.adaptivity.process.processModel.engine.ExecutableActivity
import nl.adaptivity.process.processModel.engine.ExecutableJoin
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.XMLFragmentStreamReader
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import java.io.CharArrayWriter
import java.security.Principal
import java.sql.SQLException
import java.util.concurrent.Future
import java.util.logging.Level
import javax.xml.transform.Result
import javax.xml.transform.Source

/**
 * Created by pdvrieze on 27/01/17.
 */
abstract class ProcessNodeInstance<T: ProcessNodeInstance<T>>(open val node: ExecutableProcessNode,
                                                              predecessors: Collection<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
                                                              val hProcessInstance: ComparableHandle<SecureObject<ProcessInstance>>,
                                                              override val owner: Principal,
                                                              val entryNo: Int,
                                                              private var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
                                                              val state: NodeInstanceState = NodeInstanceState.Pending,
                                                              results: Iterable<ProcessData> = emptyList(),
                                                              val failureCause: Throwable? = null) : SecureObject<ProcessNodeInstance<T>>, ReadableHandleAware<SecureObject<ProcessNodeInstance<*>>> {
  val results: List<ProcessData> = results.toList()
  val directPredecessors: Set<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>> = predecessors.asSequence().filter { it.valid }.toArraySet()

  init {
    if (entryNo!=1 && !(node.isMultiInstance || ((node as? ExecutableJoin)?.isMultiMerge ?: false))) throw ProcessException("Attempting to create a new instance ${entryNo} for node ${node} that does not support reentry")
  }

  constructor(builder: ProcessNodeInstance.Builder<*, T>) : this(builder.node, builder.predecessors,
                                                                  builder.hProcessInstance, builder.owner,
                                                                  builder.entryNo, builder.handle, builder.state,
                                                                  builder.results, builder.failureCause)


  override fun getHandle() = handle

  abstract fun builder(processInstanceBuilder: ProcessInstance.Builder): ExtBuilder<out ExecutableProcessNode, T>
  fun precedingClosure(processData: ProcessEngineDataAccess): Sequence<SecureObject<ProcessNodeInstance<*>>> {
    return directPredecessors.asSequence().flatMap { predHandle ->
      val pred = processData.nodeInstance(predHandle).withPermission()
      pred.precedingClosure(processData) + sequenceOf(pred)
    }
  }

    /** Update the node. This will store the update based on the transaction. It will return the new object. The old object
   *  may be invalid afterwards.
   */
  fun update(writableEngineData: MutableProcessEngineDataAccess,
                  body: Builder<out ExecutableProcessNode, T>.() -> Unit): PNIPair<T> {
    var nodeFuture: Future<out T>? = null
    val newInstance = writableEngineData.instance(hProcessInstance).withPermission().update(writableEngineData) {
      nodeFuture = update(this, body)
    }

    @Suppress("UNCHECKED_CAST")
    val newNode = nodeFuture?.get() ?: this as T

    return PNIPair<T>(newInstance, newNode).also { newPair ->
      assert(newPair.node == writableEngineData.nodeInstance(this@ProcessNodeInstance.getHandle())) { "The returned node and the stored node don't match for node ${newPair.node.node.id}-${newPair.node.handle}(${newPair.node.state})" }
      assert(newPair.instance.getChild(newPair.node.node.id)==newPair.node) { "The instance node and the node don't match for node ${newPair.node.node.id}-${newPair.node.handle}(${newPair.node.state})" }
    }
  }

  fun update(processInstanceBuilder: ProcessInstance.Builder, body: Builder<out ExecutableProcessNode, T>.() -> Unit): Future<out T>? {
    val builder = builder(processInstanceBuilder).apply(body)

    return processInstanceBuilder.storeChild(builder).let { if (builder !is ExtBuilder<*,*> || builder.changed) it else null }
  }

  private inline val asT get() = this as T

  override fun withPermission(): ProcessNodeInstance<T> = this

  @Throws(SQLException::class)
  open fun tickle(engineData: MutableProcessEngineDataAccess, instance: ProcessInstance, messageService: IMessageService<*>): PNIPair<T> {
    return when (state) {
      NodeInstanceState.FailRetry,
      NodeInstanceState.Pending -> provideTask(engineData, instance)
      else                      -> PNIPair(instance, asT)
    }// ignore
  }

  @Throws(SQLException::class)
  fun getResult(engineData: ProcessEngineDataAccess, name: String): ProcessData? {
    return results.firstOrNull { name == it.name }
  }

  @Throws(SQLException::class)
  fun getDefines(engineData: ProcessEngineDataAccess): List<ProcessData> {
    return node.defines.map {
      it.applyData(engineData, this)
    }
  }

  private fun hasDirectPredecessor(handle: ComparableHandle<out SecureObject<ProcessNodeInstance<*>>>): Boolean {
    for (pred in directPredecessors) {
      if (pred.handleValue == handle.handleValue) {
        return true
      }
    }
    return false
  }

  @Throws(SQLException::class)
  fun resolvePredecessors(engineData: ProcessEngineDataAccess): Collection<ProcessNodeInstance<*>> {
    return directPredecessors.asSequence().map {
            engineData.nodeInstance(it).withPermission()
          }.toList()
  }

  @Throws(SQLException::class)
  fun getPredecessor(engineData: ProcessEngineDataAccess, nodeName: String): ComparableHandle<SecureObject<ProcessNodeInstance<*>>>? {
    // TODO Use process structure knowledge to do this better/faster without as many database lookups.
    directPredecessors
          .asSequence()
          .map { engineData.nodeInstance(it).withPermission() }
          .forEach {
            if (nodeName == it.node.id) {
              return it.getHandle()
            } else {
              val result = it.getPredecessor(engineData, nodeName)
              if (result != null) {
                return result
              }
            }
          }
    return null
  }

  @Throws(SQLException::class)
  fun resolvePredecessor(engineData: ProcessEngineDataAccess, nodeName: String): ProcessNodeInstance<*>? {
    val handle = getPredecessor(engineData, nodeName) ?: throw NullPointerException("Missing predecessor with name ${nodeName} referenced from node ${node.id}")
    return engineData.nodeInstances[handle]?.withPermission()
  }

  fun getHandleValue(): Long {
    return handle.handleValue
  }

  fun condition(engineData: ProcessEngineDataAccess) = node.condition(engineData, this)
  @Throws(SQLException::class)
  open fun provideTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {

    val node = this.node // Create a local copy to prevent races - and shut up Kotlin about the possibilities as it should be immutable

    fun <MSG_T> impl(messageService: IMessageService<MSG_T>): PNIPair<T> {

      val shouldProgress = tryCreate(engineData, processInstance) {
        node.provideTask(engineData, processInstance, this)
      }

      if (node is ExecutableActivity) {
        val preparedMessage = messageService.createMessage(node.message)
        if (! tryCreate(engineData, processInstance) { messageService.sendMessage(engineData, preparedMessage, this) }) {
          failTaskCreation(engineData, processInstance, MessagingException("Failure to send message"))
        }
      }

      val pniPair = run { // Unfortunately sendMessage will invalidate the current instance
        val newInstance = engineData.instance(hProcessInstance).withPermission()
        val newNodeInstance = engineData.nodeInstance(getHandle()).withPermission() as T
        newNodeInstance.update(engineData) { state = NodeInstanceState.Sent }.apply { engineData.commit() }
      }
      if (shouldProgress) {
        return ProcessInstance.Updater(pniPair.instance).takeTask(engineData, pniPair.node)
      } else
        return pniPair

    }

    return impl(engineData.messageService())
  }

  @Throws(SQLException::class)
  open fun startTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    val startNext = tryTask(engineData, processInstance) { node.startTask(this) }
    val updatedInstances = update(engineData) { state = NodeInstanceState.Started }
    return if (startNext) {
      updatedInstances.instance.finishTask(engineData, updatedInstances.node, null)
    } else updatedInstances
  }

  @Throws(SQLException::class)
  @Deprecated("This is dangerous, it will not update the instance")
  internal open fun finishTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, resultPayload: Node? = null): PNIPair<T> {
    if (state.isFinal) {
      throw ProcessException("instance ${node.id}:${getHandle()}(${state}) cannot be finished as it is already in a final state.")
    }
    return update(engineData) {
      node.results.mapTo(results.apply{clear()}) { it.apply(resultPayload) }
      state = NodeInstanceState.Complete
    }.apply { engineData.commit() }
  }

  fun cancelAndSkip(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    return when (state) {
      NodeInstanceState.Pending,
      NodeInstanceState.FailRetry    -> update(engineData) { state = NodeInstanceState.Skipped }
      NodeInstanceState.Sent,
      NodeInstanceState.Taken,
      NodeInstanceState.Acknowledged ->
          cancelTask(engineData, processInstance).update(engineData) { state = NodeInstanceState.Skipped }
      else                           -> PNIPair(processInstance, asT)
    }
  }

  @Throws(SQLException::class)
  open fun cancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    return update(engineData) { state = NodeInstanceState.Cancelled }
  }

  @Throws(SQLException::class)
  open fun tryCancelTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance): PNIPair<T> {
    try {
      return cancelTask(engineData, processInstance)
    } catch (e: IllegalArgumentException) {
      DefaultProcessNodeInstance.logger.log(Level.WARNING, "Task could not be cancelled")
      return PNIPair(processInstance, asT)
    }
  }

  override fun toString(): String {
    return "${node.javaClass.simpleName}  (${getHandle()}, ${node.id}[$entryNo] - $state)"
  }

  @Throws(SQLException::class)
  open fun failTask(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable): PNIPair<T> {
    return update(engineData) {
      failureCause = cause
      state = if (state == NodeInstanceState.Pending) NodeInstanceState.FailRetry else NodeInstanceState.Failed
    }
  }

  @Throws(SQLException::class)
  open fun failTaskCreation(engineData: MutableProcessEngineDataAccess, processInstance: ProcessInstance, cause: Throwable): PNIPair<T> {
    return update(engineData) {
      failureCause = cause
      state = NodeInstanceState.FailRetry
    }.apply { engineData.commit() }
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 source: Source,
                                 result: Result,
                                 localEndpoint: EndpointDescriptor) {
    instantiateXmlPlaceholders(engineData, source, true, localEndpoint)
  }

  @Throws(XmlException::class, SQLException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 xmlReader: XmlReader,
                                 out: XmlWriter,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor) {
    val defines = getDefines(engineData)
    val transformer = PETransformer.create(ProcessNodeInstanceContext(this,
                                                                      defines,
                                                                      state == NodeInstanceState.Complete, localEndpoint),
                                           removeWhitespace)
    transformer.transform(xmlReader, out.filterSubstream())
  }

  @Throws(SQLException::class, XmlException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 source: Source,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): CompactFragment {
    val xmlReader = XmlStreaming.newReader(source)
    return instantiateXmlPlaceholders(engineData, xmlReader, removeWhitespace, localEndpoint)
  }

  @Throws(XmlException::class)
  fun instantiateXmlPlaceholders(engineData: ProcessEngineDataAccess,
                                 xmlReader: XmlReader,
                                 removeWhitespace: Boolean,
                                 localEndpoint: EndpointDescriptor): WritableCompactFragment {
    val caw = CharArrayWriter()

    val writer = XmlStreaming.newWriter(caw, true)
    instantiateXmlPlaceholders(engineData, xmlReader, writer, removeWhitespace, localEndpoint)
    writer.close()
    return WritableCompactFragment(emptyList<Namespace>(), caw.toCharArray())
  }

  @Throws(XmlException::class)
  fun serialize(engineData: ProcessEngineDataAccess, out: XmlWriter, localEndpoint: EndpointDescriptor) {
    out.smartStartTag(XmlProcessNodeInstance.ELEMENTNAME) {
      writeAttribute("state", state.name)
      writeAttribute("processinstance", hProcessInstance.handleValue)

      if (handle.valid) writeAttribute("handle", handle.handleValue)

      writeAttribute("nodeid", node.id)

      directPredecessors.forEach { writeSimpleElement(XmlProcessNodeInstance.PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      serializeAll(results)

      (node as? Activity<*, *>)?.message?.messageBody?.let { body ->
        instantiateXmlPlaceholders(engineData, XMLFragmentStreamReader.from(body), out, true, localEndpoint)
      }
    }
  }

  protected inline fun <R> tryCreate(engineData: MutableProcessEngineDataAccess,
                                     processInstance: ProcessInstance,
                                     body: () -> R): R =
    DefaultProcessNodeInstance._tryHelper(engineData,
                                          processInstance,
                                          body) { d, i, e ->
      failTaskCreation(d, i, e)
    }

  protected inline fun <R> tryTask(engineData: MutableProcessEngineDataAccess,
                                   processInstance: ProcessInstance,
                                   body: () -> R): R = DefaultProcessNodeInstance._tryHelper(
    engineData, processInstance, body) { d, i, e -> failTask(d, i, e) }

  interface Builder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>> {
    var node: N
    val predecessors: MutableSet<ComparableHandle<out SecureObject<ProcessNodeInstance<*>>>>
    val processInstanceBuilder: ProcessInstance.Builder
    val hProcessInstance: ComparableHandle<out SecureObject<ProcessInstance>> get() = processInstanceBuilder.handle
    var owner: Principal
    var handle: ComparableHandle<out SecureObject<out ProcessNodeInstance<*>>>
    var state: NodeInstanceState
    val results: MutableList<ProcessData>
    fun toXmlInstance(body: CompactFragment?): XmlProcessNodeInstance
    val entryNo: Int
    var failureCause: Throwable?
    fun  build(): T
    // Cancel the instance
  }

  abstract class AbstractBuilder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>> : Builder<N, T> {

    override fun toXmlInstance(body: CompactFragment?): XmlProcessNodeInstance {
      return XmlProcessNodeInstance(nodeId= node.id,
                                    predecessors = predecessors.map { Handles.handle<XmlProcessNodeInstance>(it.handleValue) },
                                    processInstance = hProcessInstance.handleValue,
                                    handle = Handles.handle(handle.handleValue),
                                    state = state,
                                    results = results,
                                    body = body)
    }

    override var failureCause: Throwable? = null
  }

  abstract class BaseBuilder<N:ExecutableProcessNode, T: ProcessNodeInstance<T>>(
    final override var node: N,
    predecessors: Iterable<ComparableHandle<SecureObject<ProcessNodeInstance<*>>>>,
    final override val processInstanceBuilder: ProcessInstance.Builder,
    final override var owner: Principal,
    final override val entryNo: Int,
    final override var handle: ComparableHandle<SecureObject<ProcessNodeInstance<*>>> = Handles.getInvalid(),
    final override var state: NodeInstanceState = NodeInstanceState.Pending) : AbstractBuilder<N, T>() {

    final override var predecessors :MutableSet<net.devrieze.util.ComparableHandle<out SecureObject<ProcessNodeInstance<*>>>> = predecessors.toMutableArraySet()

    final override val results = mutableListOf<ProcessData>()
  }

  abstract class ExtBuilder<N: ExecutableProcessNode, T: ProcessNodeInstance<*>>(protected val base: T, override val processInstanceBuilder: ProcessInstance.Builder) : AbstractBuilder<N, T>() {
    protected final val observer = { changed = true }

    final override var predecessors = ObservableSet(base.directPredecessors.toMutableArraySet(), { changed = true })
    final override var owner by overlay(observer) { base.owner }
    final override var handle: ComparableHandle<out SecureObject<ProcessNodeInstance<*>>> by overlay(observer) { base.getHandle() }
    final override var state by overlay(observer) { base.state }
    final override var results = ObservableList(base.results.toMutableList(), { changed = true })
    final var changed: Boolean = false
    final override val entryNo: Int = base.entryNo

    override abstract fun build(): T

    fun failTaskCreation(cause: Throwable) {
      failureCause = cause
      state = NodeInstanceState.FailRetry
    }

    protected inline fun <R> tryCreate(body: () -> R): R = DefaultProcessNodeInstance._tryHelper(
      body) { e -> failTaskCreation(e) }

  }

}