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

package nl.adaptivity.process.processModel

import nl.adaptivity.util.multiplatform.Throws
import nl.adaptivity.util.multiplatform.assert
import nl.adaptivity.util.xml.ICompactFragment
import nl.adaptivity.util.xml.ExtXmlDeserializable
import nl.adaptivity.util.xml.NamespaceAddingStreamReader
import nl.adaptivity.xml.*


/**
 * This class can contain xml content. It allows it to be transformed, and input/output
 * Created by pdvrieze on 30/10/15.
 */
abstract class XMLContainer : ExtXmlDeserializable, XmlSerializable, ICompactFragment {

    override var content: CharArray = CharArray(0)
        protected set

    override var namespaces: SimpleNamespaceContext = SimpleNamespaceContext()
        protected set

    override val isEmpty: Boolean get() = content.isEmpty()

    override val contentString: String
        get() = buildString(content.size) { content.forEach { append(it) } }

    val originalNSContext: Iterable<Namespace>
        get() = namespaces

    val bodyStreamReader: XmlReader
        get() = this.getXmlReader()

    constructor() {}

    constructor(originalNSContext: Iterable<Namespace>, content: CharArray) {
        setContent(originalNSContext, content)
    }

    constructor(fragment: ICompactFragment) {
        setContent(fragment)
    }

    override fun deserializeChildren(reader: XmlReader) {
        if (reader.hasNext()) {
            if (reader.next() !== EventType.END_ELEMENT) {
                val content = reader.siblingsToFragment()
                setContent(content)
            }
        }
    }

    override fun onBeforeDeserializeChildren(reader: XmlReader) {
        val nsEnd = reader.namespaceEnd
        for (i in reader.namespaceStart until nsEnd) {
            visitNamespace(reader, reader.getNamespacePrefix(i))
        }
    }

    fun setContent(originalNSContext: Iterable<Namespace>, content: CharArray) {
        this.namespaces = SimpleNamespaceContext.from(originalNSContext)
        this.content = content
    }

    fun setContent(content: ICompactFragment) {
        setContent(content.namespaces, content.content)
    }

    protected fun updateNamespaceContext(additionalContext: Iterable<Namespace>) {
        val nsmap = mutableMapOf<String, String>()
        val context = when(namespaces.size) {
            0 -> SimpleNamespaceContext.from(additionalContext)
            else               -> namespaces.combine(additionalContext)
        }
        val gatheringNamespaceContext = GatheringNamespaceContext(context, nsmap)
        visitNamespaces(gatheringNamespaceContext)

        namespaces = SimpleNamespaceContext(nsmap)
    }

    internal fun addNamespaceContext(namespaceContext: SimpleNamespaceContext) {
        namespaces = if (namespaces.size == 0) namespaceContext else namespaces.combine(namespaceContext)
    }

    override fun serialize(out: XmlWriter) {
        serializeStartElement(out)
        serializeAttributes(out)
        val outNs = out.namespaceContext
        for (ns in namespaces) {
            if (ns.namespaceURI != outNs.getNamespaceURI(ns.prefix)) {
                out.namespaceAttr(ns.prefix, ns.namespaceURI)
            }
        }
        serializeBody(out)
        serializeEndElement(out)
    }

    protected fun visitNamesInElement(source: XmlReader) {
        assert(source.eventType === EventType.START_ELEMENT)
        visitNamespace(source, source.prefix)

        for (i in source.attributeCount - 1 downTo 0) {
            val attrName = source.getAttributeName(i)
            visitNamesInAttributeValue(source.namespaceContext, source.name, attrName, source.getAttributeValue(i))
        }
    }

    protected open fun visitNamesInAttributeValue(referenceContext: NamespaceContext,
                                                  owner: QName,
                                                  attributeName: QName,
                                                  attributeValue: CharSequence) {
        // By default there are no special attributes
    }

    @Suppress("UnusedReturnValue")
    protected fun visitNamesInTextContent(parent: QName?, textContent: CharSequence): List<QName> {
        return emptyList()
    }

    protected open fun visitNamespaces(baseContext: NamespaceContext) {
        val xsr = NamespaceAddingStreamReader(baseContext,
                                              this.getXmlReader())

        visitNamespacesInContent(xsr, null)
    }

    @Throws(XmlException::class)
    private fun visitNamespacesInContent(xsr: XmlReader, parent: QName?) {
        while (xsr.hasNext()) {
            when (xsr.next()) {
                EventType.START_ELEMENT -> {
                    visitNamesInElement(xsr)
                    visitNamespacesInContent(xsr, xsr.name)
                }
                EventType.TEXT          -> {
                    visitNamesInTextContent(parent, xsr.text)
                }
            }//ignore
        }
    }

    @Throws(XmlException::class)
    private fun serializeBody(out: XmlWriter) {
        if (content.isNotEmpty()) {
            val contentReader = bodyStreamReader.asSubstream()
            while (contentReader.hasNext()) {
                contentReader.next()
                contentReader.writeCurrent(out)
            }
        }

    }

    @Throws(XmlException::class)
    protected open fun serializeAttributes(out: XmlWriter) {
        // No attributes by default
    }

    @Throws(XmlException::class)
    protected abstract fun serializeStartElement(out: XmlWriter)

    @Throws(XmlException::class)
    protected abstract fun serializeEndElement(out: XmlWriter)

    companion object {

        private val BASE_NS_CONTEXT = SimpleNamespaceContext(arrayOf(""), arrayOf(""))

        @Throws(XmlException::class)
        protected fun visitNamespace(`in`: XmlReader, prefix: CharSequence?) {
            if (prefix != null) {
                `in`.namespaceContext.getNamespaceURI(prefix.toString())
            }
        }
    }
}
