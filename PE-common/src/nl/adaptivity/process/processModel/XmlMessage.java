//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * <p>Java class for Message complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Message">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any processContents='lax'/>
 *       &lt;/sequence>
 *       &lt;attribute name="serviceNS" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="endpoint" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="operation" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="serviceName" type="{http://www.w3.org/2001/XMLSchema}NCName" />
 *       &lt;attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Message", propOrder = { "any" })
public class XmlMessage {

    // These are managed on the methods.
    private ArrayList<Object> aAny;
    private Node aBody;
    
    @XmlAttribute(name = "serviceNS")
    protected String serviceNS;
    @XmlAttribute(name = "endpoint")
    protected String endpoint;
    @XmlAttribute(name = "operation")
    protected QName operation;
    @XmlAttribute(name = "serviceName")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected QName serviceName;
    @XmlAttribute(name = "url")
    protected String url;
    @XmlAttribute(name = "method")
    protected String method;

    /**
     * Gets the value of the service property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    @XmlAttribute(name="serviceName", required = true)
    public String getServiceName() {
      return serviceName.getLocalPart();
    }

    public void setServiceName(String pName) {
      if (serviceName==null) {
        serviceName = new QName(pName);
      } else {
        serviceName = new QName(serviceName.getNamespaceURI(), pName);
      }
    }


    @XmlAttribute(name="serviceNS", required = true)
    public String getServiceNS() {
      return serviceName.getNamespaceURI();
    }

    public void setServiceNS(String pNamespace) {
      if (serviceName==null) {
        serviceName = new QName(pNamespace, "xx");
      } else {
        serviceName = new QName(pNamespace, serviceName.getLocalPart());
      }
    }

    public QName getService() {
        return serviceName;
    }

    /**
     * Sets the value of the service property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setService(QName value) {
        this.serviceName = value;
    }

    /**
     * Gets the value of the endpoint property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sets the value of the endpoint property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setEndpoint(String value) {
        this.endpoint = value;
    }

    /**
     * Gets the value of the operation property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public QName getOperation() {
        return operation;
    }

    @XmlAnyElement(lax=false)
    public Collection<Object> getAny() {
      if (aAny == null) {
        aAny = new ArrayList<Object>(1);
        if (aBody!=null) {
          aAny.add(aBody);
          aBody = null;
        }
      }
      return aAny;
    }

    public Node getMessageBody() {
      if (aBody ==null && aAny!=null) {
        Iterator<Object> it = aAny.iterator();
        while(it.hasNext()) {
          Object next = it.next();
          if ((next instanceof Element) || (next instanceof Document) || (next instanceof DocumentFragment)) {
            if (aBody !=null) {
              throw new IllegalStateException("Only one member allowed");
            }
            aBody = (Node) next;
          }
        }
        if (aBody!=null) {
          aAny = null;
        }
      }

      return aBody;
    }

    public void setMessageBody(Object o) {
      if (o instanceof Node) {
        aBody = (Node) o;
      }
    }

    /**
     * Sets the value of the operation property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setOperation(QName value) {
        this.operation = value;
    }

    public Source getBodySource() {
      return new DOMSource(getMessageBody());
    }


    /**
     * Gets the value of the url property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the method property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the method property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMethod(String value) {
        this.method = value;
    }
    
    
    @Override
    public String toString() {
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer t;
      try {
        t = tf.newTransformer();
      } catch (TransformerConfigurationException e) {
        return super.toString();
      }
      StringWriter sw = new StringWriter();
      StreamResult sr = new StreamResult(sw);
      try {
        t.transform(getBodySource(), sr);
      } catch (TransformerException e) {
        return super.toString();
      }
      return sw.toString();
    }
}
