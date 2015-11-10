package nl.adaptivity.process.editor.android;

import android.util.Log;
import nl.adaptivity.process.clientProcessModel.ClientMessage;
import nl.adaptivity.process.clientProcessModel.ClientProcessModel;
import nl.adaptivity.process.clientProcessModel.SerializerAdapter;
import nl.adaptivity.process.diagram.*;
import nl.adaptivity.process.processModel.IXmlMessage;
import nl.adaptivity.process.util.Identifiable;
import nl.adaptivity.process.util.Identifier;
import org.w3c.dom.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import java.io.*;
import java.util.*;

import static org.xmlpull.v1.XmlPullParser.*;

public class PMParser {

  public static final String MIME_TYPE="application/x-processmodel";

  public static class XmlSerializerAdapter implements SerializerAdapter {

    private final XmlSerializer mSerializer;

    private int aIndent = 0;
    private boolean aPendingBreak = false;
    private boolean aExtraIndent = false;

    public XmlSerializerAdapter(XmlSerializer serializer) {
      mSerializer = serializer;
    }

    @Override
    public void addNamespace(String prefix, String namespace) {
      // TODO maybe record pending namespaces and only add them on startTag
      try {
        mSerializer.setPrefix(prefix, namespace);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void startTag(String namespace, String name, boolean addWs) {
      try {
        if (aPendingBreak) {
          printBreak();
        }
        printExtraIndent();
        mSerializer.startTag(namespace, name);
        ++aIndent;
        aPendingBreak = addWs;
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void endTag(String namespace, String name, boolean addWs) {
      try {
        mSerializer.endTag(namespace, name);
        --aIndent;
        if (addWs) {
          printBreak();
        }
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void printExtraIndent() throws IOException {
      if (aExtraIndent) {
        mSerializer.ignorableWhitespace("  ");
        aExtraIndent = false;
      }
    }

    private void printBreak() throws IOException {
      mSerializer.ignorableWhitespace("\n");
      for(int i=aIndent; i>1; --i) { mSerializer.ignorableWhitespace("  "); }
      aPendingBreak = false;
      aExtraIndent=true;
    }

    @Override
    public void addAttribute(String namespace, String name, String value) {
      try {
        mSerializer.attribute(namespace, name, value);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void text(String string) {
      try {
        if (aPendingBreak) {
          printBreak();
        }
        printExtraIndent();
        mSerializer.text(string);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void cdata(String data) {
      try {
        if (aPendingBreak) {
          printBreak();
        }
        printExtraIndent();
        mSerializer.cdsect(data);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void comment(String data) {
      try {
        if (aPendingBreak) {
          printBreak();
        }
        printExtraIndent();
        mSerializer.comment(data);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void entityReference(String entityRef) {
      try {
        if (aPendingBreak) {
          printBreak();
        }
        printExtraIndent();
        mSerializer.entityRef(entityRef);;
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void ignorableWhitespace(String string) {
      try {
        if (aPendingBreak) {
          printBreak();
        }
        printExtraIndent();
        mSerializer.ignorableWhitespace(string);
      } catch (IllegalArgumentException | IllegalStateException | IOException e) {
        throw new RuntimeException(e);
      }
    }


  }

  public static final String NS_PROCESSMODEL="http://adaptivity.nl/ProcessEngine/";

  public static void serializeProcessModel(OutputStream out, ClientProcessModel<?> processModel) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer(out);
    serializeProcessModel(serializer, processModel);
  }

  public static void serializeProcessModel(Writer out, ClientProcessModel<?> processModel) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer(out);
    serializeProcessModel(serializer, processModel);
  }

  private static XmlSerializer getSerializer() throws XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    final XmlSerializer serializer = factory.newSerializer();
    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", false);
    return serializer;
  }

  static XmlSerializer getSerializer(OutputStream out) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out, "UTF-8");
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  static XmlSerializer getSerializer(Writer out) throws XmlPullParserException, IOException {
    XmlSerializer serializer = getSerializer();
    try {
      serializer.setOutput(out);
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new IOException(e);
    }
    return serializer;
  }

  private static void serializeProcessModel(XmlSerializer serializer, ClientProcessModel<?> processModel) {
    try {
      serializer.startDocument(null, null);
      serializer.ignorableWhitespace("\n");
      processModel.serialize(new XmlSerializerAdapter(serializer));
      serializer.endDocument();
    } catch (IllegalArgumentException | IllegalStateException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static DrawableProcessModel parseProcessModel(Reader in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    XmlPullParser parser;
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      parser = factory.newPullParser();
      parser.setInput(in);
    } catch (Exception e){
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseProcessModel(parser, simpleLayoutAlgorithm, advancedAlgorithm);
  }

  public static DrawableProcessModel parseProcessModel(InputStream in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    XmlPullParser parser;
    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(true);
      parser = factory.newPullParser();
      parser.setInput(in, "utf-8");
    } catch (Exception e){
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
    return parseProcessModel(parser, simpleLayoutAlgorithm, advancedAlgorithm);
  }

  public static DrawableProcessModel parseProcessModel(XmlPullParser in, LayoutAlgorithm<DrawableProcessNode> simpleLayoutAlgorithm, LayoutAlgorithm<DrawableProcessNode> advancedAlgorithm) {
    try {

      if(in.nextTag()==START_TAG && NS_PROCESSMODEL.equals(in.getNamespace()) && "processModel".equals(in.getName())){
        ArrayList<DrawableProcessNode> modelElems = new ArrayList<>();
        String modelName = in.getAttributeValue(XMLConstants.NULL_NS_URI, "name");
        String uuid = in.getAttributeValue(XMLConstants.NULL_NS_URI, "uuid");
        String owner = in.getAttributeValue(XMLConstants.NULL_NS_URI, "owner");
        Map<String, DrawableProcessNode> nodeMap = new HashMap<>();
        for(int type = in.nextTag(); type!=END_TAG; type = in.nextTag()) {

          DrawableProcessNode node = parseNode(in, nodeMap, modelElems);
          modelElems.add(node);
          if (node.getId()!=null) {
            nodeMap.put(node.getId(), node);
          }
        }
        // Use list indexing as resolveRefs may add elements to the list.
        // We will need to still check those
        boolean noPos = false;
        for(int i=0; i< modelElems.size(); ++i) {
          final DrawableProcessNode elem = modelElems.get(i);
          resolveRefs(elem, nodeMap, modelElems);
          noPos|=Double.isNaN(elem.getX())||Double.isNaN(elem.getY());
        }
        final DrawableProcessModel drawableProcessModel = new DrawableProcessModel(uuid==null? null: UUID.fromString(uuid), modelName, modelElems, noPos ? advancedAlgorithm : simpleLayoutAlgorithm);
        if (owner!=null) { drawableProcessModel.setOwner(owner); }
        return drawableProcessModel;

      } else {
        return null;
      }
    } catch (Exception e) {
      Log.e(PMEditor.class.getName(), e.getMessage(), e);
      return null;
    }
  }

  private static void resolveRefs(DrawableProcessNode node, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) {
    for(Identifiable predid: node.getPredecessors()) {
      // It is a temporary predecessor
      if (! (predid instanceof DrawableProcessNode)) {
        // First remove the link with the temporary
        node.removePredecessor(predid);
        // Get the node that should replace the temporary
        DrawableProcessNode realNode = nodes.get(predid);
        // Add the node as successor to the real predecessor
        addAsSuccessor(realNode, node, modelElems);
      }
    }
  }

  private static DrawableProcessNode parseNode(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlPullParserException, IOException {
    if (!NS_PROCESSMODEL.equals(in.getNamespace())) {
      throw new IllegalArgumentException("Invalid process model");
    }
    if ("start".equals(in.getName())) {
      return parseStart(in, nodes, modelElems);
    } else if ("activity".equals(in.getName())) {
      return parseActivity(in, nodes, modelElems);
    } else if ("split".equals(in.getName())) {
      return parseSplit(in, nodes, modelElems);
    } else if ("join".equals(in.getName())) {
      return parseJoin(in, nodes, modelElems);
    } else if ("end".equals(in.getName())) {
      return parseEnd(in, nodes, modelElems);
    }
    throw new UnsupportedOperationException("Unsupported tag");
  }

  private static DrawableProcessNode parseStart(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlPullParserException, IOException {
    DrawableStartNode result = new DrawableStartNode();
    parseCommon(in, nodes, modelElems, result);
    if (in.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static DrawableProcessNode parseActivity(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlPullParserException, IOException {
    DrawableActivity result = new DrawableActivity();
    parseCommon(in, nodes, modelElems, result);
    String name = trimWS(in.getAttributeValue(XMLConstants.NULL_NS_URI, "name"));
    if (name!=null && name.length()>0) {
      result.setName(name);
    }
    for(int type = in.next(); type!=END_TAG; type = in.next()) {
      switch (type) {
      case START_TAG:
        if (NS_PROCESSMODEL.equals(in.getNamespace())) {
          if ("message".equals(in.getName())) {
            result.setMessage(parseMessage(in));
          } else {
            parseUnknownTag(in);
          }
        } else {
          parseUnknownTag(in);
        }
        break;
      default:
          // ignore
      }
    }
    return result;
  }

  private static IXmlMessage parseMessage(XmlPullParser in) {
    ClientMessage result = new ClientMessage();
    String endpoint = in.getAttributeValue(XMLConstants.NULL_NS_URI, "endpoint");
    String operation = in.getAttributeValue(XMLConstants.NULL_NS_URI, "operation");
    String url = in.getAttributeValue(XMLConstants.NULL_NS_URI, "url");
    String method = in.getAttributeValue(XMLConstants.NULL_NS_URI, "method");
    String type = in.getAttributeValue(XMLConstants.NULL_NS_URI, "type");
    String serviceNS = in.getAttributeValue(XMLConstants.NULL_NS_URI, "serviceNS");
    String serviceName = in.getAttributeValue(XMLConstants.NULL_NS_URI, "serviceName");
    result.setEndpoint(endpoint);
    result.setOperation(operation);
    result.setUrl(url);
    result.setMethod(method);
    result.setType(type);
    if(serviceName!=null) {
      result.setServiceNS(serviceNS);
      result.setServiceName(serviceName);
    }

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc;
    try {
      doc = dbf.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    int tagtype;
    try {
      while ((tagtype=in.next())!=END_TAG) {
        switch (tagtype) {
          case START_TAG: {
            Node node = parseXmlTag(doc, in);
            if (doc.getDocumentElement()!=null) {
              doc.replaceChild(node, doc.getDocumentElement());
            } else {
              doc.appendChild(node);
            }
            break;
          }
          default: {
            // ignore text, it
          }
        }
      }
    } catch (DOMException | XmlPullParserException | IOException e) {
      Log.e(PMParser.class.getSimpleName(), "Error parsing activity body", e);
    }
    // TODO fix this as it does not do fragments properly
    result.setMessageBody(new DOMSource(doc.getDocumentElement()));
    return result;
  }

  private static Element parseXmlTag(Document doc, XmlPullParser in) throws IOException, XmlPullParserException {
    Element element=doc.createElementNS(in.getNamespace(), in.getName());
    element.setPrefix(in.getPrefix());
    for(int i=0; i<in.getAttributeCount(); ++i) {
      Attr a = doc.createAttributeNS(in.getAttributeNamespace(i), in.getAttributeName(i));
      a.setPrefix(in.getAttributePrefix(i));
      a.setNodeValue(in.getAttributeValue(i));
      element.setAttributeNode(a);
    }
    int type;
    while ((type=in.next())!=END_TAG) {
      switch (type) {
        case START_TAG: {
          Element e = parseXmlTag(doc, in);
          element.appendChild(e);
          break;
        }
        case TEXT: {
          Text text = doc.createTextNode(in.getText());
          element.appendChild(text);
          break;
        }
      }
    }
    return element;
  }

  private static QName toQName(XmlPullParser in, String value) {
    if (value==null) { return null; }
    int i = value.indexOf(':');
    if (i>0) {
      String prefix = value.substring(0, i);
      String namespace = in.getNamespace(prefix);
      String localname = value.substring(i+1);
      return new QName(namespace, localname, prefix);
    } else {
      String namespace = in.getNamespace("");
      return new QName(namespace, value);
    }
  }

  private static void parseUnknownTag(XmlPullParser in) throws XmlPullParserException, IOException {
    for(int type = in.next(); type!=END_TAG; type = in.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(in);
        break;
      default:
          // ignore
      }
    }
  }

  private static DrawableProcessNode parseJoin(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlPullParserException, IOException {
    DrawableJoin result = new DrawableJoin();
    parseCommon(in, nodes, modelElems, result);
    parseJoinSplitAttrs(in, result);
    List<Identifiable> predecessors = new ArrayList<>();

    for(int type = in.nextTag(); type!=END_TAG; type = in.nextTag()) {
      if (! (NS_PROCESSMODEL.equals(in.getNamespace()) && "predecessor".equals(in.getName()))) {
        throw new IllegalArgumentException("Invalid process model");
      }
      StringBuilder name = new StringBuilder();
      type = in.next();
      while (type!=END_TAG) {
        if (type==TEXT) {
          name.append(in.getText());
        } else if (type==START_TAG) {
          throw new IllegalArgumentException("Invalid process model");
        }
        type=in.next();
      }
      predecessors.add(getPredecessor(trimWS(name), nodes, modelElems));
    }
    result.setPredecessors(predecessors);

    return result;
  }

  private static String trimWS(CharSequence str) {
    if (str==null) { return null; }
    int start, end;
    for(start=0;start<str.length()&&isXMLWS(str.charAt(start));++start) {/*no body*/}
    for(end=str.length()-1;end>=start&& isXMLWS(str.charAt(end));--end) {/*no body*/}
    return str.subSequence(start, end+1).toString();
  }

  private static boolean isXMLWS(int codepoint) {
    return codepoint==0x20|codepoint==0x9||codepoint==0xD||codepoint==0xA;
  }

  private static DrawableProcessNode parseSplit(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlPullParserException, IOException {
    DrawableSplit result = new DrawableSplit();
    parseCommon(in, nodes, modelElems, result);
    parseJoinSplitAttrs(in, result);
    for(int type = in.next(); type!=END_TAG; type = in.next()) {
      switch (type) {
      case START_TAG:
        parseUnknownTag(in);
        break;
      default:
          // ignore
      }
    }

    return result;
  }

  private static void parseJoinSplitAttrs(XmlPullParser in, DrawableJoinSplit node) {
    for(int i=0; i< in.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(in.getAttributeNamespace(i))) {
        final String aname = in.getAttributeName(i);
        if ("min".equals(aname)) {
          node.setMin(Integer.parseInt(in.getAttributeValue(i)));
        } else if ("max".equals(aname)) {
          node.setMax(Integer.parseInt(in.getAttributeValue(i)));
        }
      }
    }
  }

  private static DrawableProcessNode parseEnd(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) throws XmlPullParserException, IOException {
    DrawableEndNode result = new DrawableEndNode();
    parseCommon(in, nodes, modelElems, result);
    if (in.nextTag()!=END_TAG) { throw new IllegalArgumentException("Invalid process model"); }
    return result;
  }

  private static void parseCommon(XmlPullParser in, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems, DrawableProcessNode node) {
    for(int i=0; i< in.getAttributeCount();++i) {
      if (XMLConstants.NULL_NS_URI.equals(in.getAttributeNamespace(i))) {
        final String aname = in.getAttributeName(i);
        if ("x".equals(aname)) {
          node.setX(Double.parseDouble(in.getAttributeValue(i)));
        } else if ("y".equals(aname)) {
          node.setY(Double.parseDouble(in.getAttributeValue(i)));
        } else if ("id".equals(aname)) {
          node.setId(trimWS(in.getAttributeValue(i)));
        } else if ("label".equals(aname)) {
          node.setLabel(in.getAttributeValue(i));
        } else if ("name".equals(aname)) {
          if (node.getLabel()==null) {
            node.setLabel(in.getAttributeValue(i));
          }
        } else if ("predecessor".equals(aname)) {
          addPredecessor(node, trimWS(in.getAttributeValue(i)), nodes, modelElems);
//          pNode.setPredecessors(getPredecessors(pIn.getAttributeValue(i),pNodes, pModelElems));
        }
      }
    }
  }

  private static void addPredecessor(DrawableProcessNode node, String name, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) {
    Identifiable predecessor = getPredecessor(name, nodes, modelElems);
    if (predecessor instanceof DrawableProcessNode) {
      addAsSuccessor((DrawableProcessNode) predecessor, node, modelElems);
    }
  }

  private static Identifiable getPredecessor(String name, Map<String, DrawableProcessNode> nodes, List<DrawableProcessNode> modelElems) {
    Identifiable val = nodes.get(name);
    if (val==null) {
      val = new Identifier(name);
    } else { // there already is a node
      // Allow temporary references to collect as many successors as desired, it might be a split.
      if ((!(val instanceof DrawableProcessNode))|| (((DrawableProcessNode)val).getSuccessors().size()<((DrawableProcessNode)val).getMaxSuccessorCount())) {
        return val;
      } else {
        // There is no suitable successor
        return introduceSplit((DrawableProcessNode)val, modelElems);
      }
    }
    return val;
  }

  private static DrawableSplit introduceSplit(DrawableProcessNode predecessor, List<DrawableProcessNode> modelElems) {
    for(DrawableProcessNode successor:predecessor.getSuccessors()) {
      if (successor instanceof DrawableSplit) {
        return (DrawableSplit) successor;
      }
    }

    DrawableSplit newSplit = new DrawableSplit();
    ArrayList<DrawableProcessNode> successors = new ArrayList<>(predecessor.getSuccessors());
    for(DrawableProcessNode successor: successors) {
      predecessor.removeSuccessor(successor);
      successor.removePredecessor(predecessor);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }
    predecessor.addSuccessor(newSplit);
    newSplit.addPredecessor(predecessor);
    modelElems.add(newSplit);
    return newSplit;
  }

  private static void addAsSuccessor(DrawableProcessNode predecessor, DrawableProcessNode successor, List<DrawableProcessNode> modelElems) {
    if (predecessor.getSuccessors().size()<predecessor.getMaxSuccessorCount()) {
      predecessor.addSuccessor(successor);
      successor.addPredecessor(predecessor);
    } else {
      DrawableSplit newSplit = introduceSplit(predecessor, modelElems);
      newSplit.addSuccessor(successor);
      successor.addPredecessor(newSplit);
    }

  }

}
