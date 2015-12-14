//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import java.net.URI;


/**
 * <p>
 * Java class for Message complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
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
 */
@XmlDeserializer(XmlMessage.Factory.class)
public class XmlMessage extends BaseMessage implements IXmlMessage, ExtXmlDeserializable {

  public static class Factory implements XmlDeserializerFactory {

    @NotNull
    @Override
    public Object deserialize(@NotNull final XmlReader in) throws XmlException {
      return XmlMessage.deserialize(in);
    }
  }

  public static final String ELEMENTLOCALNAME = "message";

  public static final QName ELEMENTNAME=new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public XmlMessage() { /* default constructor */ }


  public XmlMessage(final QName service, final String endpoint, final String operation, final String url, final String method, final String contentType, final CompactFragment messageBody) {
    super(service, endpoint, operation, url, method, contentType, messageBody);
  }


  @NotNull
  public static XmlMessage get(final IXmlMessage message) {
    if (message==null) { return null; }
    if (message instanceof XmlMessage) { return (XmlMessage) message; }
    return new XmlMessage(message.getService(),
                          message.getEndpoint(),
                          message.getOperation(),
                          message.getUrl(),
                          message.getMethod(),
                          message.getContentType(),
                          message.getMessageBody());
  }

  @NotNull
  public static XmlMessage deserialize(@NotNull final XmlReader in) throws XmlException {
    return XmlUtil.deserializeHelper(new XmlMessage(), in);
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  protected void serializeStartElement(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeStartElement(out, ELEMENTNAME);
  }

  @Override
  protected void serializeEndElement(@NotNull final XmlWriter out) throws XmlException {
    XmlUtil.writeEndElement(out, ELEMENTNAME);
  }

  @Nullable
  @Override
  public EndpointDescriptor getEndpointDescriptor() {
    final String url = getUrl();
    return new EndpointDescriptorImpl(getService(), getEndpoint(), url==null ? null : URI.create(url));
  }


  public void setContentType(final String type) {
    super.setType(type);
  }


}
