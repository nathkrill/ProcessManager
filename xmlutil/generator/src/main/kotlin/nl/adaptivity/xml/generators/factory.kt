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

package nl.adaptivity.xml.generators

import net.devrieze.util.ReflectionUtil
import net.devrieze.util.StringUtil
import nl.adaptivity.xml.*
import nl.adaptivity.xml.schema.annotations.AnyType
import nl.adaptivity.xml.schema.annotations.Element
import java.io.CharArrayWriter
import java.io.File
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.*
import java.util.*
import javax.xml.namespace.NamespaceContext
import javax.xml.namespace.QName

/*
 * Simple information creating package that just lists the possible, and the available classes.
 * Created by pdvrieze on 15/04/16.
 */

class Factory {
  companion object {

    /**
     * This function implements task level generation. Support that as well in this file.
     */
    @JvmStatic
    fun doGenerate(outputDir: File, input: Iterable<File>) {
      if (outputDir.isFile) throw ProcessingException("The output location is not a directory")
      visitClasses(input) { clazz ->
        if (XmlSerializable::class.java.isAssignableFrom(clazz)) {
          val elementAnnot = clazz.getAnnotation(Element::class.java)
          if (elementAnnot != null) {
            val typeInfo = FullTypeInfo(clazz, elementAnnot)
            generateFactory(outputDir, typeInfo)
          }
        }
      }
    }

    private fun generateFactory(outDir: File, typeInfo: FullTypeInfo) {
      val factoryClassName = typeInfo.factoryClassName
      val packageName = typeInfo.packageName
      val nsPrefix = typeInfo.nsPrefix

      val fileCreator = createJavaFile(packageName, factoryClassName) {
        emptyConstructor()
        writeSerialize(nsPrefix, typeInfo)
        writeDeserialize(nsPrefix, typeInfo)

      }

      val outputFile = getFactorySourceFile(outDir, packageName, factoryClassName)
      outputFile.writer().use {
        fileCreator.appendTo(it)
      }
    }

    private fun JavaFile.writeSerialize(nsPrefix: CharSequence?,
                                        typeInfo: FullTypeInfo) {
      method("serialize",
             null,
             arrayOf(XmlException::class.java),
             XmlWriter::class.java to "writer",
             typeInfo.elemType to "value") {
        val writer: XmlWriter = XmlStreaming.newWriter(StringWriter())

        appendln("    writer.startTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(
              nsPrefix)});")

        typeInfo.attributes.forEach { attr ->
          appendln()
          if (attr.accessorType.isMap) {
            val keyType = ReflectionUtil.typeParams(attr.accessorType.javaType,
                                                    Map::class.java)?.get(0) ?: CharSequence::class.java
            appendln("    for(${Map.Entry::class.java.withParams(keyType, attr.accessorType.elemType).ref} attr: value.${attr.getterJava}.entrySet()) {")
            val keyClass = keyType.toClass()
            if (QName::class.java.isAssignableFrom(keyClass)) {
              appendln("      QName key = attr.getKey(); writer.attribute(key.getNamespaceURI(), key.getLocalPart(), key.getPrefix(), ${attr.readJava(
                    "attr.getValue()")});")
            } else {
              val getKey = if (String::class.java == keyClass) "attr.getKey()" else "attr.getKey().toString()"
              appendln("      writer.attribute(null, ${getKey}, null, ${attr.readJava("attr.getValue()")});")
            }
            appendln("    }")
          } else {
            appendln("    {")
            appendln("      final ${attr.accessorType.javaType.ref} attrValue = value.${attr.getterJava};")
            if (attr.isOptional && !attr.accessorType.isPrimitive) {
              appendln("      if (attrValue!=null) writer.attribute(null, ${toLiteral(attr.name)}, null, ${attr.readJava(
                    "attrValue")});")
            } else {
              appendln("      writer.attribute(null, ${toLiteral(attr.name)}, null, attrValue==null ? ${toLiteral(attr.default)} : ${attr.readJava(
                    "attrValue")});")
            }
            appendln("    }")
          }
        }

        typeInfo.textContent?.let { content ->
          appendln()
          appendln("    {")
          appendln("      final ${content.accessorType.javaType.ref} contentValue = value.${content.getterJava};")
          appendln("      if (contentValue!=null) writer.text(${content.readJava("contentValue")});")
          appendln("    }")
        }

        typeInfo.children.forEach { childInfo ->
          val accessor = childInfo.accessorType
          appendln()
          appendln("    {")
          val attrname = if (accessor.isCollection) "childValues" else "childValue"
          appendln("      final ${accessor.javaType.ref} $attrname = value.${childInfo.getterJava};")
          var indent: String
          if (accessor.isCollection) {
            indent = " ".repeat(8)
            appendln("      for(final ${accessor.elemType.ref} childValue: childValues) {")
          } else indent = " ".repeat(6)

          writeSerializeChild(indent, typeInfo, childInfo, "childValue")

          if (accessor.isCollection) {
            appendln("      }")
          }
          appendln("    }")

        }

        appendln()
        appendln("    writer.endTag(${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)}, ${toLiteral(
              nsPrefix)});")
      }
    }

    private fun getFactorySourceFile(outDir: File, packageName: String, factoryClassName: String): File {
      val directory = packageName.replace('.', '/');
      val filename = factoryClassName + ".java"
      return File(outDir, "${directory}/$filename").apply { parentFile.mkdirs(); createNewFile() }
    }

    private fun JavaFile.writeDeserialize(nsPrefix: CharSequence?,
                                          typeInfo: FullTypeInfo) {

      method("deserialize",
             typeInfo.elemType,
             arrayOf(XmlException::class.java),
             XmlReader::class.java to "reader") {
        val wildCardAttribute = run {
          var _wildCardAttr:AttributeInfo? = null
          if (typeInfo.attributes.size > 0) {
            for (attr in typeInfo.attributes) {
              if (attr.accessorType.isMap) {
                if (_wildCardAttr!=null) { throw ProcessingException("Attempting to set multiple wildcard attributes") }
                imports.add(HashMap::class.java)
                _wildCardAttr = attr
              }
              appendln("    ${attr.accessorType.javaType.ref} ${attr.name} = ${if (attr.default.isNotBlank()) attr.javaFromString(
                    toLiteral(attr.default)) else attr.accessorType.defaultValueJava};")
            }
            appendln()
          }
          _wildCardAttr;
        }

        if (typeInfo.children.size>0) {
          for (child in typeInfo.children) {
            if (child.accessorType.isCollection) { imports.add(ArrayList::class.java) }
            appendln("    ${child.accessorType.javaType.ref} ${child.name} = ${child.accessorType.defaultValueJava};")
          }
          appendln()
        }

        appendln("    reader.require(${XmlStreaming.EventType::class.java.ref}.START_ELEMENT, ${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)});")

        if (typeInfo.attributes.size>0) {
          appendln()

          appendln("    for (int i = 0; i < reader.getAttributeCount(); i++) {")
          if ((typeInfo.attributes.size>1 || wildCardAttribute==null )) {
            appendln("      switch(reader.getAttributeLocalName(i).toString()) {")
            for(attr in typeInfo.attributes) {
              appendln("        case \"${attr.name}\": ${attr.javaFromString("reader.getAttributeValue(i)")};")
            }
            appendln("        default:")
            if (wildCardAttribute!=null) {
              appendln("          ${wildCardAttribute.name}.put(${wildcardAttrName(wildCardAttribute)},${wildcardAttrValue(wildCardAttribute)});")
            } else {
              appendln("          throw new XmlException(\"Unexpected attribute found (\"+reader.getAttributeLocalName(i)+\")\");")
            }
            appendln("      }")
          } else { // must have a wildcard only
            appendln("      ${wildCardAttribute.name}.put(${wildcardAttrName(wildCardAttribute)},${wildcardAttrValue(wildCardAttribute)});")
          }
          appendln("    }")
        }

        val eventType = XmlStreaming.EventType::class.java.ref
        appendln()
        imports.add(XmlStreaming.EventType::class.java)
        appendln("    EventType eventType;")
        appendln("    while ((eventType=reader.next())!=${eventType}.END_ELEMENT) {")
        appendln("      switch(eventType) {")
        appendln("        case CDSECT:")
        appendln("        case TEXT:")
        if (typeInfo.textContent!=null) {
          appendln("        break;")
        }
        appendln("      }")
        appendln("    }")

        appendln("    reader.require(${eventType}.END_ELEMENT, ${toLiteral(typeInfo.nsUri)}, ${toLiteral(typeInfo.elementName)});")

        appendln("")
        appendln("    throw new UnsupportedOperationException(\"creating the type is not yet supported\");")
      }
    }
  }
}

private class JavaFile(val packageName:String, val className:String) {

  val classBody = mutableListOf<Appendable.()->Unit>()
  val imports = mutableSetOf<Class<*>>()

  fun emptyConstructor() {
    classBody.add {
      appendln("  public $className() {}")
    }
  }

  val Type.ref:String get() = this.ref({null})

  fun Type.ref(lookup:(TypeVariable<*>)->String?):String {

    fun newType(clazz: Class<*>):String { // This does not work for duplicates in the local package
      val simpleName = clazz.simpleName
      if (clazz in imports) return simpleName
      if (imports.any { it.simpleName == simpleName }) { return clazz.canonicalName }
      imports.add(clazz)
      return simpleName
    }

    return resolveType(this, lookup, ::newType )
  }

  inline fun method(name:String, returnType:Type?, vararg parameters:Pair<Type,String>, crossinline body:Appendable.()->Unit) {
    return method(name, returnType, emptyArray(), *parameters) { body() }
  }


  fun method(name:String, returnType:Type?, throws: Array<out Class<out Throwable>>, vararg parameters:Pair<Type,String>, body:Appendable.()->Unit) {
    classBody.add {
      val typeVars : List<SimpleTypeVar> = getTypeVars(parameters.map { it.first })

      fun variableLookup(tv: TypeVariable<*>):String? {
        val l = typeVars.firstOrNull { it.name == tv.name }
        return (l?.bounds ?: tv.bounds).asSequence().firstOrNull()?.let { it.ref(::variableLookup) }
      }

      System.err.println("typeVars = $typeVars")
      if (returnType==null) {
        append("  public static final void ${name}(")
      } else {
        append("  public static final ${returnType.ref(::variableLookup)} ${name}(")

      }
      parameters.joinTo(this) { val (type, name) = it; "${type.ref} ${name}" }
      append(")")
      if (throws.size>0) {
        append(" throws ")
        throws.joinTo(this) { it.ref }
      }
      appendln(" {")
      body()
      appendln("  }")
    }
  }

  fun appendTo(writer: Writer): Unit {
    // First generate the body, so all imports are resolved.
    val body = CharArrayWriter().apply {
      classBody.forEach {
        appendln()
        it.invoke(this)
      }
    }.toCharArray()

    writer.run {
      appendln("/* Automatically generated by ${this@JavaFile.className} */")
      appendln()
      appendln("package ${packageName};")

      if (imports.size>0) {
        appendln()
        imports.asSequence().filter { c-> c.`package`.name!=packageName || c.enclosingClass!=null}
              .map { c -> "import ${c.canonicalName};" }.sorted().forEach { appendln(it) }
      }
      appendln()
      appendln("public class ${className} {")

      write(body)

      appendln()
      appendln("}")
    }
  }


  fun Appendable.writeSerializeChild(indent: String, owner: FullTypeInfo, childInfo: ChildInfo, valueRef: String) {
    append(indent).append("if (").append(valueRef).append("!=null) ")
    if (childInfo.accessorType.isXmlSerializable) {
      append(valueRef).appendln(".serialize(writer);")
    } else if (childInfo.accessorType.isSimpleType) {
      val childType = childInfo.accessorType
      appendln(" {")
      append(indent).appendln("  writer.startTag(${toLiteral(owner.nsUri)}, ${toLiteral(childInfo.name)}, ${toLiteral(
            owner.nsPrefix)});")
      append(indent).appendln("  writer.text(${childInfo.readJava(valueRef)});")
      append(indent).appendln("  writer.endTag(${toLiteral(owner.nsUri)}, ${toLiteral(childInfo.name)}, ${toLiteral(
            owner.nsPrefix)});")
      append(indent).appendln("}")
    } else /*if (childInfo.elemType==AnyType::class.java)*/ {

      appendln("${FactoryHelper.XMLWriterUtil.ref}.serialize(writer, ${valueRef});")
//      } else {
//        throw ProcessingException("Don't know how to serialize child ${childInfo.name} type ${childInfo.elemType.typeName}")
    }
  }



  fun wildcardAttrName(attr:AttributeInfo):String {
    val attrType = attr.accessorType.javaType
    if (attrType is ParameterizedType) {
      val actualName = attrType.actualTypeArguments[0].toClass()
      if (actualName.isAssignableFrom(CharSequence::class.java)) {
        return "reader.getAttributeLocalName(i)"
      } else if (actualName.isAssignableFrom(String::class.java)) {
        return "reader.getAttributeLocalName(i).toString()"
      } else if (actualName.isAssignableFrom(QName::class.java)) {
        return "reader.getName()"
      }
    }
    return "reader.getAttributeLocalName(i).toString()" // assume string
  }

  fun wildcardAttrValue(attr:AttributeInfo):String {
    val attrType = attr.accessorType.javaType
    if (attrType is ParameterizedType) {
      val actualValue = attrType.actualTypeArguments[1].toClass()
      if (actualValue.isAssignableFrom(CharSequence::class.java)) {
        return "reader.getAttributeValue(i)"
      } else if (actualValue.isAssignableFrom(String::class.java)) {
        val stringUtil = StringUtil::class.java.ref
        return "$stringUtil.toString(reader.getAttributeLocalName(i))"
      } else if (actualValue.isAssignableFrom(QName::class.java)) {
        val stringUtil = StringUtil::class.java.ref
        return "${FactoryHelper.XMLUtil.ref}.asQName($stringUtil.toString(reader.getNamespaceContext(),reader.getAttributeValue(i)))"
      }
    }
    return "reader.getAttributeLocalName(i).toString()" // assume string
  }
}

private inline fun createJavaFile(packageName: String, className: String, block: JavaFile.()->Unit): JavaFile {
  return JavaFile(packageName, className).apply(block)
}
