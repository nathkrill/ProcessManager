package nl.adaptivity.process.engine;

import java.io.Serializable;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.devrieze.util.HandleMap.Handle;
import net.devrieze.util.HandleMap.HandleAware;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.security.SecureObject;
import net.devrieze.util.security.SecurityProvider;
import nl.adaptivity.process.IMessageService;
import nl.adaptivity.process.engine.processModel.JoinInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.processModel.EndNode;
import nl.adaptivity.process.processModel.engine.JoinImpl;
import nl.adaptivity.process.processModel.engine.ProcessModelImpl;
import nl.adaptivity.process.processModel.engine.ProcessNodeImpl;
import nl.adaptivity.process.processModel.engine.StartNodeImpl;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.util.xml.XmlSerializable;

import org.w3c.dom.Node;


public class ProcessInstance implements Serializable, HandleAware<ProcessInstance>, SecureObject, XmlSerializable {

  public enum State {
    INITIALIZED,
    STARTED,
    FINISHED,
    FAILED,
    CANCELLED;
  }

  @XmlRootElement(name = "processInstance", namespace = Constants.PROCESS_ENGINE_NS)
  @XmlAccessorType(XmlAccessType.NONE)
  public static class ProcessInstanceRef implements Handle<ProcessInstance> {

    private long aHandle;

    private long aProcessModel;

    private String aName;

    public ProcessInstanceRef() {
      // empty constructor;
    }

    public ProcessInstanceRef(final ProcessInstance pProcessInstance) {
      setHandle(pProcessInstance.getHandle());
      setProcessModel(pProcessInstance.aProcessModel.getHandle());
      aName = pProcessInstance.getName();
      if ((aName == null) || (aName.trim().length() == 0)) {
        aName = pProcessInstance.aProcessModel.getName() + " instance " + aHandle;
      }
    }

    public void setHandle(final long handle) {
      aHandle = handle;
    }

    @Override
    @XmlAttribute(name = "handle")
    public long getHandle() {
      return aHandle;
    }

    public void setProcessModel(final long processModel) {
      aProcessModel = processModel;
    }

    @XmlAttribute(name = "processModel")
    public long getProcessModel() {
      return aProcessModel;
    }

    @XmlAttribute(name = "name")
    public String getName() {
      return aName;
    }

    public void setName(final String pName) {
      aName = pName;
    }

  }

  private static final long serialVersionUID = 1145452195455018306L;

  private final ProcessModelImpl aProcessModel;

  private final Collection<ProcessNodeInstance> aThreads;

  private final Collection<ProcessNodeInstance> aFinishedNodes;

  private final Collection<ProcessNodeInstance> aEndResults;

  private HashMap<JoinImpl, JoinInstance> aJoins;

  private long aHandle;

  private final ProcessEngine aEngine;

  private List<ProcessData> aInputs = new ArrayList<>();

  private List<ProcessData> aOutputs = new ArrayList<>();

  private final String aName;

  private final Principal aOwner;

  private State aState;

  ProcessInstance(final long pHandle, final Principal pOwner, final ProcessModelImpl pProcessModel, final String pName, final State pState, final ProcessEngine pEngine) {
    aHandle = pHandle;
    aProcessModel = pProcessModel;
    aOwner = pOwner;
    aEngine = pEngine;
    aName =pName;
    aState = pState;
    aThreads = new LinkedList<>();
    aJoins = new HashMap<>();
    aEndResults = new ArrayList<>();
    aFinishedNodes = new ArrayList<>();
  }

  public ProcessInstance(final Principal pOwner, final ProcessModelImpl pProcessModel, final String pName, final State pState, final ProcessEngine pEngine) {
    aProcessModel = pProcessModel;
    aName = pName;
    aEngine = pEngine;
    aThreads = new LinkedList<>();
    aOwner = pOwner;
    aJoins = new HashMap<>();
    aEndResults = new ArrayList<>();
    aFinishedNodes = new ArrayList<>();
    aState = pState;
  }

  void setThreads(DBTransaction pTransaction, final Collection<? extends Handle<? extends ProcessNodeInstance>> pThreads) throws SQLException {
    for(Handle<? extends ProcessNodeInstance> thread:pThreads) {
      aThreads.add(aEngine.getNodeInstance(pTransaction, thread.getHandle(), SecurityProvider.SYSTEMPRINCIPAL));
    }
  }

  public void initialize(DBTransaction pTransaction) throws SQLException {
    if (aState!=null || aThreads.size()>0) {
      throw new IllegalStateException("The instance already appears to be initialised");
    }
    for (final StartNodeImpl node : aProcessModel.getStartNodes()) {
      final ProcessNodeInstance instance = new ProcessNodeInstance(node, null, this);
      aEngine.registerNodeInstance(pTransaction, instance);
      aThreads.add(instance);
    }
    aState = State.INITIALIZED;
    aEngine.updateStorage(pTransaction, this);
  }

  public synchronized void finish(DBTransaction pTransaction) throws SQLException {
    int aFinished = getFinishedCount();
    if (aFinished >= aProcessModel.getEndNodeCount()) {
      aEngine.finishInstance(pTransaction, this);
    }
  }

  private int getFinishedCount() {
    return aEndResults.size();
  }

  public synchronized JoinInstance getJoinInstance(DBTransaction pTransaction, final JoinImpl pJoin, final ProcessNodeInstance pPredecessor) throws SQLException {
    JoinInstance result = aJoins.get(pJoin);
    if (result == null) {
      final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(pJoin.getPredecessors().size());
      predecessors.add(pPredecessor);
      result = new JoinInstance(pTransaction, pJoin, predecessors, this);
      aJoins.put(pJoin, result);
    } else {
      result.addPredecessor(pTransaction, pPredecessor);
    }
    return result;
  }

  public synchronized void removeJoin(final JoinInstance pJ) {
    aJoins.remove(pJ.getNode());
  }

  @Override
  public long getHandle() {
    return aHandle;
  }

  @Override
  public void setHandle(final long pHandle) {

    aHandle = pHandle;
  }

  public String getName() {
    return aName;
  }

  public Principal getOwner() {
    return aOwner;
  }

  public ProcessEngine getEngine() {
    return aEngine;
  }

  public ProcessInstanceRef getRef() {
    return new ProcessInstanceRef(this);
  }

  public ProcessModelImpl getProcessModel() {
    return aProcessModel;
  }

  /**
   * Get the payload that was passed to start the instance.
   * @return The process initial payload.
   */
  public List<ProcessData> getInputs() {
    return aInputs;
  }

  public synchronized State getState() {
    return aState;
  }

  public synchronized void start(DBTransaction pTransaction, final IMessageService<?, ProcessNodeInstance> pMessageService, final Node pPayload) throws SQLException {
    if (aState==null) {
      initialize(pTransaction);
    }
    if (aThreads.size() == 0) {
      throw new IllegalStateException("No starting nodes in process");
    }
    aInputs = aProcessModel.toInputs(pPayload);
    for (final ProcessNodeInstance node : aThreads) {
      provideTask(pTransaction, pMessageService, node);
    }
  }

  /** Method called when the instance is loaded from the server. This should reinitialise the instance. */
  public void reinitialize() {
    // TODO Auto-generated method stub

  }

  public synchronized void provideTask(DBTransaction pTransaction, final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) throws SQLException {
    if (pNode.provideTask(pTransaction, pMessageService)) {
      takeTask(pTransaction, pMessageService, pNode);
    }
  }

  public synchronized void takeTask(DBTransaction pTransaction, final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) throws SQLException {
    if (pNode.takeTask(pTransaction, pMessageService)) {
      startTask(pTransaction, pMessageService, pNode);
    }
  }

  public synchronized void startTask(DBTransaction pTransaction, final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) throws SQLException {
    if (pNode.startTask(pTransaction, pMessageService)) {
      finishTask(pTransaction, pMessageService, pNode, null);
    }
  }

  public synchronized void finishTask(DBTransaction pTransaction, final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode, final Node pResultPayload) throws SQLException {
    pNode.finishTask(pTransaction, pResultPayload);
    // TODO ensure that finishing a task is always committed.
    if (pNode.getNode() instanceof EndNode) {
      aEndResults.add(pNode);
      aThreads.remove(pNode);
      finish(pTransaction);
    } else {
      aFinishedNodes.add(pNode);
      aThreads.remove(pNode);
      final List<ProcessNodeInstance> startedTasks = new ArrayList<>(pNode.getNode().getSuccessors().size());
      for (final ProcessNodeImpl successorNode : pNode.getNode().getSuccessors()) {
        final ProcessNodeInstance instance = getProcessNodeInstance(pTransaction, pNode, successorNode);
        aThreads.add(instance);
        startedTasks.add(instance);
        aEngine.registerNodeInstance(pTransaction, instance);
      }
      for (final ProcessNodeInstance task : startedTasks) {
        provideTask(pTransaction, pMessageService, task);
      }
    }
  }

  private ProcessNodeInstance getProcessNodeInstance(DBTransaction pTransaction, final ProcessNodeInstance pPredecessor, final ProcessNodeImpl pNode) throws SQLException {
    if (pNode instanceof JoinImpl) {
      final JoinImpl join = (JoinImpl) pNode;
      if (aJoins == null) {
        aJoins = new HashMap<>();
      }
      JoinInstance instance = aJoins.get(join);
      if (instance == null) {

        final Collection<Handle<? extends ProcessNodeInstance>> predecessors = new ArrayList<>(pNode.getPredecessors().size());
        predecessors.add(pPredecessor);
        instance = new JoinInstance(pTransaction, join, predecessors, this);
        aJoins.put(join, instance);
      } else {
        instance.addPredecessor(pTransaction, pPredecessor);
      }
      return instance;

    } else {
      return new ProcessNodeInstance(pNode, pPredecessor, this);
    }
  }

  public synchronized void failTask(DBTransaction pTransaction, @SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode, final Throwable pCause) throws SQLException {
    pNode.failTask(pTransaction, pCause);
  }

  public synchronized void cancelTask(DBTransaction pTransaction, @SuppressWarnings("unused") final IMessageService<?, ProcessNodeInstance> pMessageService, final ProcessNodeInstance pNode) throws SQLException {
    pNode.cancelTask(pTransaction);
  }

  public synchronized Collection<ProcessNodeInstance> getActivePredecessorsFor(final JoinImpl pJoin) {
    final ArrayList<ProcessNodeInstance> activePredecesors = new ArrayList<>(Math.min(pJoin.getPredecessors().size(), aThreads.size()));
    for (final ProcessNodeInstance node : aThreads) {
      if (node.getNode().isPredecessorOf(pJoin)) {
        activePredecesors.add(node);
      }
    }
    return activePredecesors;
  }

  public synchronized Collection<? extends Handle<? extends ProcessNodeInstance>> getDirectSuccessors(DBTransaction pTransaction, final ProcessNodeInstance pPredecessor) throws SQLException {
    final ArrayList<Handle<? extends ProcessNodeInstance>> result = new ArrayList<>(pPredecessor.getNode().getSuccessors().size());
    for (final ProcessNodeInstance candidate : aThreads) {
      addDirectSuccessor(pTransaction, result, candidate, pPredecessor);
    }
    return result;
  }

  private void addDirectSuccessor(DBTransaction pTransaction, final ArrayList<Handle<? extends ProcessNodeInstance>> pResult, ProcessNodeInstance pCandidate, final Handle<? extends ProcessNodeInstance> pPredecessor) throws SQLException {
    // First look for this node, before diving into it's children
    for (final Handle<? extends ProcessNodeInstance> node : pCandidate.getDirectPredecessors()) {
      if (node.getHandle() == pPredecessor.getHandle()) {
        pResult.add(pCandidate);
        return; // Assume that there is no further "successor" down the chain
      }
    }
    for (final Handle<? extends ProcessNodeInstance> hnode : pCandidate.getDirectPredecessors()) {
      // Use the fact that we start with a proper node to get the engine and get the actual node based on the handle (which might be a node itself)
      ProcessNodeInstance node = pCandidate.getProcessInstance().getEngine().getNodeInstance(pTransaction, hnode.getHandle(), SecurityProvider.SYSTEMPRINCIPAL);
      addDirectSuccessor(pTransaction, pResult, node, pPredecessor);
    }
  }

  @Override
  public void serialize(XMLStreamWriter pOut) throws XMLStreamException {
    //
    if(pOut.getPrefix(Constants.PROCESS_ENGINE_NS)==null) {
      pOut.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, Constants.PROCESS_ENGINE_NS);
    }
    pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "processInstance");
    try {
      pOut.writeAttribute("handle", Long.toString(aHandle));
      pOut.writeAttribute("name", aName);
      pOut.writeAttribute("processModel", Long.toString(getProcessModel().getHandle()));
      pOut.writeAttribute("owner", aOwner.getName());

      pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "inputs");
      try {
        for(ProcessData input:aInputs) {
          input.serialize(pOut);
        }
      } finally {
        pOut.writeEndElement();
      }

      pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "outputs");
      try {
        for(ProcessData output:aOutputs) {
          output.serialize(pOut);
        }
      } finally {
        pOut.writeEndElement();
      }

      if (aThreads.size()>0) {
        try {
          pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "active");
        } finally {
          for(ProcessNodeInstance active: aThreads) {
            writeActiveNodeRef(pOut, active);
          }
          pOut.writeEndElement();
        }
      }
      if (aThreads.size()>0) {
        try {
          pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "finished");
        } finally {
          for(ProcessNodeInstance active: aFinishedNodes) {
            writeActiveNodeRef(pOut, active);
          }
          pOut.writeEndElement();
        }
      }
      if (aEndResults.size()>0) {
        try {
          pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "endresults");
          for(ProcessNodeInstance result: aEndResults) {
            writeResultNodeRef(pOut, result);
          }
        } finally {
          pOut.writeEndElement();
        }
      }
    } finally {
      pOut.writeEndElement();
    }

  }

  private static void writeActiveNodeRef(XMLStreamWriter pOut, ProcessNodeInstance pNodeInstance) throws XMLStreamException {
    pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "nodeinstance");
    try {
      writeNodeRefCommon(pOut, pNodeInstance);
    } finally{
      pOut.writeEndElement();
    }
  }

  private static void writeResultNodeRef(XMLStreamWriter pOut, ProcessNodeInstance pNodeInstance) throws XMLStreamException {
    pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "nodeinstance");
    try {
      writeNodeRefCommon(pOut, pNodeInstance);
      pOut.writeStartElement(Constants.PROCESS_ENGINE_NS, "results");
      try {
        List<ProcessData> results = pNodeInstance.getResult();
        for(ProcessData result:results) {
          result.serialize(pOut);
        }
      } finally {
        pOut.writeEndElement();
      }
    } finally{
      pOut.writeEndElement();
    }
  }

  private static void writeNodeRefCommon(XMLStreamWriter pOut, ProcessNodeInstance pNodeInstance) throws XMLStreamException {
    pOut.writeAttribute("nodeid", pNodeInstance.getNode().getId());
    pOut.writeAttribute("handle", Long.toString(pNodeInstance.getHandle()));
    pOut.writeAttribute("state", pNodeInstance.getState().toString());
    if (pNodeInstance.getState()==TaskState.Failed) {
      final Throwable failureCause = pNodeInstance.getFailureCause();
      pOut.writeAttribute("failureCause", failureCause.getClass().getName()+": "+failureCause.getMessage());
    }

  }

  void setInputs(List<ProcessData> pInputs) {
    aInputs.clear();
    aInputs.addAll(pInputs);
  }

  public void setOutputs(List<ProcessData> pOutputs) {
    aOutputs.clear();
    aOutputs.addAll(pOutputs);
  }

}
