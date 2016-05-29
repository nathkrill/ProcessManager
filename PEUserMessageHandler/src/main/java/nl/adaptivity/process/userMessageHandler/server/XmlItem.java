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

package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.StringUtil;
import nl.adaptivity.process.userMessageHandler.server.UserTask.TaskItem;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.xml.*;
import nl.adaptivity.util.xml.*;
import nl.adaptivity.xml.XmlUtil;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class XmlItem implements TaskItem, XmlSerializable, SimpleXmlDeserializable {

  public class Factory implements XmlDeserializerFactory<XmlItem> {

    @Override
    public XmlItem deserialize(final XmlReader reader) throws XmlException {
      return XmlItem.deserialize(reader);
    }
  }

  public static final String ELEMENTLOCALNAME = "item";
  public static final QName ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, ELEMENTLOCALNAME, "umh");
  private static final QName OPTION_ELEMENTNAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, "option", "umh");

  private String mName;
  private String mLabel;
  private String mType;
  private String mValue;
  private String mParams;
  private List<String> mOptions;

  public static XmlItem deserialize(final XmlReader in) throws XmlException {
    return XmlUtil.<XmlItem>deserializeHelper(new XmlItem(), in);
  }

  @Override
  public boolean deserializeChild(final XmlReader in) throws XmlException {
    if (XmlReaderUtil.isElement(in, OPTION_ELEMENTNAME)) {
      if (mOptions==null) { mOptions = new ArrayList<>(); }
      mOptions.add(StringUtil.toString(XmlReaderUtil.readSimpleElement(in)));
      return true;
    }
    return false;
  }

  @Override
  public boolean deserializeChildText(final CharSequence elementText) {
    return false;
  }

  @Override
  public boolean deserializeAttribute(final CharSequence attributeNamespace, final CharSequence attributeLocalName, final CharSequence attributeValue) {
    switch (attributeLocalName.toString()) {
      case "name": mName = attributeValue.toString(); return true;
      case "label": mLabel = attributeValue.toString(); return true;
      case "params": mParams = attributeValue.toString(); return true;
      case "type": mType = attributeValue.toString(); return true;
      case "value": mValue = attributeValue.toString(); return true;
    }
    return false;
  }

  @Override
  public void onBeforeDeserializeChildren(final XmlReader in) throws XmlException {
    // do nothing
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public void serialize(final XmlWriter out) throws XmlException {
    XmlWriterUtil.smartStartTag(out, ELEMENTNAME);
    XmlWriterUtil.writeAttribute(out, "name", mName);
    XmlWriterUtil.writeAttribute(out, "label", mLabel);
    XmlWriterUtil.writeAttribute(out, "params", mParams);
    XmlWriterUtil.writeAttribute(out, "type", mType);
    XmlWriterUtil.writeAttribute(out, "value", mValue);
    if (mOptions!=null) {
      for(String option:mOptions) {
        XmlWriterUtil.writeSimpleElement(out, OPTION_ELEMENTNAME, option);
      }
    }
    XmlWriterUtil.endTag(out, ELEMENTNAME);
  }

  @Override
  public String getName() {
    return mName;
  }

  public void setName(String name) {
    mName = name;
  }

  @Override
  public String getLabel() {
    return mLabel;
  }

  public void setLabel(String label) {
    mLabel = label;
  }

  @Override
  public String getParams() {
    return mParams;
  }

  public void setParams(String params) {
    mParams = params;
  }

  @Override
  public String getType() {
    return mType;
  }

  public void setType(String type) {
    mType = type;
  }

  @Override
  public String getValue() {
    return mValue;
  }

  public void setValue(String value) {
    mValue = value;
  }

  @Override
  public List<String> getOptions() {
    if (mOptions==null) { mOptions = new ArrayList<>(); }
    return mOptions;
  }

  public void setOptions(List<String> options) {
    mOptions = options;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mName == null) ? 0 : mName.hashCode());
    result = prime * result + ((mOptions == null || mOptions.isEmpty()) ? 0 : mOptions.hashCode());
    result = prime * result + ((mType == null) ? 0 : mType.hashCode());
    result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    XmlItem other = (XmlItem) obj;
    if (mName == null) {
      if (other.mName != null)
        return false;
    } else if (!mName.equals(other.mName))
      return false;
    if (mOptions==null || mOptions.isEmpty()) {
      if (other.mOptions!=null && ! mOptions.isEmpty())
        return false;
    } else if (!mOptions.equals(other.mOptions))
      return false;
    if (mType == null) {
      if (other.mType != null)
        return false;
    } else if (!mType.equals(other.mType))
      return false;
    if (mValue == null) {
      if (other.mValue != null)
        return false;
    } else if (!mValue.equals(other.mValue))
      return false;
    return true;
  }

  public static Collection<XmlItem> get(Collection<? extends TaskItem> source) {
    if (source.isEmpty()) {
      return Collections.emptyList();
    }
    if (source.size()==1) {
      return Collections.singleton(get(source.iterator().next()));
    }
    ArrayList<XmlItem> result = new ArrayList<>(source.size());
    for(TaskItem item: source) {
      result.add(get(item));
    }
    return result;
  }

  public static XmlItem get(TaskItem orig) {
    if (orig instanceof XmlItem) { return (XmlItem) orig; }
    if (orig == null) { return null; }
    XmlItem result = new XmlItem();
    result.mName = orig.getName();
    result.mLabel = orig.getLabel();
    result.mType = orig.getType();
    result.mValue = orig.getValue();
    result.mParams = orig.getParams();
    result.mOptions = new ArrayList<>(orig.getOptions());
    return result;
  }
}