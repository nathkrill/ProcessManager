/*
 * Copyright (c) 2018.
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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.08.06 at 08:14:28 PM BST
//


package nl.adaptivity.process.engine.processModel

import net.devrieze.util.Handle
import net.devrieze.util.handle
import nl.adaptivity.process.ProcessConsts.Engine
import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.xmlutil.util.ICompactFragment
import nl.adaptivity.xmlutil.util.SimpleXmlDeserializable
import nl.adaptivity.xmlutil.*

@XmlDeserializer(XmlProcessNodeInstance.Factory::class)
class XmlProcessNodeInstance : SimpleXmlDeserializable, XmlSerializable {

  class Factory : XmlDeserializerFactory<XmlProcessNodeInstance> {

    override fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance.deserialize(reader)
    }
  }

  constructor()

  constructor(nodeId: String,
              predecessors :Iterable<Handle<XmlProcessNodeInstance>>,
              processInstance: Long,
              handle: Handle<XmlProcessNodeInstance>,
              state: NodeInstanceState,
              results: Iterable<ProcessData>,
              body: ICompactFragment?) {
    this.nodeId = nodeId
    this._predecessors.addAll(predecessors)
    this.processInstance = processInstance
    this.handle = handle.handleValue
    this.state = state
    this.results.addAll(results)
    this.body = body
  }

  private val _predecessors = mutableListOf<Handle<XmlProcessNodeInstance>>()

  /**
   * Gets the value of the predecessor property.
   */
  val predecessors: List<Handle<XmlProcessNodeInstance>>
    get() = _predecessors

  var body: ICompactFragment? = null

  var handle = -1L

  var entryNo = 0

  var state: NodeInstanceState? = null

  var stateXml: String?
    get() = state?.name
    set(value) { state = value?.let { NodeInstanceState.valueOf(it) } }

  var processInstance: Long = -1

  var nodeId: String? = null

  var results: MutableList<ProcessData> = mutableListOf()

  override fun deserializeChild(reader: XmlReader): Boolean {
    if (reader.isElement(Engine.NAMESPACE, "predecessor")) {
      _predecessors.add(handle(handle= reader.readSimpleElement()))
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
    get() = if (processInstance == -1L) null else processInstance
    set(value) {
      this.processInstance = value ?: -1L
    }

  override fun deserializeChildText(elementText: CharSequence): Boolean {
    return false
  }

  override fun deserializeAttribute(attributeNamespace: String?,
                                    attributeLocalName: String,
                                    attributeValue: String): Boolean {
    when (attributeLocalName) {
      "state"           -> state = NodeInstanceState.valueOf(attributeValue)
      "processinstance" -> processInstance = attributeValue.toLong()
      "handle"          -> handle = attributeValue.toLong()
      "nodeid"          -> nodeId = attributeValue
      "entryNo"         -> entryNo = attributeValue.toInt()
      else              -> return false
    }
    return true
  }

  override fun onBeforeDeserializeChildren(reader: XmlReader) {

  }

  override fun serialize(out: XmlWriter) {
    out.smartStartTag(ELEMENTNAME) {
      writeAttribute("state", state?.name)

      if (processInstance != -1L) writeAttribute("processinstance", processInstance)
      if (handle != -1L) writeAttribute("handle", handle)
      writeAttribute("nodeid", nodeId)
      writeAttribute("entryNo", entryNo)

      _predecessors.forEach { writeSimpleElement(PREDECESSOR_ELEMENTNAME, it.handleValue.toString()) }

      results.forEach { it.serialize(this) }

      body?.let {
        out.smartStartTag(BODY_ELEMENTNAME) {
          it.serialize(out.filterSubstream())
        }
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

    fun deserialize(reader: XmlReader): XmlProcessNodeInstance {
      return XmlProcessNodeInstance().deserializeHelper(reader)
    }
  }


}
