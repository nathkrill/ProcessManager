package nl.adaptivity.process.editor.android;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;

import nl.adaptivity.diagram.Canvas;
import nl.adaptivity.diagram.Pen;
import nl.adaptivity.diagram.Rectangle;
import nl.adaptivity.process.clientProcessModel.ClientProcessNode;
import nl.adaptivity.process.diagram.DrawableActivity;
import nl.adaptivity.process.diagram.DrawableEndNode;
import nl.adaptivity.process.diagram.DrawableJoin;
import nl.adaptivity.process.diagram.DrawableProcessModel;
import nl.adaptivity.process.diagram.DrawableProcessNode;
import nl.adaptivity.process.diagram.DrawableStartNode;
import nl.adaptivity.process.diagram.LayoutAlgorithm;
import nl.adaptivity.process.processModel.ProcessNodeSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public class PMParser {


  private static class RefNode extends ClientProcessNode<DrawableProcessNode> implements DrawableProcessNode {

    final String aRef;

    public RefNode(String pRef) {
      aRef = pRef;
    }

    @Override
    public void draw(Canvas pArg0, Rectangle pArg1) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Rectangle getBounds() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addSuccessor(DrawableProcessNode pArg0) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isPredecessorOf(DrawableProcessNode pArg0) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ProcessNodeSet<DrawableProcessNode> getPredecessors() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ProcessNodeSet<DrawableProcessNode> getSuccessors() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setPredecessor(DrawableProcessNode pArg0) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setSuccessor(DrawableProcessNode pArg0) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Pen getPen() {
      return null;
    }

    @Override
    public void setFGPen(Pen pPen) {
      // ignore
    }

  }

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  static DrawableProcessModel parseProcessModel(InputStream pIn, LayoutAlgorithm<DrawableProcessNode> pLayoutAlgorithm) {
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      XmlPullParser in = factory.newPullParser();
      in.setInput(pIn, "utf-8");

      if(in.nextTag()==START_TAG && NS_PROCESSMODEL.equals(in.getNamespace()) && "processModel".equals(in.getName())){
        ArrayList<DrawableProcessNode> modelElems = new ArrayList<DrawableProcessNode>();
        String modelName = in.getAttributeValue(XMLConstants.NULL_NS_URI, "name");
        Map<String, DrawableProcessNode> nodeMap = new HashMap<String, DrawableProcessNode>();
        for(int type = in.nextTag(); type!=END_TAG; type = in.nextTag()) {

          DrawableProcessNode node = parseNode(in, nodeMap);
          modelElems.add(node);
          if (node.getId()!=null) {
            nodeMap.put(node.getId(), node);
          }
        }
        return new DrawableProcessModel(modelName, modelElems, pLayoutAlgorithm);

      } else {
        return null;
      }
    } catch (Exception e) {
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
  }

  private static Collection<? extends DrawableEndNode> getEndNodes(Map<String, DrawableProcessNode> pNodes) {
    List<DrawableEndNode> result = new ArrayList<DrawableEndNode>();
    for(DrawableProcessNode node: pNodes.values()) {
      resolveRefs(node, pNodes);
      if (node instanceof DrawableEndNode) {
        result.add((DrawableEndNode) node);
      }
    }
    return result;
  }

  private static void resolveRefs(DrawableProcessNode pNode, Map<String, DrawableProcessNode> pNodes) {
    List<DrawableProcessNode> preds = new ArrayList<DrawableProcessNode>();
    boolean changed = false;
    for(DrawableProcessNode pred: pNode.getPredecessors()) {
      if (pred instanceof RefNode) {
        String ref = ((RefNode)pred).aRef;
        if (pNode.getPredecessors().size()==1) {
          pNode.setPredecessors(Collections.singleton(pNodes.get(ref)));
          return;
        } else {
          preds.add(pNodes.get(ref));
          changed = true;
        }
      } else {
        preds.add(pred);
      }
    }
    if (changed) {
      pNode.setPredecessors(preds);;
    }
  }

  private static DrawableProcessNode parseNode(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes) throws XmlPullParserException, IOException {
    if (!NS_PROCESSMODEL.equals(pIn.getNamespace())) {
      throw new IllegalArgumentException("Invalid process model");
    }
    if ("start".equals(pIn.getName())) {
      return parseStart(pIn, pNodes);
    } else if ("activity".equals(pIn.getName())) {
      return parseActivity(pIn, pNodes);
    } else if ("join".equals(pIn.getName())) {
      return parseJoin(pIn, pNodes);
    } else if ("end".equals(pIn.getName())) {
      return parseEnd(pIn, pNodes);
    }
    throw new UnsupportedOperationException("Unsupported tag");
  }

  private static DrawableProcessNode parseStart(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes) throws XmlPullParserException, IOException {
    DrawableStartNode result = new DrawableStartNode();
    parseCommon(pIn, pNodes, result);
    if (pIn.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static DrawableProcessNode parseActivity(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes) throws XmlPullParserException, IOException {
    DrawableActivity result = new DrawableActivity();
    parseCommon(pIn, pNodes, result);
    String name = pIn.getAttributeValue(XMLConstants.NULL_NS_URI, "name");
    if (name!=null && name.length()>0) {
      result.setName(name);
    }
    for(int type = pIn.next(); type!=END_TAG; type = pIn.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(pIn);
        break;
      default:
          // ignore
      }
    }
    return result;
  }

  private static void parseUnknownTag(XmlPullParser pIn) throws XmlPullParserException, IOException {
    for(int type = pIn.next(); type!=END_TAG; type = pIn.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(pIn);
        break;
      default:
          // ignore
      }
    }
  }

  private static DrawableProcessNode parseJoin(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes) throws XmlPullParserException, IOException {
    DrawableJoin result = new DrawableJoin();
    parseCommon(pIn, pNodes, result);
    parseJoinAttrs(pIn, result);
    List<DrawableProcessNode> predecessors = new ArrayList<DrawableProcessNode>();

    for(int type = pIn.nextTag(); type!=END_TAG; type = pIn.nextTag()) {
      if (! (NS_PROCESSMODEL.equals(pIn.getNamespace()) && "predecessor".equals(pIn.getName()))) {
        throw new IllegalArgumentException("Invalid process model");
      }
      StringBuilder name = new StringBuilder();
      type = pIn.next();
      while (type!=END_TAG) {
        if (type==TEXT) {
          name.append(pIn.getText());
        } else if (type==START_TAG) {
          throw new IllegalArgumentException("Invalid process model");
        }
        type=pIn.next();
      }
      predecessors.add(getPredecessor(name.toString(), pNodes));
    }
    result.setPredecessors(predecessors);

    return result;
  }

  private static void parseJoinAttrs(XmlPullParser pIn, DrawableJoin pNode) {
    for(int i=0; i< pIn.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(pIn.getAttributeNamespace(i))) {
        final String aname = pIn.getAttributeName(i);
        if ("min".equals(aname)) {
          pNode.setMin(Integer.parseInt(pIn.getAttributeValue(i)));
        } else if ("max".equals(aname)) {
          pNode.setMax(Integer.parseInt(pIn.getAttributeValue(i)));
        }
      }
    }
  }

  private static DrawableProcessNode parseEnd(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes) throws XmlPullParserException, IOException {
    DrawableEndNode result = new DrawableEndNode();
    parseCommon(pIn, pNodes, result);
    if (pIn.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static void parseCommon(XmlPullParser pIn, Map<String, DrawableProcessNode> pNodes, DrawableProcessNode pNode) {
    for(int i=0; i< pIn.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(pIn.getAttributeNamespace(i))) {
        final String aname = pIn.getAttributeName(i);
        if ("x".equals(aname)) {
          pNode.setX(Double.parseDouble(pIn.getAttributeValue(i)));
        } else if ("y".equals(aname)) {
          pNode.setY(Double.parseDouble(pIn.getAttributeValue(i)));
        } else if ("id".equals(aname)) {
          pNode.setId(pIn.getAttributeValue(i));
        } else if ("predecessor".equals(aname)) {
          pNode.setPredecessors(getPredecessors(pIn.getAttributeValue(i),pNodes));
        }
      }
    }
  }

  private static Collection<? extends DrawableProcessNode> getPredecessors(String pName, Map<String, DrawableProcessNode> pNodes) {
    final DrawableProcessNode predecessor = getPredecessor(pName, pNodes);
    return predecessor==null ? null : Collections.singleton(predecessor);
  }

  private static DrawableProcessNode getPredecessor(String pName, Map<String, DrawableProcessNode> pNodes) {
    DrawableProcessNode val = pNodes.get(pName);
    if (val==null) {
      val = new RefNode(pName);
    }
    return val;
  }

}
