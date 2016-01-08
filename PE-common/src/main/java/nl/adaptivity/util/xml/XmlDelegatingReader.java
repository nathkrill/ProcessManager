package nl.adaptivity.util.xml;

import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;
import nl.adaptivity.xml.XmlStreaming.EventType;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;


/**
 * Simple baseclass for a delagting XmlReader.
 * Created by pdvrieze on 16/11/15.
 */
public class XmlDelegatingReader implements XmlReader {

  protected final XmlReader mDelegate;

  protected XmlDelegatingReader(final XmlReader delegate) {
    mDelegate = delegate;
  }

  @Override
  public final EventType nextTag() throws XmlException {
    EventType event;
    while ((event=next())!=EventType.START_ELEMENT && event!=EventType.END_ELEMENT && event!=null) {
      if (event==EventType.TEXT) {
        if (! XmlUtil.isXmlWhitespace(getText())) {
          throw new XmlException("Unexpected text content");
        }
      }
    }
    return event;
  }

  @Override
  public boolean hasNext() throws XmlException {
    return mDelegate.hasNext();
  }

  @Override
  public EventType next() throws XmlException {
    return mDelegate.next();
  }

  @Override
  public CharSequence getNamespaceUri() throws XmlException {
    return mDelegate.getNamespaceUri();
  }

  @Override
  public CharSequence getLocalName() throws XmlException {
    return mDelegate.getLocalName();
  }

  @Override
  public CharSequence getPrefix() throws XmlException {
    return mDelegate.getPrefix();
  }

  @Override
  public QName getName() throws XmlException {
    return mDelegate.getName();
  }

  @Override
  public void require(final EventType type, final CharSequence namespace, final CharSequence name) throws XmlException {
    mDelegate.require(type, namespace, name);
  }

  @Override
  public int getDepth() throws XmlException {
    return mDelegate.getDepth();
  }

  @Override
  public CharSequence getText() throws XmlException {
    return mDelegate.getText();
  }

  @Override
  public int getAttributeCount() throws XmlException {
    return mDelegate.getAttributeCount();
  }

  @Override
  public CharSequence getAttributeNamespace(final int i) throws XmlException {
    return mDelegate.getAttributeNamespace(i);
  }

  @Override
  public CharSequence getAttributePrefix(final int i) throws XmlException {
    return mDelegate.getAttributePrefix(i);
  }

  @Override
  public CharSequence getAttributeLocalName(final int i) throws XmlException {
    return mDelegate.getAttributeLocalName(i);
  }

  @Override
  public QName getAttributeName(final int i) throws XmlException {
    return mDelegate.getAttributeName(i);
  }

  @Override
  public CharSequence getAttributeValue(final int i) throws XmlException {
    return mDelegate.getAttributeValue(i);
  }

  @Override
  public EventType getEventType() throws XmlException {
    return mDelegate.getEventType();
  }

  @Override
  public CharSequence getAttributeValue(final CharSequence nsUri, final CharSequence localName) throws XmlException {
    return mDelegate.getAttributeValue(nsUri, localName);
  }

  @Override
  public int getNamespaceStart() throws XmlException {
    return mDelegate.getNamespaceStart();
  }

  @Override
  public int getNamespaceEnd() throws XmlException {
    return mDelegate.getNamespaceEnd();
  }

  @Override
  public CharSequence getNamespacePrefix(final int i) throws XmlException {
    return mDelegate.getNamespacePrefix(i);
  }

  @Override
  public void close() throws XmlException {
    mDelegate.close();
  }

  @Override
  public CharSequence getNamespaceUri(final int i) throws XmlException {
    return mDelegate.getNamespaceUri(i);
  }

  @Override
  public CharSequence getNamespacePrefix(final CharSequence namespaceUri) throws XmlException {
    return mDelegate.getNamespacePrefix(namespaceUri);
  }

  @Override
  public boolean isWhitespace() throws XmlException {
    return mDelegate.isWhitespace();
  }

  @Override
  public boolean isEndElement() throws XmlException {
    return mDelegate.isEndElement();
  }

  @Override
  public boolean isCharacters() throws XmlException {
    return mDelegate.isCharacters();
  }

  @Override
  public boolean isStartElement() throws XmlException {
    return mDelegate.isStartElement();
  }

  @Override
  public String getNamespaceUri(final CharSequence prefix) throws XmlException {
    return mDelegate.getNamespaceUri(prefix);
  }

  @Override
  public String getLocationInfo() {
    return mDelegate.getLocationInfo();
  }

  @Override
  public NamespaceContext getNamespaceContext() throws XmlException {
    return mDelegate.getNamespaceContext();
  }

  @Override
  public CharSequence getEncoding() {
    return mDelegate.getEncoding();
  }

  @Override
  public Boolean getStandalone() {
    return mDelegate.getStandalone();
  }

  @Override
  public CharSequence getVersion() {
    return mDelegate.getVersion();
  }
}
