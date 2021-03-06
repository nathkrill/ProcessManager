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

import nl.adaptivity.util.multiplatform.URI
import nl.adaptivity.xmlutil.serialization.XmlSerialName


/**
 * Fault reporting structure
 *
 *
 * Java class for Fault complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * ```
 * <complexType name="Fault">
 * <complexContent>
 * <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * <sequence>
 * <element name="Code" type="{http://www.w3.org/2003/05/soap-envelope}faultcode"/>
 * <element name="Reason" type="{http://www.w3.org/2003/05/soap-envelope}faultreason"/>
 * <element name="Node" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 * <element name="Role" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 * <element name="Detail" type="{http://www.w3.org/2003/05/soap-envelope}detail" minOccurs="0"/>
 * </sequence>
 * </restriction>
 * </complexContent>
 * </complexType>
 * ```
 */
class Fault {

    @XmlSerialName("Code", Envelope.NAMESPACE, Envelope.PREFIX)
    var code: Faultcode? = null

    @XmlSerialName("Reason", Envelope.NAMESPACE, Envelope.PREFIX)
    var reason: Faultreason? = null

    @XmlSerialName("Node", Envelope.NAMESPACE, Envelope.PREFIX)
    var node: URI? = null

    @XmlSerialName("Role", Envelope.NAMESPACE, Envelope.PREFIX)
    var role: URI ? =null

    @XmlSerialName("Detail", Envelope.NAMESPACE, Envelope.PREFIX)
    var detail: Detail? = null

}
