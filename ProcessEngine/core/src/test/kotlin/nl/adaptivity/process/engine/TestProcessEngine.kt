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

import net.devrieze.util.*
import net.devrieze.util.security.SecureObject
import net.devrieze.util.security.SimplePrincipal
import net.devrieze.util.security.withPermission
import nl.adaptivity.messaging.EndpointDescriptor
import nl.adaptivity.messaging.EndpointDescriptorImpl
import nl.adaptivity.process.MemTransactionedHandleMap
import nl.adaptivity.process.engine.ProcessInstance.State
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.engine.processModel.JoinInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance.Builder
import nl.adaptivity.process.processModel.XmlMessage
import nl.adaptivity.process.processModel.engine.ExecutableProcessModel
import nl.adaptivity.process.processModel.engine.ExecutableProcessNode
import nl.adaptivity.process.processModel.engine.ExecutableStartNode
import nl.adaptivity.process.processModel.engine.IProcessModelRef
import nl.adaptivity.util.activation.Sources
import nl.adaptivity.xml.*
import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.XMLUnit
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.w3c.dom.Document
import org.w3c.dom.Node

import javax.xml.bind.JAXB
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource

import java.io.*
import java.net.URI
import java.util.UUID

import java.nio.charset.Charset.defaultCharset
import org.custommonkey.xmlunit.XMLAssert.assertXMLEqual
import org.testng.Assert.*
import uk.ac.bournemouth.ac.db.darwin.processengine.ProcessEngineDB.pnipredecessors.predecessor


/**
 * Created by pdvrieze on 18/08/15.
 */
class TestProcessEngine {

  internal lateinit var mProcessEngine: ProcessEngine<StubProcessTransaction>
  private val mLocalEndpoint = EndpointDescriptorImpl(QName.valueOf("processEngine"), "processEngine", URI.create("http://localhost/"))
  private val mStubMessageService: StubMessageService<ProcessTransaction> = StubMessageService(mLocalEndpoint)
  private val mStubTransactionFactory = object : ProcessTransactionFactory<StubProcessTransaction> {
    override fun startTransaction(engineData: IProcessEngineData<StubProcessTransaction>): StubProcessTransaction {
      return StubProcessTransaction(engineData)
    }
  }
  private val mPrincipal = SimplePrincipal("pdvrieze")

  private fun getXml(name: String): InputStream? {
    try {
      javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name).use { reader ->
        val out = ByteArrayOutputStream()
        if (!InputStreamOutputStream.getInputStreamOutputStream(reader, out).get()) {
          return null
        }
        val byteArray = out.toByteArray()
        assertTrue(byteArray.size > 0, "Some bytes in the xml files are expected")
        val bais = ByteArrayInputStream(byteArray)
        return bais
      }
    } catch (e: Exception) {
      if (e is RuntimeException) {
        throw e
      }
      throw RuntimeException(e)
    }

  }

  @Throws(XmlException::class)
  private fun getStream(name: String): XmlReader {
    return XmlStreaming.newReader(getXml(name)!!, "UTF-8")
  }

  @Throws(XmlException::class)
  private fun getProcessModel(name: String): ExecutableProcessModel {
    return ExecutableProcessModel.deserialize(getStream(name))
  }

  private fun getDocument(name: String): Document {
    try {
      javaClass.getResourceAsStream("/nl/adaptivity/process/engine/test/" + name).use { reader -> return documentBuilder.parse(reader) }
    } catch (e: Exception) {
      if (e is RuntimeException) {
        throw e
      }
      throw RuntimeException(e)
    }

  }

  @Throws(XmlException::class)
  private fun serializeToXmlCharArray(obj: Any): CharArray {
    return CharArrayWriter().apply {
      val caw = this
      if (obj is XmlSerializable) {
        XmlStreaming.newWriter(this).use { writer ->
          obj.serialize(writer)
        }
      } else {
        JAXB.marshal(obj, caw)
      }
    }.toCharArray()
  }

  private fun <R> testProcess(model: ExecutableProcessModel, payload: Node? = null, body: (ProcessTransaction, ExecutableProcessModel, HProcessInstance) -> R):R {
    mProcessEngine.startTransaction().use { transaction ->

      val modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal)
      val instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance", UUID.randomUUID(), payload)

      return body(transaction, transaction.readableEngineData.processModel(modelHandle).mustExist(modelHandle).withPermission(), instanceHandle)
    }
  }

  private fun ProcessTransaction.getInstance(instanceHandle: HProcessInstance):ProcessInstance {
    return readableEngineData.instance(instanceHandle).withPermission()
  }

  private fun StubMessageService<ProcessTransaction>.messageNode(transaction: ProcessTransaction, index:Int): ProcessNodeInstance {
    return transaction.readableEngineData.nodeInstance(this._messages[index].source).withPermission()
  }

  private fun ProcessInstance.child(transaction: ProcessTransaction, name: String) : ProcessNodeInstance {
    return getChild(transaction, name)?.withPermission() ?: throw AssertionError("No node instance for node id ${name} found")
  }

  private val ProcessInstance.sortedFinished
    get() = finished.sortedBy { it.handleValue }

  private val ProcessInstance.sortedActive
    get() = active.sortedBy { it.handleValue }

  private val ProcessInstance.sortedCompleted
    get() = completedEndnodes.sortedBy { it.handleValue }

  private fun ProcessInstance.assertFinishedHandles(vararg handles: ComparableHandle<ProcessNodeInstance>) = apply {
    assertEquals(ArrayList(sortedFinished), handles.sorted())
  }

  private fun ProcessInstance.assertFinished(vararg handles: ProcessNodeInstance) = apply {
    assertEquals(ArrayList(sortedFinished), handles.asSequence().map{ it.handle }.sortedBy { it.handleValue }.toList())
  }

  private fun ProcessInstance.assertActiveHandles(vararg handles: ComparableHandle<ProcessNodeInstance>) = apply {
    assertEquals(ArrayList(sortedActive), handles.sorted())
  }

  private fun ProcessInstance.assertActive(vararg handles: ProcessNodeInstance) = apply {
    assertEquals(ArrayList(sortedActive), handles.asSequence().map{ it.handle }.sortedBy { it.handleValue }.toList())
  }

  private fun ProcessInstance.assertCompletedHandles(vararg handles: ComparableHandle<ProcessNodeInstance>) = apply {
    assertEquals(ArrayList(sortedCompleted), handles.sorted())
  }

  private fun ProcessInstance.assertCompleted(vararg handles: ProcessNodeInstance): ProcessInstance {
    val actual = ArrayList(sortedCompleted)
    val expected = handles.asSequence().map { it.handle }.sortedBy { it.handleValue }.toList()
    assertEquals(actual, expected)
    return this
  }

  private fun ProcessNodeInstance.assertStarted() = apply {
    assertEquals(this.state, NodeInstanceState.Started)
  }

  private fun ProcessInstance.assertIsStarted() = apply {
    assertEquals(this.state, State.STARTED)
  }

  private fun ProcessInstance.assertIsFinished() = apply {
    assertEquals(this.state, State.FINISHED)
  }

  private fun ProcessNodeInstance.assertSent() = apply {
    assertEquals(this.state, NodeInstanceState.Sent)
  }

  private fun ProcessNodeInstance.assertPending() = apply {
    assertEquals(this.state, NodeInstanceState.Pending)
  }

  private fun ProcessNodeInstance.assertAcknowledged() = apply {
    assertEquals(this.state, NodeInstanceState.Acknowledged)
  }

  private fun ProcessNodeInstance.assertComplete() = apply {
    assertEquals(this.state, NodeInstanceState.Complete)
  }

  @BeforeMethod
  fun beforeTest() {
    mStubMessageService.clear()
    //    DelegateProcessEngineData<StubProcessTransaction> engineData =
    //            new DelegateProcessEngineData<>(mStubTransactionFactory,
    //                                            cache(new MemProcessModelMap(), 1),
    //                                            cache(new MemTransactionedHandleMap<>(), 1),
    //                                            cache(new MemTransactionedHandleMap<>(), 2));

    mProcessEngine = ProcessEngine.newTestInstance(
        mStubMessageService,
        mStubTransactionFactory,
        cacheModels<Any>(MemProcessModelMap(), 1),
        cacheInstances(MemTransactionedHandleMap<SecureObject<ProcessInstance>, StubProcessTransaction>(), 1),
        cacheNodes<Any>(MemTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>(PNI_SET_HANDLE), 2), true)
  }

  @Test
  @Throws(Exception::class)
  fun testExecuteSingleActivity() {
    val model = getProcessModel("testModel1.xml")
    val transaction = mProcessEngine.startTransaction()
    val modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal)

    val instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null)

    assertEquals(mStubMessageService._messages.size, 1)
    assertEquals(mStubMessageService.getMessageNode(0).handleValue, 1L)

    val expected = getXml("testModel1_task1.xml")

    val receivedChars = serializeToXmlCharArray(mStubMessageService._messages[0].base)

    XMLUnit.setIgnoreWhitespace(true)
    try {
      assertXMLEqual(InputStreamReader(expected!!), CharArrayReader(receivedChars))
    } catch (e: AssertionError) {
      e.printStackTrace()
      try {
        assertEquals(String(receivedChars), Streams.toString(getXml("testModel1_task1.xml"), defaultCharset()))
      } catch (f: Exception) {
        f.initCause(e)
        throw f
      }

    }

    run {
      val processInstance = transaction.getInstance(instanceHandle).assertIsStarted()

      assertEquals(processInstance.active.size, 1)

      processInstance.child(transaction, "start").assertComplete().let { start ->
        processInstance.assertFinished(start)
        assertTrue(start.node is ExecutableStartNode)
      }

      processInstance.assertCompleted() // no completions

      val taskNode = mStubMessageService.messageNode(transaction, 0)
      taskNode.assertSent()
      processInstance.assertActive(taskNode)
      processInstance.finishTask(transaction, mStubMessageService, taskNode, null).assertComplete()
    }

    run {
      val processInstance = transaction.getInstance(instanceHandle).assertIsFinished()
      val start = processInstance.child(transaction, "start")
      val ac = processInstance.child(transaction, "ac2")
      val end = processInstance.child(transaction, "end")
      processInstance.assertActive()
      processInstance.assertFinished(start, ac)
      processInstance.assertCompleted(end)

    }
  }

  @Test
  fun testSplitJoin1() {
    testProcess(simpleSplitModel) { transaction, model, instanceHandle ->
      run {
        val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
        val start = instance.child(transaction, "start")
        run {
          val split = instance.child(transaction, "split1").assertStarted()
          val ac1 = instance.child(transaction, "ac1").assertPending()
          val ac2 = instance.child(transaction, "ac2").assertPending()

          instance.assertFinished(start)
          instance.assertActive(split, ac1, ac2)

          run {
            val messageSources = mStubMessageService._messages
                .map { transaction.readableEngineData.nodeInstance(it.source).withPermission() }
                .sortedBy { it.node.id }
            assertEquals(messageSources, listOf(ac1, ac2))
            mStubMessageService._messages.forEach { msg ->
              msg.source
            }
          }

          instance.finishTask(transaction, mStubMessageService, ac1, null).assertComplete()
        }
        run {
          val split = instance.child(transaction, "split1").assertStarted()
          val ac1 = instance.child(transaction, "ac1").assertComplete()
          val ac2 = instance.child(transaction, "ac2").assertAcknowledged()
          val join = instance.child(transaction, "join1").assertStarted()
          instance.assertActive(ac2, join, split)
          instance.assertFinished(ac1, start)

          ac2.startTask(transaction, mStubMessageService)
        }
        run {
          val split = instance.child(transaction, "split1").assertComplete()
          val ac1 = instance.child(transaction, "ac1").assertComplete()
          val ac2 = instance.child(transaction, "ac2").assertStarted()
          val join = instance.child(transaction, "join1").assertStarted()

          instance.finishTask(transaction, mStubMessageService, ac2, null).assertComplete()
        }

        run {
          val split = instance.child(transaction, "split1").assertComplete()
          val ac1 = instance.child(transaction, "ac1").assertComplete()
          val ac2 = instance.child(transaction, "ac2").assertComplete()
          val join = instance.child(transaction, "join1").assertComplete()
          val end = instance.child(transaction, "end").assertComplete()

          assertEquals(instance.active.size, 0)
          instance.assertFinished(start, split, ac1, ac2, join, end)
        }

      }
      run {
        val instance = transaction.readableEngineData.instance(instanceHandle).withPermission()
        assertEquals(instance.state, State.FINISHED)
      }
    }

  }

  @Test
  @Throws(Exception::class)
  fun testGetDataFromTask() {
    val model = getProcessModel("testModel2.xml")
    val transaction = mProcessEngine.startTransaction()
    val modelHandle = mProcessEngine.addProcessModel(transaction, model, mPrincipal)

    val instanceHandle = mProcessEngine.startProcess(transaction, mPrincipal, modelHandle, "testInstance1", UUID.randomUUID(), null)

    assertEquals(mStubMessageService._messages.size, 1)

    XMLUnit.setIgnoreWhitespace(true)
    assertXMLEqual(InputStreamReader(getXml("testModel2_task1.xml")!!), CharArrayReader(serializeToXmlCharArray(mStubMessageService
        ._messages[0].base)))
    var ac1: ProcessNodeInstance = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal) ?: throw AssertionError("Message node not found")// This should be 0 as it's the first activity


    mStubMessageService.clear() // (Process the message)
    assertEquals(ac1.results.size, 0)
    ac1 = mProcessEngine.finishTask(transaction, ac1.handle, getDocument("testModel2_response1.xml"), mPrincipal)
    assertEquals(ac1.state, NodeInstanceState.Complete)
    ac1 = mProcessEngine.getNodeInstance(transaction, ac1.handle, mPrincipal) ?: throw AssertionError("Node ${ac1.handle} not found")
    assertEquals(ac1.results.size, 2)
    val result1 = ac1.results[0]
    val result2 = ac1.results[1]
    assertEquals(result1.name, "name")
    assertEquals(result1.content.contentString, "Paul")
    assertEquals(result2.name, "user")
    assertXMLEqual("<user><fullname>Paul</fullname></user>", result2.content.contentString)

    assertEquals(mStubMessageService._messages.size, 1)
    assertEquals(mStubMessageService.getMessageNode(0).handleValue, 2L) //We should have a new message with the new task (with the data)
    val ac2 = mProcessEngine.getNodeInstance(transaction, mStubMessageService.getMessageNode(0), mPrincipal)

    val ac2Defines = ac2!!.getDefines(transaction)
    assertEquals(ac2Defines.size, 1)


    val define = ac2Defines[0]
    assertEquals(define.name, "mylabel")
    assertEquals(define.content.contentString, "Hi Paul. Welcome!")

  }

  private val simpleSplitModel: ExecutableProcessModel get() {
    return ExecutableProcessModel.build {
      owner = mPrincipal
      val start = startNode {
        id = "start"
      }
      val split1 = split {
        predecessor = start.identifier
        id = "split1"
        min = 2
        max = 2
      }
      val ac1 = activity {
        predecessor = split1
        id = "ac1"
        message = XmlMessage()
        result {
          name = "ac1result"
          content = "ac1content".toCharArray()
        }
      }
      val ac2 = activity {
        predecessor = split1
        id = "ac2"
        message = XmlMessage()
        result {
          name = "ac2result"
          content = "ac2content".toCharArray()
        }
      }
      val join = join {
        predecessors(ac1, ac2)
        id = "join1"
      }
      endNode {
        id = "end"
        predecessor = join
      }
    }
  }

  companion object {

    private val PNI_SET_HANDLE = fun(pni: SecureObject<out ProcessNodeInstance>, handle: Long?): SecureObject<out ProcessNodeInstance> {
      if (pni.withPermission().getHandleValue() == handle) {
        return pni
      }
      val builder = pni.withPermission().builder()
      builder.handle = Handles.handle<SecureObject<ProcessNodeInstance>>(handle!!)
      return builder.build()
    }

    private fun <V:Any> cacheInstances(base: MutableTransactionedHandleMap<V, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<V, StubProcessTransaction> {
      return CachingHandleMap<V, StubProcessTransaction>(base, count)
    }

    private fun <V> cacheNodes(base: MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction>, count: Int): MutableTransactionedHandleMap<SecureObject<ProcessNodeInstance>, StubProcessTransaction> {
      return CachingHandleMap(base, count, PNI_SET_HANDLE)
    }

    private fun <V> cacheModels(base: IMutableProcessModelMap<StubProcessTransaction>, count: Int): IMutableProcessModelMap<StubProcessTransaction> {
      return CachingProcessModelMap(base, count)
    }

    private val documentBuilder: DocumentBuilder by lazy {
      DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isIgnoringElementContentWhitespace = false
        isCoalescing = false

      }.newDocumentBuilder()
    }

    private fun assertXMLSimilar(expected: Document, actual: Document) {
      val diff = XMLUnit.compareXML(expected, actual)
      val detailedDiff = DetailedDiff(diff)
      if (!detailedDiff.similar()) {
        fail(detailedDiff.toString())
      }
    }

    @Throws(TransformerException::class)
    private fun toDocument(node: Node): Document {
      val result = documentBuilder.newDocument()
      Sources.writeToResult(DOMSource(node), DOMResult(result))
      return result
    }
  }


}
