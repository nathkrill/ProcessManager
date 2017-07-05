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

package nl.adaptivity.ws.rest

import net.devrieze.util.Annotations
import net.devrieze.util.JAXBCollectionWrapper
import net.devrieze.util.ReaderInputStream
import net.devrieze.util.Types
import nl.adaptivity.messaging.HttpResponseException
import nl.adaptivity.messaging.MessagingException
import nl.adaptivity.rest.annotations.RestMethod
import nl.adaptivity.rest.annotations.RestParam
import nl.adaptivity.rest.annotations.RestParam.ParamType
import nl.adaptivity.util.HttpMessage
import nl.adaptivity.util.KotlinEnumHelper
import nl.adaptivity.util.activation.Sources
import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.util.xml.DomUtil
import nl.adaptivity.util.xml.getXmlReader
import nl.adaptivity.xml.*
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.*
import java.nio.charset.Charset
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.servlet.http.HttpServletResponse
import javax.xml.XMLConstants
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlSeeAlso
import javax.xml.bind.util.JAXBSource
import javax.xml.namespace.QName
import javax.xml.stream.FactoryConfigurationError
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.dom.DOMSource
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory


abstract class RestMethodWrapper protected constructor(owner: Any, method: Method) : nl.adaptivity.ws.WsMethodWrapper(
    owner, method) {


  private class Java6RestMethodWrapper(pOwner: Any, pMethod: Method) : RestMethodWrapper(pOwner, pMethod) {

    override val parameterTypes: Array<Class<*>>
      get() = method.parameterTypes

    override val parameterAnnotations: Array<Array<Annotation>>
      get() = method.parameterAnnotations

    override val elementWrapper: XmlElementWrapper?
      get() = method.getAnnotation(XmlElementWrapper::class.java)


    override val returnType: Class<*>
      get() = method.returnType

    override val genericReturnType: Type
      get() = method.genericReturnType

    override val restMethod: RestMethod?
      get() = method.getAnnotation(RestMethod::class.java)

    override val declaringClass: Class<*>
      get() = method.declaringClass

    override fun exec() {
      if (params == null) {
        throw IllegalArgumentException("Argument unmarshalling has not taken place yet")
      }
      try {
        result = method(mOwner, *params)
      } catch (e: IllegalArgumentException) {
        throw MessagingException(e)
      } catch (e: IllegalAccessException) {
        throw MessagingException(e)
      } catch (e: InvocationTargetException) {
        val cause = e.cause
        throw MessagingException(cause ?: e)
      }

    }

  }

  private class Java8RestMethodWrapper(pOwner: Any, pMethod: Method) : RestMethodWrapper(pOwner, pMethod) {

    private val mMethodHandle: MethodHandle
    override val parameterAnnotations: Array<Array<Annotation>>
    override val elementWrapper: XmlElementWrapper?
    override val genericReturnType: Type
    override val returnType: Class<*>
    override val restMethod: RestMethod?
    override val declaringClass: Class<*>

    init {
      try {
        mMethodHandle = MethodHandles.lookup().unreflect(pMethod).bindTo(pOwner)
        parameterAnnotations = pMethod.parameterAnnotations
        elementWrapper = pMethod.getAnnotation(XmlElementWrapper::class.java)
        genericReturnType = pMethod.genericReturnType
        returnType = pMethod.returnType
        restMethod = pMethod.getAnnotation(RestMethod::class.java)
        declaringClass = pMethod.declaringClass
      } catch (e: IllegalAccessException) {
        throw RuntimeException(e)
      }

    }

    override val parameterTypes: Array<Class<*>>
      get() = mMethodHandle.type().parameterArray()

    override fun exec() {
      if (params == null) {
        throw IllegalArgumentException("Argument unmarshalling has not taken place yet")
      }
      try {
        result = mMethodHandle.invokeWithArguments(*params)
      } catch (e: InvocationTargetException) {
        val cause = e.cause
        throw MessagingException(cause ?: e)
      } catch (e: Throwable) {
        throw MessagingException(e)
      }

    }

  }

  private var pathParams: Map<String, String> = emptyMap()

  private var contentTypeSet = false

  private object HasMethodHandleHelper {
    val HASHANDLES: Boolean

    init {
      var hashandles: Boolean
      try {
        hashandles = MethodHandle::class.java.name != null
      } catch (e: RuntimeException) {
        hashandles = false
      }

      HASHANDLES = hashandles
    }
  }

  fun setPathParams(pPathParams: Map<String, String>) {
    pathParams = pPathParams
  }

  @Throws(XmlException::class)
  fun unmarshalParams(httpMessage: HttpMessage) {
    if (params != null) {
      throw IllegalStateException("Parameters have already been unmarshalled")
    }
    val parameterTypes = parameterTypes
    val parameterAnnotations = parameterAnnotations
    val argCnt = 0
    params = arrayOfNulls<Any>(parameterTypes.size)

    for (i in parameterTypes.indices) {
      val annotation = Annotations.getAnnotation(parameterAnnotations[i], RestParam::class.java)
      val name: String
      val type: ParamType
      val xpath: String?
      if (annotation == null) {
        name = "arg" + Integer.toString(argCnt)
        type = ParamType.QUERY
        xpath = null
      } else {
        name = annotation.name
        type = annotation.type
        xpath = annotation.xpath
      }

      when (type) {
        RestParam.ParamType.ATTACHMENT -> {
          if (httpMessage.attachments.isEmpty()) {
            // No attachments, are we the only one, then take the body
            val attachmentCount = parameterAnnotations.asSequence()
              .mapNotNull { Annotations.getAnnotation(it, RestParam::class.java) }
              .count { it.type == ParamType.ATTACHMENT }

            if (attachmentCount == 1) {
              if (httpMessage.body != null) {
                params[i] = coerceBody(parameterTypes[i], name, httpMessage.body)
              } else if (httpMessage.byteContent.size == 1) {
                params[i] = coerceSource(parameterTypes[i], httpMessage.byteContent[0])
              }
            }
          } else {
            params[i] = getParam(parameterTypes[i], name, type, xpath, httpMessage)
          }
        }
        else                           -> params[i] = getParam(parameterTypes[i], name, type, xpath, httpMessage)
      }


    }
  }

  @Throws(XmlException::class)
  private fun getParam(parameterJavaClass: Class<*>,
                       paramName: String,
                       restParamType: ParamType,
                       xpath: String?,
                       httpMessage: HttpMessage): Any? {
    var result: Any? = null
    when (restParamType) {
      RestParam.ParamType.GET        -> result = getParamGet(paramName, httpMessage)
      RestParam.ParamType.POST       -> result = getParamPost(paramName, httpMessage)
      RestParam.ParamType.QUERY      -> {
        result = getParamGet(paramName, httpMessage)
        if (result == null) {
          result = getParamPost(paramName, httpMessage)
        }
      }
      RestParam.ParamType.VAR        -> result = pathParams[paramName]
      RestParam.ParamType.XPATH      -> result = getParamXPath(parameterJavaClass, xpath ?:".", httpMessage.body)
      RestParam.ParamType.BODY       -> result = getBody(parameterJavaClass, httpMessage)
      RestParam.ParamType.ATTACHMENT -> result = getAttachment(parameterJavaClass, paramName, httpMessage)
      RestParam.ParamType.PRINCIPAL  -> {
        val principal = httpMessage.userPrincipal
        if (parameterJavaClass.isAssignableFrom(String::class.java)) {
          result = principal.name
        } else {
          result = principal
        }
      }
    }
    // XXX generizice this and share the same approach to unmarshalling in ALL code
    // TODO support collection/list parameters
    if (result != null && !parameterJavaClass.isInstance(result)) {
      if ((Types.isPrimitive(parameterJavaClass) || Types.isPrimitiveWrapper(parameterJavaClass)) && result is String) {
        try {
          result = Types.parsePrimitive(parameterJavaClass, result as String?)
        } catch (e: NumberFormatException) {
          throw HttpResponseException(HttpServletResponse.SC_BAD_REQUEST, "The argument given is invalid", e)
        }

      } else if (Enum::class.java.isAssignableFrom(parameterJavaClass)) {
        @Suppress("UNCHECKED_CAST")
        val clazz = parameterJavaClass as Class<Enum<*>>
        result = clazz.valueOf(result.toString())
      } else if (result is Node) {
        val factory = parameterJavaClass.getAnnotation(XmlDeserializer::class.java)
        if (factory != null) {
          try {
            result = factory.value.java.newInstance().deserialize(XmlStreaming.newReader(DOMSource(result as Node?)))
          } catch (e: IllegalAccessException) {
            throw XmlException(e)
          } catch (e: InstantiationException) {
            throw XmlException(e)
          }

        } else {
          result = JAXB.unmarshal(DOMSource(result as Node?), parameterJavaClass)
        }
      } else {
        val s = result.toString()
        // Only wrap when we don't start with <
        val requestBody = (if (s.startsWith("<")) s else "<wrapper>$s</wrapper>").toCharArray()
        if (requestBody.size > 0) {
          result = JAXB.unmarshal(CharArrayReader(requestBody), parameterJavaClass)
        } else {
          result = null
        }
      }
    }

    return result
  }

  @Throws(TransformerException::class, IOException::class, FactoryConfigurationError::class)
  private fun serializeValue(pResponse: HttpServletResponse, value: Any?) {
    if (value == null) {
      throw FileNotFoundException()
    }
    if (value is Source) {
      setContentType(pResponse, "application/binary")// Unknown content type
      Sources.writeToStream(value as Source?, pResponse.outputStream)
    } else if (value is Node) {
      pResponse.contentType = "text/xml"
      Sources.writeToStream(DOMSource(value as Node?), pResponse.outputStream)
    } else if (value is XmlSerializable) {
      pResponse.contentType = "text/xml"
      val factory = XMLOutputFactory.newInstance()
      factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, java.lang.Boolean.TRUE)
      try {
        val out = XmlStreaming.newWriter(pResponse.outputStream, pResponse.characterEncoding, true)
        try {
          out.startDocument(null, null, null)
          value.serialize(out)
          out.endDocument()
        } finally {
          out.close()
        }
      } catch (e: XmlException) {
        throw TransformerException(e)
      }

    } else if (value is Collection<*>) {
      val annotation = elementWrapper
      if (annotation != null) {
        setContentType(pResponse, "text/xml")
        pResponse.outputStream.use { outStream ->

          writeCollection(outStream, genericReturnType, value, getQName(annotation))
        }
      }
    } else if (value is CharSequence) {
      setContentType(pResponse, "text/plain")
      pResponse.writer.append(value as CharSequence?)
    } else {
      if (value != null) {
        try {
          val jaxbContext = JAXBContext.newInstance(returnType)
          setContentType(pResponse, "text/xml")

          val jaxbSource = JAXBSource(jaxbContext, value)
          Sources.writeToStream(jaxbSource, pResponse.outputStream)

        } catch (e: JAXBException) {
          throw MessagingException(e)
        }

      }
    }
  }

  protected abstract val elementWrapper: XmlElementWrapper?

  protected abstract val restMethod: RestMethod?

  protected abstract val parameterAnnotations: Array<Array<Annotation>>

  protected abstract val parameterTypes: Array<Class<*>>

  protected abstract val returnType: Class<*>

  protected abstract val genericReturnType: Type

  protected abstract val declaringClass: Class<*>


  @Deprecated("use {@link #marshalResult(HttpServletResponse)}, the pRequest parameter is ignored", ReplaceWith("marshalResult(response)"))
  @Throws(TransformerException::class, IOException::class, XmlException::class)
  fun marshalResult(request: HttpMessage, response: HttpServletResponse) {
    marshalResult(response)
  }

  @Throws(TransformerException::class, IOException::class, XmlException::class)
  fun marshalResult(pResponse: HttpServletResponse) {
    if (result is XmlSerializable) {
      // By default don't use JAXB
      setContentType(pResponse, "text/xml")
      OutputStreamWriter(pResponse.outputStream, pResponse.characterEncoding).use { writer ->
        XmlStreaming.newWriter(writer).use { xmlWriter ->
          (result as XmlSerializable).serialize(xmlWriter)
        }
      }
      return
    }
    val xmlRootElement = if (result == null) null else result.javaClass.getAnnotation(XmlRootElement::class.java)
    if (xmlRootElement != null) {
      try {
        val jaxbContext = JAXBContext.newInstance(returnType)
        val jaxbSource = JAXBSource(jaxbContext, result)
        setContentType(pResponse, "text/xml")
        Sources.writeToStream(jaxbSource, pResponse.outputStream)
      } catch (e: JAXBException) {
        throw MessagingException(e)
      }

    } else {
      serializeValue(pResponse, this.result)
    }
  }

  private fun setContentType(pResponse: HttpServletResponse, pDefault: String) {
    if (!contentTypeSet) {
      val methodAnnotation = restMethod
      if (methodAnnotation == null || methodAnnotation.contentType.length == 0) {
        pResponse.contentType = pDefault
      } else {
        pResponse.contentType = methodAnnotation.contentType
      }
      contentTypeSet = true
    }
  }

  private fun writeCollection(outputStream: OutputStream,
                              collectionType: Type,
                              collection: Collection<*>,
                              outerTagName: QName) {
    val rawType: Class<*>
    if (collectionType is ParameterizedType) {
      rawType = collectionType.rawType as Class<*>
    } else if (collectionType is Class<*>) {
      rawType = collectionType
    } else if (collectionType is WildcardType) {
      val UpperBounds = collectionType.upperBounds
      if (UpperBounds.size > 0) {
        rawType = UpperBounds[0] as Class<*>
      } else {
        rawType = Any::class.java
      }
    } else if (collectionType is TypeVariable<*>) {
      val UpperBounds = collectionType.bounds
      if (UpperBounds.size > 0) {
        rawType = UpperBounds[0] as Class<*>
      } else {
        rawType = Any::class.java
      }
    } else {
      throw IllegalArgumentException("Unsupported type variable")
    }
    var elementType: Class<*>?
    if (Collection::class.java.isAssignableFrom(rawType)) {
      val paramTypes = Types.getTypeParametersFor(Collection::class.java, collectionType)
      elementType = Types.toRawType(paramTypes[0])
      if (elementType!!.isInterface) {
        // interfaces not supported by jaxb
        elementType = Types.commonAncestor(collection)
      }
    } else {
      elementType = Types.commonAncestor(collection)
    }
    try {
      // As long as JAXB is an option, we have to know that this is a StAXWriter as JAXB needs to write to that.
      XmlStreaming.newWriter(outputStream, "UTF-8").use { xmlWriter ->

        var marshaller: Marshaller? = null
        xmlWriter.smartStartTag(outerTagName)
        for (item in collection) {
          when (item) {
            is XmlSerializable -> item.serialize(xmlWriter)
            is Node -> xmlWriter.serialize(item)
            null -> Unit
            else  -> {
              val m = marshaller ?: run {
                val jaxbcontext: JAXBContext = when (elementType) {
                  null -> newJAXBContext(JAXBCollectionWrapper::class.java)
                  else -> newJAXBContext(JAXBCollectionWrapper::class.java, elementType!!)
                }
                jaxbcontext.createMarshaller().also { marshaller = it }
              }
              m.marshal(item, delegateMethod.invoke(xmlWriter) as XMLStreamWriter)
            }
          }
        }
        xmlWriter.endTag(outerTagName)
      }
    } catch (e: Throwable) {
      throw MessagingException(e)
    }

  }

  @Throws(JAXBException::class)
  private fun newJAXBContext(vararg classes: Class<*>): JAXBContext {
    val seeAlso = declaringClass.getAnnotation(XmlSeeAlso::class.java)
    val classList: Array<out Class<*>?> = when {
      seeAlso != null && seeAlso.value.isNotEmpty() -> {
        val seeAlsoClasses: Array<Class<*>> = seeAlso.value.let { t -> Array<Class<*>>(t.size) { t[it].java } }
        seeAlsoClasses + classes
      }
      else                                          -> classes
    }
    return JAXBContext.newInstance(*classList)
  }

  companion object {

    @Volatile private var _getDelegate: MethodHandle? = null

    operator fun get(pOwner: Any, pMethod: Method): RestMethodWrapper {
      // Make it work with private methods and
      pMethod.isAccessible = true
      if (HasMethodHandleHelper.HASHANDLES && "1.7" != "java.specification.version") {
        return Java8RestMethodWrapper(pOwner, pMethod)
      } else {
        return Java6RestMethodWrapper(pOwner, pMethod)
      }
    }

    @Throws(XmlException::class)
    private fun getBody(pClass: Class<*>, pMessage: HttpMessage): Any? {
      val body = pMessage.body
      if (body != null) {
        return DomUtil.childrenToDocumentFragment(body.getXmlReader())
      } else {
        return getAttachment(pClass, null, pMessage)
      }
    }

    private fun getAttachment(pClass: Class<*>, pName: String?, pMessage: HttpMessage): Any? {
      val source = pMessage.getAttachment(pName)
      return coerceSource(pClass, source)
    }

    private fun coerceBody(pTargetType: Class<*>, name: String, pBody: CompactFragment): Any? {
      val dataSource = object : DataSource {
        @Throws(IOException::class)
        override fun getInputStream(): InputStream {
          return ReaderInputStream(Charset.forName("UTF-8"), CharArrayReader(pBody.content))
        }

        @Throws(IOException::class)
        override fun getOutputStream(): OutputStream {
          throw UnsupportedOperationException()
        }

        override fun getContentType(): String {
          return "application/xml"
        }

        override fun getName(): String {
          return name
        }
      }
      return coerceSource(pTargetType, dataSource)
    }

    private fun coerceSource(pClass: Class<*>, pSource: DataSource?): Any? {
      if (pSource != null) {
        if (DataHandler::class.java.isAssignableFrom(pClass)) {
          return DataHandler(pSource)
        }
        if (DataSource::class.java.isAssignableFrom(pClass)) {
          return pSource
        }
        if (InputStream::class.java.isAssignableFrom(pClass)) {
          try {
            return pSource.inputStream
          } catch (e: IOException) {
            throw MessagingException(e)
          }

        }
        try {
          // This will try to do magic to handle the data
          return DataHandler(pSource).content
        } catch (e: IOException) {
          throw MessagingException(e)
        }

      }
      return null
    }

    private fun getParamGet(pName: String, pMessage: HttpMessage): String? {
      return pMessage.getQuery(pName)
    }

    private fun getParamPost(pName: String, pMessage: HttpMessage): String? {
      return pMessage.getPosts(pName)
    }

    @Throws(XmlException::class)
    private fun <T> getParamXPath(paramType: Class<T>, xpath: String, body: CompactFragment): T? {
      // TODO Avoid JAXB where possible, use XMLDeserializer instead
      val string = CharSequence::class.java.isAssignableFrom(paramType)
      var match: Node?
      val fragment = DomUtil.childrenToDocumentFragment(body.getXmlReader())
      var n: Node? = fragment.firstChild
      while (n != null) {
        match = xpathMatch(n, xpath)
        if (match != null) {
          if (!string) {
            val deserializer = paramType.getAnnotation(XmlDeserializer::class.java)
            if (deserializer != null) {
              try {
                val factory = deserializer.value.java.newInstance()
                factory.deserialize(XmlStreaming.newReader(DOMSource(n)))
              } catch (e: InstantiationException) {
                throw RuntimeException(e)
              } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
              }

            } else {
              return JAXB.unmarshal(DOMSource(match), paramType)
            }
          } else {
            return paramType.cast(nodeToString(match))
          }
        }
        n = n.nextSibling
      }
      return null
    }

    private fun nodeToString(pNode: Node): String {
      return pNode.textContent
    }

    private fun xpathMatch(pN: Node, pXpath: String): Node? {
      val factory = XPathFactory.newInstance()
      val xpath = factory.newXPath()
      val result: NodeList?
      try {
        result = xpath.evaluate(pXpath, DOMSource(pN), XPathConstants.NODESET) as NodeList
      } catch (e: XPathExpressionException) {
        return null
      }

      if (result == null || result.length == 0) {
        return null
      }
      return result.item(0)
    }

    private val delegateMethod: MethodHandle
      get() {
        return this._getDelegate ?: kotlin.run {
           MethodHandles.lookup()
            .findVirtual(Class.forName("nl.adaptivity.xml.StAXWriter"),
                         "getDelegate", MethodType.methodType(XMLStreamWriter::class.java))
             .also { this._getDelegate = it }
        }
      }

    private fun getQName(pAnnotation: XmlElementWrapper): QName {
      var nameSpace = pAnnotation.namespace
      if ("##default" == nameSpace) {
        nameSpace = XMLConstants.NULL_NS_URI
      }
      val localName = pAnnotation.name
      return QName(nameSpace, localName, XMLConstants.DEFAULT_NS_PREFIX)
    }
  }

}

private inline fun Class<Enum<*>>.valueOf(name:String): Enum<*> {
  return KotlinEnumHelper.enumValueOf(this, name)
}