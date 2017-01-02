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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.09.24 at 08:12:58 PM CEST 
//


package org.w3.soapEnvelope;

import javax.xml.bind.annotation.*;


/**
 * <p>
 * Java class for reasontext complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="reasontext">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang use="required""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "reasontext", propOrder = { "value" })
public class Reasontext {

  @XmlValue
  protected String value;

  @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace", required = true)
  protected String lang;

  /**
   * Gets the value of the value property.
   * 
   * @return possible object is {@link String }
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value of the value property.
   * 
   * @param value allowed object is {@link String }
   */
  public void setValue(final String value) {
    this.value = value;
  }

  /**
   * Gets the value of the lang property.
   * 
   * @return possible object is {@link String }
   */
  public String getLang() {
    return lang;
  }

  /**
   * Sets the value of the lang property.
   * 
   * @param value allowed object is {@link String }
   */
  public void setLang(final String value) {
    this.lang = value;
  }

}
