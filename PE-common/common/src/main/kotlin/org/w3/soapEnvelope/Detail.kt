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
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.09.24 at 08:12:58 PM CEST
//


package org.w3.soapEnvelope

import nl.adaptivity.xml.QName
import nl.adaptivity.xml.schema.annotations.AnyType
import nl.adaptivity.xml.schema.annotations.Child


/**
 *
 *
 * Java class for detail complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="detail">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 * </sequence>
 * <anyAttribute processContents='lax' namespace='##other'/>
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 */
@nl.adaptivity.xml.schema.annotations.Element(name = "detail", nsUri = Envelope.NAMESPACE, nsPrefix = Envelope.PREFIX,
                                              children = arrayOf(Child(type = AnyType::class)))
class Detail {

    private var _any: MutableList<Any?>? = null

    var any: MutableList<Any?>
        get() = _any ?: mutableListOf<Any?>().also { _any = it }
        set(value) { _any = value as MutableList<Any?> }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property
     * on this class.
     *
     *
     * the map is keyed by the name of the attribute and the value is the string
     * value of the attribute. the map returned by this method is live, and you
     * can add new attribute by updating the map directly. Because of this design,
     * there's no setter.
     *
     * @return always non-null
     */
    val otherAttributes: Map<QName, String> = HashMap()

}