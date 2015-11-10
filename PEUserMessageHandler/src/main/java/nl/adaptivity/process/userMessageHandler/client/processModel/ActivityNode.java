package nl.adaptivity.process.userMessageHandler.client.processModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;
import nl.adaptivity.gwt.ext.client.XMLUtil;

import java.util.*;


public class ActivityNode extends ProcessNode {

  private final String aName;

  private String aPredecessorId;

  private String aOperation;

  private String aServiceNS;

  private String aServiceName;

  private String aEndpoint;

  private List<Element> aImports;

  private List<Element> aExports;

  private String aCondition;

  private ProcessNode aPredecessor;

  private Set<ProcessNode> aSuccessors;

  public ActivityNode(final String id, final String name, final String predecessor) {
    super(id);
    aName = name;
    aPredecessorId = predecessor;
  }

  public static ActivityNode fromXml(final Element node) {
    String id = null;
    String name = null;
    String predecessor = null;
    {
      final NamedNodeMap attrs = node.getAttributes();
      final int attrCount = attrs.getLength();
      for (int i = 0; i < attrCount; ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("id".equals(attr.getName())) {
            id = attr.getValue();
          } else if ("predecessor".equals(attr.getName())) {
            predecessor = attr.getValue();
          } else if ("name".equals(attr.getName())) {
            name = attr.getValue();
          } else {
            GWT.log("Unsupported attribute in activitynode " + attr.getName(), null);
          }
        }
      }
    }

    final ActivityNode result = new ActivityNode(id, name, predecessor);

    final List<Element> imports = new ArrayList<Element>();
    final List<Element> exports = new ArrayList<Element>();
    Node message;

    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (XMLUtil.isNS(ProcessModel.PROCESSMODEL_NS, child)) {

        if (XMLUtil.isLocalPart("import", child)) {
          imports.add(importFromXml((Element) child));
        } else if (XMLUtil.isLocalPart("export", child)) {
          exports.add(exportFromXml((Element) child));
        } else if (XMLUtil.isLocalPart("message", child)) {
          {
            final NamedNodeMap attrs = child.getAttributes();
            final int attrCnt = attrs.getLength();
            for (int i = 0; i < attrCnt; ++i) {
              final Attr attr = (Attr) attrs.item(i);
              if (XMLUtil.isNS(null, attr)) {
                if ("serviceNS".equals(attr.getName())) {
                  result.setServiceNS(attr.getValue());
                } else if ("serviceName".equals(attr.getName())) {
                  result.setServiceName(attr.getValue());
                } else if ("endpoint".equals(attr.getName())) {
                  result.setEndpoint(attr.getValue());
                } else if ("operation".equals(attr.getName())) {
                  result.setOperation(attr.getValue());
                } else {
                  GWT.log("Unsupported attribute " + attr, null);
                }
              } else {
                GWT.log("Unsupported attribute " + attr, null);
              }
            }
          }


          message = ((Element) child).getFirstChild();
          if (message.getNextSibling() != null) {
            GWT.log("Too many children to activity message", null);
          }
        } else if (XMLUtil.isLocalPart("condition", child)) {
          result.setCondition(conditionFromXml((Element) child));
        }
      }
    }
    result.setImports(imports);
    result.setExports(exports);

    return result;
  }

  private static String conditionFromXml(final Element elem) {
    return XMLUtil.getTextChildren(elem);
  }

  private static Element importFromXml(final Element child) {
    return child;
  }

  private static Element exportFromXml(final Element child) {
    return child;
  }

  private void setExports(final List<Element> exports) {
    aExports = exports;
  }

  private void setImports(final List<Element> imports) {
    aImports = imports;
  }

  private void setCondition(final String condition) {
    aCondition = condition;
  }

  private void setOperation(final String value) {
    aOperation = value;
  }

  private void setEndpoint(final String value) {
    aEndpoint = value;
  }

  private void setServiceName(final String value) {
    aServiceName = value;
  }

  private void setServiceNS(final String value) {
    aServiceNS = value;
  }

  @Override
  public void resolvePredecessors(final Map<String, ProcessNode> map) {
    final ProcessNode predecessor = map.get(aPredecessorId);
    if (predecessor != null) {
      aPredecessorId = predecessor.getId();
      aPredecessor = predecessor;
      aPredecessor.ensureSuccessor(this);
    }
  }

  @Override
  public void ensureSuccessor(final ProcessNode node) {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    aSuccessors.add(node);
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    if (aSuccessors == null) {
      aSuccessors = new LinkedHashSet<ProcessNode>();
    }
    return aSuccessors;
  }


  @Override
  public Collection<ProcessNode> getPredecessors() {
    return Collections.singletonList(aPredecessor);
  }
}
