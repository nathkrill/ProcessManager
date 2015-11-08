package nl.adaptivity.process.engine.processModel;

import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.process.engine.PETransformer.AbstractDataContext;
import nl.adaptivity.process.engine.ProcessData;
import nl.adaptivity.process.processModel.IXmlResultType;
import nl.adaptivity.process.processModel.XmlDefineType;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.CompactFragment;
import nl.adaptivity.util.xml.Namespace;
import nl.adaptivity.util.xml.SimpleNamespaceContext;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import java.util.Collections;
import java.util.List;

public class ProcessNodeInstanceContext extends AbstractDataContext {

  private static final CompactFragment EMPTY_FRAGMENT = new CompactFragment(Collections.<Namespace>emptyList(), new char[0]);
  private final ProcessNodeInstance mProcessNodeInstance;
  private List<ProcessData> mDefines;
  private boolean mProvideResults;

  public ProcessNodeInstanceContext(ProcessNodeInstance pProcessNodeInstance, List<ProcessData> pDefines, boolean pProvideResults) {
    mProcessNodeInstance = pProcessNodeInstance;
    mDefines = pDefines;
    mProvideResults = pProvideResults;
  }

  @Override
  protected ProcessData getData(String pValueName) {
    switch (pValueName) {
      case "handle": return new ProcessData(pValueName, new CompactFragment(Long.toString(mProcessNodeInstance.getHandle())));
      case "instancehandle": return new ProcessData(pValueName, new CompactFragment(Long.toString(mProcessNodeInstance.getProcessInstance().getHandle())));
      case "endpoint": return new ProcessData(pValueName, createEndpoint());
      case "owner": return new ProcessData(pValueName, new CompactFragment(mProcessNodeInstance.getProcessInstance().getOwner().getName()));
    }

    for(ProcessData define: mDefines) {
      if (pValueName.equals(define.getName())) {
        return define;
      }
    }

    if (mProvideResults) {
      for(ProcessData result: mProcessNodeInstance.getResults()) {
        if (pValueName.equals(result.getName())) {
          return result;
        }
      }
    }
    // allow for missing values in the database. If they were "defined" treat is as an empty value.
    for(XmlDefineType resultDef: mProcessNodeInstance.getNode().getDefines()) {
      if (pValueName.equals(resultDef.getName())) {
        return new ProcessData(pValueName, EMPTY_FRAGMENT);
      }
    }
    if (mProvideResults) {
      // allow for missing values in the database. If they were "defined" treat is as an empty value.
      for(IXmlResultType resultDef: mProcessNodeInstance.getNode().getResults()) {
        if (pValueName.equals(resultDef.getName())) {
          return new ProcessData(pValueName, EMPTY_FRAGMENT);
        }
      }
    }
    return null;
  }

  private CompactFragment createEndpoint() {
    SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Collections.singletonMap("jbi", Constants.MY_JBI_NS));
    StringBuilder content = new StringBuilder();
    content.append("<jbi:endpointDescriptor");

    EndpointDescriptor localEndpoint = mProcessNodeInstance.getProcessInstance().getEngine().getLocalEndpoint();
    content.append(" endpointLocation=\"").append(localEndpoint.getEndpointLocation().toString()).append('"');
    content.append(" endpointName=\"").append(localEndpoint.getEndpointName()).append('"');
    content.append(" serviceLocalName=\"").append(localEndpoint.getServiceName().getLocalPart()).append('"');
    content.append(" serviceNS=\"").append(localEndpoint.getServiceName().getNamespaceURI()).append('"');
    content.append(" />");
    return new CompactFragment(namespaces, content.toString().toCharArray());
  }

  @Override
  public List<XMLEvent> resolveDefaultValue(XMLEventFactory pXef) throws XMLStreamException {
    throw new UnsupportedOperationException("There is no default in this context");
  }

}