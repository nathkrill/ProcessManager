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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.08.06 at 08:14:28 PM BST
//


package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.Handles
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState
import nl.adaptivity.process.processModel.MutableProcessNode
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.SimpleXmlDeserializable
import nl.adaptivity.xml.*
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.namespace.QName

@XmlDeserializer(XmlProcessNodeInstance.Factory::class)
class XmlProcessNodeInstance : SimpleXmlDeserializable, XmlSerializable {

  class Factory : XmlDeserializerFactory<XmlProcessNodeInstance> {

    @Throws(XmlException::class)
    override fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance.deserialize(reader)
    }
  }

  constructor()

  constructor(nodeId: String,
              predecessors :Iterable<Handle<out IProcessNodeInstance<*>>>,
              processInstance: Long,
              handle: Handle<out IProcessNodeInstance<*>>,
              state: NodeInstanceState,
              results: Iterable<ProcessData>,
              body: CompactFragment?) {

  }

  private val _predecessors = mutableListOf<Handle<out IProcessNodeInstance<*>>>()

  /**
   * Gets the value of the predecessor property.
   */
  val predecessors: List<Handle<out IProcessNodeInstance<*>>>
    @XmlElement(name = "predecessor")
    get() = _predecessors

  /**
   * Gets the value of the body property.

   * @return The body
   */
  /**
   * Sets the value of the body property.

   * @param value The body
   */
  @XmlElement(required = true)
  var body: CompactFragment? = null

  @XmlAttribute(name = "handle", required = true)
  var handle = -1L

  var state: NodeInstanceState? = null

  var stateXml: String?
    @XmlAttribute(name = "state")
    get() = state?.name
    set(value) { state = value?.let { NodeInstanceState.valueOf(it) } }

  var processInstance: Long = -1

  @XmlAttribute(name = "nodeid")
  var nodeId: String? = null

  @get:XmlElement(name = "result")
  var results: MutableList<ProcessData> = mutableListOf()

  @Throws(XmlException::class)
  override fun deserializeChild(reader: XmlReader): Boolean {
    if (reader.isElement(Engine.NAMESPACE, "predecessor")) {
      _predecessors.add(Handles.handle<IProcessNodeInstance<*>>(reader.readSimpleElement().toString()))
      return true
    } else if (reader.isElement(Engine.NAMESPACE, "body")) {
      body = reader.elementContentToFragment()
      return true
    } else if (reader.isElement(ProcessData.ELEMENTNAME)) {
      results.add(ProcessData.deserialize(reader))
      return true
    }
    return false
  }

  override val elementName: QName
    get() = ELEMENTNAME

  var xmlProcessinstance: Long?
    @XmlAttribute(name = "processinstance")
    get() = if (processInstance == -1L) null else processInstance
    set(value) {
      this.processInstance = value ?: -1L
    }

  override fun deserializeChildText(elementText: CharSequence): Boolean {
    return false
  }

  override fun deserializeAttribute(attributeNamespace: CharSequence,
                                    attributeLocalName: CharSequence,
                                    attributeValue: CharSequence): Boolean {
    when (attributeLocalName.toString()) {
      "state"           -> {
        state = NodeInstanceState.valueOf(attributeValue.toString())
        return true
      }
      "processinstance" -> {
        processInstance = attributeValue.toString().toLong()
        return true
      }
      "handle"          -> {
        handle = attributeValue.toString().toLong()
        return true
      }
      "nodeid"          -> {
        nodeId = attributeValue.toString()
        return true
      }
    }
    return false
  }

  @Throws(XmlException::class)
  override fun onBeforeDeserializeChildren(reader: XmlReader) {

  }

  @Throws(XmlException::class)
  override fun serialize(out: XmlWriter) {
    out.smartStartTag(ELEMENTNAME) {
      writeAttribute("state", state?.name)

      if (processInstance != -1L) writeAttribute("processinstance", processInstance)
      if (handle != -1L) writeAttribute("handle", handle)
      if (nodeId != null) writeAttribute("nodeid", nodeId)

      _predecessors.forEach { writeSimpleElement(PREDECESSOR_ELEMENTNAME, java.lang.Long.toString(it.handleValue)) }

      results.forEach { it.serialize(this) }

      body?.let {
        out.smartStartTag(BODY_ELEMENTNAME)
        it.serialize(out.filterSubstream())
        out.endTag(BODY_ELEMENTNAME)
      }
    }
  }

  companion object {


    const val ELEMENTLOCALNAME = "nodeInstance"
    val ELEMENTNAME = QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX)
    const val PREDECESSOR_LOCALNAME = "predecessor"
    val PREDECESSOR_ELEMENTNAME = QName(Engine.NAMESPACE, PREDECESSOR_LOCALNAME, Engine.NSPREFIX)
    const val RESULT_LOCALNAME = "result"
    val RESULT_ELEMENTNAME = QName(Engine.NAMESPACE, RESULT_LOCALNAME, Engine.NSPREFIX)
    const private val BODY_LOCALNAME = "body"
    internal val BODY_ELEMENTNAME = QName(Engine.NAMESPACE, BODY_LOCALNAME, Engine.NSPREFIX)

    @Throws(XmlException::class)
    fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance().deserializeHelper(reader)
    }
  }


}
