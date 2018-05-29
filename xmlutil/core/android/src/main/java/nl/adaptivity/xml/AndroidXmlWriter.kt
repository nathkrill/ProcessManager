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

import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.io.Writer
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

actual typealias PlatformXmlWriter = AndroidXmlWriter

/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 */
class AndroidXmlWriter : XmlWriter {

    private val namespaceHolder = NamespaceHolder()
    private val isRepairNamespaces: Boolean
    private val writer: XmlSerializer

    override var indent: Int = 0
    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    override val namespaceContext: NamespaceContext
        get() = namespaceHolder.namespaceContext

    override val depth: Int
        get() = namespaceHolder.depth

    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads constructor(writer: Writer, repairNamespaces: Boolean = true, omitXmlDecl: Boolean = false) : this(repairNamespaces, omitXmlDecl) {
        this.writer.setOutput(writer)
        initWriter(this.writer)
    }

    @Throws(XmlPullParserException::class)
    private constructor(repairNamespaces: Boolean, omitXmlDecl: Boolean) {
        isRepairNamespaces = repairNamespaces
        writer = BetterXmlSerializer().apply { isOmitXmlDecl = omitXmlDecl }
        initWriter(writer)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads constructor(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = true, omitXmlDecl: Boolean = false) :
        this(repairNamespaces, omitXmlDecl) {
        writer.setOutput(outputStream, encoding)
        initWriter(writer)
    }

    private fun initWriter(writer: XmlSerializer) {
        try {
            writer.setPrefix(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @JvmOverloads
    constructor(serializer: XmlSerializer, repairNamespaces: Boolean = true) {
        writer = serializer
        isRepairNamespaces = repairNamespaces
        initWriter(writer)
    }

    private fun writeIndent(newDepth:Int = depth) {
        if (lastTagDepth>=0 && indent > 0 && lastTagDepth!=depth) {
            writer.ignorableWhitespace("\n${" ".repeat(indent * depth)}")
        }
        lastTagDepth = newDepth
    }


    @Throws(XmlException::class)
    override fun flush() {
        try {
            writer.flush()
        } catch (e: IOException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        writeIndent()

        try {
            if (namespace != null && namespace.isNotEmpty()) {
                writer.setPrefix(prefix ?: "", namespace)
            }
            writer.startTag(namespace, localName)
            namespaceHolder.incDepth()
            ensureNamespaceIfRepairing(namespace, prefix)
        } catch (e: IOException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    private fun ensureNamespaceIfRepairing(namespace: String?, prefix: String?) {
        if (isRepairNamespaces && namespace != null && namespace.isNotEmpty() && prefix != null) {
            // TODO fix more cases than missing namespaces with given prefix and uri
            if (namespaceHolder.getNamespaceUri(prefix) != (namespace ?:"")) {
                namespaceAttr(prefix, namespace)
            }
        }
    }

    @Throws(XmlException::class)
    override fun comment(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.comment(text)
    }

    @Throws(XmlException::class)
    override fun text(text: String) {
        writer.text(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun cdsect(text: String) {
        writer.cdsect(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun entityRef(text: String) {
        writer.entityRef(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun processingInstruction(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.processingInstruction(text)
    }

    @Throws(XmlException::class)
    override fun ignorableWhitespace(text: String) {
        writer.ignorableWhitespace(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (prefix != null && prefix.isNotEmpty() && namespace != null && namespace.isNotEmpty()) {
            setPrefix(prefix, namespace)
            ensureNamespaceIfRepairing(namespace, prefix)
        }
        val writer = writer
        if (writer is BetterXmlSerializer) {
            writer.attribute(namespace, prefix?:"", name, value)
        } else {
            writer.attribute(namespace, name, value)
        }
    }

    @Throws(XmlException::class)
    override fun docdecl(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.docdecl(text)
    }

    /**
     * {@inheritDoc}
     * @param version Unfortunately the serializer is forced to version 1.0
     */
    @Throws(XmlException::class)
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.startDocument(encoding, standalone)
    }

    @Throws(XmlException::class)
    override fun endDocument() {
        assert(depth == 0)
        writer.endDocument()
    }

    @Throws(XmlException::class)
    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        namespaceHolder.decDepth()
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.endTag(namespace, localName)
    }

    @Throws(XmlException::class)
    override fun setPrefix(prefix: String, namespaceUri: String) {
        if (namespaceUri != getNamespaceUri(prefix)) {
            namespaceHolder.addPrefixToContext(prefix, namespaceUri)
            writer.setPrefix(prefix, namespaceUri)
        }
    }

    @Throws(XmlException::class)
    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)
        if (namespacePrefix.isNotEmpty()) {
            writer.attribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, namespacePrefix,
                             namespaceUri)
        } else {
            writer.attribute(XMLConstants.NULL_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, namespaceUri)
        }
    }

    override fun getNamespaceUri(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    override fun getPrefix(namespaceUri: String?): String? {
        return namespaceUri?.let { namespaceHolder.getPrefix(it) }
    }

    @Throws(XmlException::class)
    override fun close() {
        namespaceHolder.clear()
    }



    companion object {
        const val TAG_DEPTH_NOT_TAG = -1
        const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE
    }
}

