//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.08.27 at 08:15:55 PM CEST 
//


package nl.adaptivity.process.processModel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java class for ImportType complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="ImportType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="path" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "ImportType")
public class XmlImportType implements IXmlImportType {

  public static final String ELEMENTNAME = "import";

  private String name;

  private String path;

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IXmlImportType#getName()
   */
  @Override
  @XmlAttribute(required = true)
  public String getName() {
    return name;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IXmlImportType#setName(java.lang.String)
   */
  @Override
  public void setName(final String value) {
    this.name = value;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IXmlImportType#getPath()
   */
  @Override
  @XmlAttribute
  public String getPath() {
    return path;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IXmlImportType#setPath(java.lang.String)
   */
  @Override
  public void setPath(final String value) {
    this.path = value;
  }

}
