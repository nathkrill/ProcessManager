package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.*;

import nl.adaptivity.gwt.ext.client.XMLUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.xml.client.Attr;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NamedNodeMap;
import com.google.gwt.xml.client.Node;


public class EndNode extends ProcessNode {

  private String aPredecessorId;

  private List<Element> aExports;

  private ProcessNode aPredecessor;

  public EndNode(final String pId, final String pPredecessorName) {
    super(pId);
    aPredecessorId = pPredecessorName;
  }

  public static EndNode fromXml(final Element pNode) {
    String id = null;
    String predecessor = null;
    {
      final NamedNodeMap attrs = pNode.getAttributes();
      final int attrCount = attrs.getLength();
      for (int i = 0; i < attrCount; ++i) {
        final Attr attr = (Attr) attrs.item(i);
        if (XMLUtil.isNS(null, attr)) {
          if ("id".equals(attr.getName())) {
            id = attr.getValue();
          } else if ("predecessor".equals(attr.getName())) {
            predecessor = attr.getValue();
          } else {
            GWT.log("Unsupported attribute in endnode " + attr.getName(), null);
          }
        }
      }
    }

    final EndNode result = new EndNode(id, predecessor);

    final List<Element> exports = new ArrayList<Element>();

    for (Node child = pNode.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (XMLUtil.isNS(ProcessModel.PROCESSMODEL_NS, child)) {

        if (XMLUtil.isLocalPart("export", child)) {
          exports.add(exportFromXml((Element) child));
        } else {
          GWT.log("Unexpected subtag " + child, null);
        }
      } else {
        GWT.log("Unexpected subtag " + child, null);
      }
    }
    result.setExports(exports);

    return result;
  }

  private void setExports(final List<Element> pExports) {
    aExports = pExports;
  }

  private static Element exportFromXml(final Element pChild) {
    return pChild;
  }

  @Override
  public void resolvePredecessors(final Map<String, ProcessNode> pMap) {
    final ProcessNode predecessor = pMap.get(aPredecessorId);
    if (predecessor != null) {
      aPredecessorId = predecessor.getId();
      aPredecessor = predecessor;
      aPredecessor.ensureSuccessor(this);
    }
  }

  @Override
  public void ensureSuccessor(final ProcessNode pNode) {
    throw new UnsupportedOperationException("end nodes never have successors");
  }

  @Override
  public Collection<ProcessNode> getSuccessors() {
    return new ArrayList<ProcessNode>(0);
  }

  @Override
  public Collection<ProcessNode> getPredecessors() {
    return Collections.singletonList(aPredecessor);
  }

}
