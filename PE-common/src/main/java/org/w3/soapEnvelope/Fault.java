//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.09.24 at 08:12:58 PM CEST 
//


package org.w3.soapEnvelope;

import javax.xml.bind.annotation.*;


/**
 * Fault reporting structure
 * <p>
 * Java class for Fault complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="Fault">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Code" type="{http://www.w3.org/2003/05/soap-envelope}faultcode"/>
 *         &lt;element name="Reason" type="{http://www.w3.org/2003/05/soap-envelope}faultreason"/>
 *         &lt;element name="Node" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="Role" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="Detail" type="{http://www.w3.org/2003/05/soap-envelope}detail" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Fault", propOrder = { "code", "reason", "node", "role", "detail" })
public class Fault {

  @XmlElement(name = "Code", required = true)
  protected Faultcode code;

  @XmlElement(name = "Reason", required = true)
  protected Faultreason reason;

  @XmlElement(name = "Node")
  @XmlSchemaType(name = "anyURI")
  protected String node;

  @XmlElement(name = "Role")
  @XmlSchemaType(name = "anyURI")
  protected String role;

  @XmlElement(name = "Detail")
  protected Detail detail;

  /**
   * Gets the value of the code property.
   * 
   * @return possible object is {@link Faultcode }
   */
  public Faultcode getCode() {
    return code;
  }

  /**
   * Sets the value of the code property.
   * 
   * @param value allowed object is {@link Faultcode }
   */
  public void setCode(final Faultcode value) {
    this.code = value;
  }

  /**
   * Gets the value of the reason property.
   * 
   * @return possible object is {@link Faultreason }
   */
  public Faultreason getReason() {
    return reason;
  }

  /**
   * Sets the value of the reason property.
   * 
   * @param value allowed object is {@link Faultreason }
   */
  public void setReason(final Faultreason value) {
    this.reason = value;
  }

  /**
   * Gets the value of the node property.
   * 
   * @return possible object is {@link String }
   */
  public String getNode() {
    return node;
  }

  /**
   * Sets the value of the node property.
   * 
   * @param value allowed object is {@link String }
   */
  public void setNode(final String value) {
    this.node = value;
  }

  /**
   * Gets the value of the role property.
   * 
   * @return possible object is {@link String }
   */
  public String getRole() {
    return role;
  }

  /**
   * Sets the value of the role property.
   * 
   * @param value allowed object is {@link String }
   */
  public void setRole(final String value) {
    this.role = value;
  }

  /**
   * Gets the value of the detail property.
   * 
   * @return possible object is {@link Detail }
   */
  public Detail getDetail() {
    return detail;
  }

  /**
   * Sets the value of the detail property.
   * 
   * @param value allowed object is {@link Detail }
   */
  public void setDetail(final Detail value) {
    this.detail = value;
  }

}
