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

@file:JvmName("XmlReaderUtil")

package nl.adaptivity.xml

import net.devrieze.util.kotlin.matches
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.xml.XmlEvent.NamespaceImpl
import java.io.Closeable
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName

/**
 * Created by pdvrieze on 15/11/15.
 */
interface XmlReader : Closeable, AutoCloseable {

  /** Get the next tag. This must call next, not use the underlying stream.  */
  @Throws(XmlException::class)
  fun nextTag(): EventType {
    var event = next()
    while (event !== EventType.START_ELEMENT && event !== EventType.END_ELEMENT && event != null)
    {
      if (event === EventType.TEXT)
      {
        if (!isXmlWhitespace(text))
        {
          throw XmlException("Unexpected text content")
        }
      }
      event = next()
    }
    return event
  }


  @Throws(XmlException::class)
  operator fun hasNext(): Boolean

  @Throws(XmlException::class)
  operator fun next(): EventType

  val namespaceUri: CharSequence
    @Throws(XmlException::class) get

  val localName: CharSequence
    @Throws(XmlException::class) get

  val prefix: CharSequence
    @Throws(XmlException::class) get

  open val name: QName
    @Throws(XmlException::class)
    get

  open val isStarted:Boolean

  @Throws(XmlException::class)
  fun require(type: EventType, namespace: CharSequence?, name: CharSequence?)

  val depth: Int

  val text: CharSequence

  val attributeCount: Int
    @Throws(XmlException::class)
    get

  @Throws(XmlException::class)
  fun getAttributeNamespace(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributePrefix(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributeLocalName(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getAttributeName(i: Int): QName

  @Throws(XmlException::class)
  fun getAttributeValue(i: Int): CharSequence

  val eventType: EventType
    @Throws(XmlException::class) get

  @Throws(XmlException::class)
  fun getAttributeValue(nsUri: CharSequence?, localName: CharSequence): CharSequence?

  val namespaceStart: Int
    @Throws(XmlException::class) get

  val namespaceEnd: Int
    @Throws(XmlException::class) get

  @Throws(XmlException::class)
  fun getNamespacePrefix(i: Int): CharSequence

  @Throws(XmlException::class)
  override fun close()

  @Throws(XmlException::class)
  fun getNamespaceUri(i: Int): CharSequence

  @Throws(XmlException::class)
  fun getNamespacePrefix(namespaceUri: CharSequence): CharSequence?

  @Throws(XmlException::class)
  fun isWhitespace(): Boolean

  @Throws(XmlException::class)
  fun isEndElement(): Boolean

  /** Is the currrent element character content */
  @Throws(XmlException::class)
  fun isCharacters(): Boolean

  /** Is the current element a start element */
  @Throws(XmlException::class)
  fun isStartElement(): Boolean

  @Throws(XmlException::class)
  fun getNamespaceUri(prefix: CharSequence): String?

  /** Get some information on the current location in the file. This is implementation dependent.  */
  val locationInfo: String?

  /** The current namespace context */
  val namespaceContext: NamespaceContext
    @Throws(XmlException::class) get

  val encoding: CharSequence?

  val standalone: Boolean?

  val version: CharSequence?
}

val XmlReader.attributes: Array<out XmlEvent.Attribute> get() =
      Array<XmlEvent.Attribute>(attributeCount) { i ->
        XmlEvent.Attribute(locationInfo,
                           getAttributeNamespace(i),
                           getAttributeLocalName(i),
                           getAttributePrefix(i),
                           getAttributeValue(i))
      }

val XmlReader.namespaceIndices: IntRange get() = namespaceStart..(namespaceEnd-1)

val XmlReader.attributeIndices: IntRange get() = 0..(attributeCount-1)

val XmlReader.namespaceDecls: Array<out Namespace> get() =
      Array<Namespace>(namespaceEnd - namespaceStart) { i ->
        val nsIndex = namespaceStart + i
        NamespaceImpl(getNamespacePrefix(nsIndex), getNamespaceUri(nsIndex))
      }

val XmlReader.qname:QName get() = text.toQname()

fun XmlReader.isPrefixDeclaredInElement(prefix: String): Boolean {
  val r = this
  for (i in r.namespaceStart..r.namespaceEnd - 1) {
    if (r.getNamespacePrefix(i).matches(prefix)) {
      return true
    }
  }
  return false
}

@JvmOverloads
fun XmlReader.unhandledEvent(message:String? = null) {
  val actualMessage =   when (eventType) {
    EventType.CDSECT,
    EventType.TEXT          -> if (!isWhitespace()) message ?: "Content found where not expected [$locationInfo] Text:'$text'" else null
    EventType.START_ELEMENT -> message ?: "Element found where not expected [$locationInfo]: $name"
    EventType.END_DOCUMENT  -> message ?: "End of document found where not expected"
    else                                      -> null
  }// ignore

  actualMessage?.let { throw XmlException(it) }
}

@Throws(XmlException::class)
fun XmlReader.isElement(elementname: QName): Boolean {
  return isElement(EventType.START_ELEMENT, elementname.namespaceURI, elementname.localPart, elementname.prefix)
}

fun XmlReader.asSubstream(): XmlReader = SubstreamFilterReader(this)


/**
 * Get the next text sequence in the reader. This will skip over comments and ignorable whitespace, but not tags.
 * Any tags encountered with cause an exception to be thrown.
 *
 * @return   The text found
 *
 * @throws XmlException If reading breaks, or an unexpected element was found.
 */
@Throws(XmlException::class)
fun XmlReader.allText(): CharSequence {
  val t = this
  return buildString {
    var type: EventType? = null

    while ((t.next().apply { type = this@apply }) !== EventType.END_ELEMENT) {
      when (type) {
        EventType.COMMENT              -> {
        } // ignore
        EventType.IGNORABLE_WHITESPACE ->
          // ignore whitespace starting the element.
          if (length != 0) append(t.text)

        EventType.TEXT,
        EventType.CDSECT               -> append(t.text)
        else                                             -> throw XmlException("Found unexpected child tag")
      }//ignore

    }

  }
}

@Throws(XmlException::class)
fun XmlReader.skipElement() {
  val t = this
  t.require(EventType.START_ELEMENT, null, null)
  while (t.hasNext() && t.next() !== EventType.END_ELEMENT) {
    if (t.eventType === EventType.START_ELEMENT) {
      t.skipElement()
    }
  }
}

@Throws(XmlException::class)
fun XmlReader.readSimpleElement(): CharSequence {
  val t = this
  t.require(EventType.START_ELEMENT, null, null)
  return buildString {

    while ((t.next()) !== EventType.END_ELEMENT) {
      when (t.eventType) {
        EventType.COMMENT,
        EventType.PROCESSING_INSTRUCTION -> {
        }
        EventType.TEXT,
        EventType.CDSECT                 -> append(t.text)
        else                                               -> throw XmlException(
          "Expected text content or end tag, found: ${t.eventType}")
      }/* Ignore */
    }

  }
}


/**
 * Skil the preamble events in the stream reader
 * @param in The stream reader to skip
 */
@Throws(XmlException::class)
fun XmlReader.skipPreamble() {
  while (isIgnorable() && hasNext()) {
    next()
  }
}

@Throws(XmlException::class)
fun XmlReader.isIgnorable(): Boolean {
  when (eventType) {
    EventType.COMMENT,
    EventType.START_DOCUMENT,
    EventType.END_DOCUMENT,
    EventType.PROCESSING_INSTRUCTION,
    EventType.DOCDECL,
    EventType.IGNORABLE_WHITESPACE -> return true
    EventType.TEXT                 -> return isXmlWhitespace(text)
    else                                             -> return false
  }
}

/**
 * Differs from [.siblingsToFragment] in that it skips the current event.
 * @param reader
 * *
 * @return
 * *
 * @throws XmlException
 */
@Throws(XmlException::class)
fun XmlReader.elementContentToFragment(): CompactFragment
{
  val r = this
  r.skipPreamble()
  if (r.hasNext()) {
    r.require(EventType.START_ELEMENT, null, null)
    r.next()
    return r.siblingsToFragment()
  }
  return CompactFragment("")
}

@Throws(XmlException::class)
fun XmlReader.siblingsToCharArray() = siblingsToFragment().content

/**
 * Check that the current state is a start element for the given name. The mPrefix is ignored.
 * @param in The stream reader to check
 * *
 * @param type
 * @param elementname The name to check against  @return `true` if it matches, otherwise `false`
 */

@Throws(XmlException::class)
fun XmlReader.isElement(type: EventType, elementname: QName): Boolean {
  return this.isElement(type, elementname.namespaceURI, elementname.localPart, elementname.prefix)
}

/**
 * Check that the current state is a start element for the given name. The mPrefix is ignored.
 * @param in The stream reader to check
 * *
 * @param elementNamespace  The namespace to check against.
 * *
 * @param elementName The local name to check against
 * *
 * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined
 * *
 * @return `true` if it matches, otherwise `false`
 */
@Throws(XmlException::class)
@JvmOverloads
fun XmlReader.isElement(elementNamespace: CharSequence?, elementName: CharSequence, elementPrefix: CharSequence?=null): Boolean {
  return this.isElement(EventType.START_ELEMENT, elementNamespace, elementName, elementPrefix)
}

/**
 * Check that the current state is a start element for the given name. The mPrefix is ignored.
 * @param in The stream reader to check
 * *
 * @param type The type to verify. Should be named so start or end element
 * @param elementNamespace  The namespace to check against.
 * *
 * @param elementName The local name to check against
 * *
 * @param elementPrefix The mPrefix to fall back on if the namespace can't be determined    @return `true` if it matches, otherwise `false`
 */
@Throws(XmlException::class)
@JvmOverloads
fun XmlReader.isElement(type: EventType, elementNamespace: CharSequence?, elementName: CharSequence, elementPrefix: CharSequence?=null): Boolean {
  val r = this
  if (r.eventType !== type) {
    return false
  }
  val expNs: CharSequence? = elementNamespace?.let { if (it.isEmpty()) null else it }

  if (r.localName != elementName) {
    return false
  }

  if (elementNamespace.isNullOrEmpty()) {
    if (elementPrefix.isNullOrEmpty()) {
      return r.prefix.isNullOrEmpty()
    } else {
      return elementPrefix == r.prefix
    }
  } else {
    return expNs == r.namespaceUri
  }
}

/** Write the current event to the writer. This will **not** move the reader. */
@Throws(XmlException::class)
fun XmlReader.writeCurrent(writer:XmlWriter) = eventType.writeEvent(writer, this)

/**
 * A class that filters an xml stream such that it will only contain expected elements.
 */
private class SubstreamFilterReader(delegate: XmlReader) : XmlBufferedReader(delegate) {

  @Throws(XmlException::class)
  override fun doPeek(): List<XmlEvent> {
    return super.doPeek().filter {
      when (it.eventType) {
        EventType.START_DOCUMENT, EventType.PROCESSING_INSTRUCTION, EventType.DOCDECL, EventType.END_DOCUMENT -> false
        else                                                                                                                                                                          -> true
      }
    }
  }
}
