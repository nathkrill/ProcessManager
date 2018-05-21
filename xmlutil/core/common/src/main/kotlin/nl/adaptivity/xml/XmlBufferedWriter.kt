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

package nl.adaptivity.xml

import nl.adaptivity.util.xml.CombiningNamespaceContext

class XmlBufferedWriter(buffer: MutableList<XmlEvent> = mutableListOf(), delegateNamespaceContext: NamespaceContext?=null) : XmlWriter {
    private val _buffer = buffer

    val buffer: List<XmlEvent> get() = _buffer

    private val namespaceHolder = NamespaceHolder()

    override val depth: Int get() = namespaceHolder.depth

    override var indent: Int
        get() = 0
        set(value) {} // Buffered writers don't add synthetic elements

    override val namespaceContext: NamespaceContext = if (delegateNamespaceContext==null) {
        namespaceHolder.namespaceContext
    } else {
        CombiningNamespaceContext(namespaceHolder.namespaceContext, delegateNamespaceContext)
    }

    override fun setPrefix(prefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(prefix, namespaceUri)
    }

    override fun getNamespaceUri(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    override fun getPrefix(namespaceUri: String?): String? {
        return namespaceUri?.let { namespaceHolder.getPrefix(it) }
    }

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        namespaceHolder.incDepth()
        val effNamespace = effectiveNamespace(namespace, prefix)
        val effPrefix = effectivePrefix(prefix, effNamespace)

        _buffer.add(
            XmlEvent.StartElementEvent(null, effNamespace ?: "", localName, effPrefix ?: "", emptyArray(), emptyArray()))
    }

    private fun effectivePrefix(prefix: String?, namespace: String?) =
        prefix ?: namespace?.let { namespaceContext.getPrefix(it) }

    private fun effectiveNamespace(namespace: String?, prefix: String?) =
        if (namespace.isNullOrEmpty()) prefix?.let { namespaceContext.getNamespaceURI(prefix) } else namespace

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)
        val localName: String
        val prefix: String
        if (namespacePrefix.isEmpty()) {
            localName = XMLConstants.XMLNS_ATTRIBUTE
            prefix = ""
        } else {
            localName = namespacePrefix
            prefix = XMLConstants.XMLNS_ATTRIBUTE
        }

        _buffer.add(XmlEvent.Attribute(null, XMLConstants.XMLNS_ATTRIBUTE_NS_URI, localName, prefix, namespaceUri))
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        val effNamespace = effectiveNamespace(namespace, prefix)
        val effPrefix = effectivePrefix(prefix, effNamespace) ?: ""
        _buffer.add(XmlEvent.EndElementEvent(null, effNamespace ?: "", localName, effPrefix))
        namespaceHolder.decDepth()
    }

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        _buffer.add(XmlEvent.StartDocumentEvent(null, encoding, version, standalone))
    }

    override fun processingInstruction(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.PROCESSING_INSTRUCTION, text))
    }

    override fun docdecl(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.DOCDECL, text))
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (namespace == XMLConstants.XMLNS_ATTRIBUTE_NS_URI || prefix == XMLConstants.XMLNS_ATTRIBUTE ||
            (prefix.isNullOrEmpty() && name == XMLConstants.XMLNS_ATTRIBUTE)) {
            namespaceAttr(name, value)
        } else {
            val effNamespace = effectiveNamespace(namespace, prefix)
            val effPrefix = effectivePrefix(prefix, effNamespace) ?: ""
            _buffer.add(XmlEvent.Attribute(null, effNamespace ?: "", name, effPrefix, value))
        }
    }

    override fun comment(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.COMMENT, text))
    }

    override fun text(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.TEXT, text))
    }

    override fun cdsect(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.CDSECT, text))
    }

    override fun entityRef(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.ENTITY_REF, text))
    }

    override fun ignorableWhitespace(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.IGNORABLE_WHITESPACE, text))
    }

    override fun endDocument() {
        _buffer.add(XmlEvent.EndDocumentEvent(null))
    }

    override fun close() {}

    override fun flush() {}

    /**
     * Write the events to the target, removing them from the writer
     */
    fun flushTo(target: XmlWriter) {
        writeTo(target)
        _buffer.clear()
    }

    /**
     * Write the events to the target
     */
    fun writeTo(target: XmlWriter) {
        for (event in _buffer) {
            event.writeTo(target)
        }
    }
}