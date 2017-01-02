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
// Generated on: 2009.08.27 at 08:15:55 PM CEST
//


package nl.adaptivity.process.processModel;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@XmlDeserializer(XmlDefineType.Factory.class)
public class XmlDefineType extends XPathHolder implements IXmlDefineType {

  public static class Factory implements XmlDeserializerFactory<XmlDefineType> {

    @NotNull
    @Override
    public XmlDefineType deserialize(@NotNull final XmlReader reader) throws XmlException {
      return XmlDefineType.deserialize(reader);
    }
  }


  @ProcessModelDSL
  public static class Builder {

    public @Nullable String name;
    public @Nullable String path;
    public @NotNull char[] content;
    public @NotNull List<Namespace> nsContext;

    Builder() {
      name = null;
      path = null;
      content = new char[0];
      nsContext = new ArrayList<>();
    }

    Builder(IXmlResultType orig) {
      name = orig.getName();
      path = orig.getPath();
      content = Arrays.copyOf(orig.getContent(), orig.getContent().length);
      nsContext = new ArrayList<>();
      Iterable<Namespace> origContext = orig.getOriginalNSContext();
      if (origContext!=null) {
        for(Namespace ns:origContext) {
          nsContext.add(ns);
        }
      }
    }

    public XmlResultType build() {
      return new XmlResultType(name, path, content, nsContext);
    }
  }


  public static final String ELEMENTLOCALNAME = "define";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  private String refNode;

  private String refName;

  public XmlDefineType() {}

  public XmlDefineType(final String name, final String refNode, final String refName, final String path, final char[] content, final Iterable<nl.adaptivity.xml.Namespace> originalNSContext) {
    super(content, originalNSContext, path, name);
    this.refNode = refNode;
    this.refName = refName;
  }

  @NotNull
  public static XmlDefineType deserialize(@NotNull final XmlReader in) throws XmlException {
    return XPathHolder.deserialize(in, new XmlDefineType());
  }

  @NotNull
  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, @NotNull final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case "refnode": setRefNode(StringUtil.toString(attributeValue)); return true;
      case "refname": setRefName(StringUtil.toString(attributeValue)); return true;
      default:
        return super.deserializeAttribute(attributeNamespace, attributeLocalName, attributeValue);
    }
  }

  @Override
  protected void serializeStartElement(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX));
  }

  @Override
  protected void serializeEndElement(@NotNull final XmlWriter out) throws XmlException {
    XmlWriterUtil.endTag(out, new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX));
  }

  @Override
  protected void serializeAttributes(@NotNull final XmlWriter out) throws XmlException {
    super.serializeAttributes(out);
    XmlWriterUtil.writeAttribute(out, "refnode", getRefNode());
    XmlWriterUtil.writeAttribute(out, "refname", getRefName());
  }

  /* (non-Javadoc)
     * @see nl.adaptivity.process.processModel.IXmlDefineType#getRefNode()
     */
  @Override
  public String getRefNode() {
    return refNode;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.IXmlDefineType#setRefNode(String)
   */
  @Override
  public void setRefNode(final String value) {
    this.refNode = value;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#getName()
   */
  @Override
  public String getRefName() {
    return refName;
  }

  /* (non-Javadoc)
   * @see nl.adaptivity.process.processModel.XmlImportType#setName(java.lang.String)
   */
  @Override
  public void setRefName(final String value) {
    this.refName = value;
  }

  /**
   *
   * @param export
   * @return
   */
  @NotNull
  public static XmlDefineType get(@NotNull final IXmlDefineType export) {
    if (export instanceof XmlDefineType) { return (XmlDefineType) export; }
    return new XmlDefineType(export.getName(), export.getRefNode(), export.getRefName(), export.getPath(), export.getContent(), export.getOriginalNSContext());
  }
}
