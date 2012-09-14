package nl.adaptivity.process.userMessageHandler.server;

import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;

import nl.adaptivity.jbi.util.EndPointDescriptor;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.process.exec.Task.TaskState;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.process.messaging.GenericEndpoint;

@XmlSeeAlso(InternalEndpoint.XmlTask.class)
@XmlAccessorType(XmlAccessType.NONE)
public class InternalEndpoint implements GenericEndpoint {

  @XmlRootElement(name="task")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class XmlTask implements UserTask<XmlTask>{
    private static final QName UPDATE_OPERATION_NAME = new QName("http://adaptivity.nl/ProcessEngine/", "updateTaskState");
    private static final QName FINISH_OPERATION_NAME = new QName("http://adaptivity.nl/ProcessEngine/", "finishTask");
    private long aHandle;
    private long aRemoteHandle;
    private TaskState aState=TaskState.Available;
    private String aSummary;
    private EndPointDescriptor aEndPoint = null;
    private AsyncMessenger aContext;

    public XmlTask() {
      aHandle = -1;
      aRemoteHandle = -1;
    }

    public XmlTask(long pHandle) {
      aHandle = pHandle;
    }

    @XmlAttribute
    @Override
    public TaskState getState() {
      return aState;
    }

    @Override
    public void setState(TaskState pNewState) {
      try {
        TaskState newState;
        if (pNewState==TaskState.Complete) {
          newState = finishRemoteTask();
        } else {
          newState = updateRemoteTaskState(pNewState);
        }
        aState = newState;
      } catch (JAXBException e) {
        e.printStackTrace();
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      } catch (MyMessagingException e) {
        e.printStackTrace();
        Logger.getLogger(getClass().getCanonicalName()).throwing("XmlTask", "setState", e);
      }
    }

    private TaskState updateRemoteTaskState(TaskState pState) throws JAXBException, MyMessagingException {
      throw new UnsupportedOperationException("Not implemented");
      /*
      @SuppressWarnings("unchecked") Source messageContent = SoapHelper.createMessage(UPDATE_OPERATION_NAME, Tripple.<String, Class<?>, Object>tripple("handle", long.class, aRemoteHandle), Tripple.<String, Class<?>, Object>tripple("state", TaskState.class, pState));
      DeliveryChannel channel = aContext.getDeliveryChannel();

      ServiceEndpoint se = null;
      {
        for(ServiceEndpoint candidate:aContext.getEndpointsForService(aEndPoint.getServiceName())) {
          if (candidate.getEndpointName().equals(aEndPoint.getEndpointName())) {
            se = candidate;
            break;
          }
        }
      }
      if (se==null) { throw new MessagingException("No endpoint found"); }

      MessageExchangeFactory exf = channel.createExchangeFactory(se);
      InOut ex = exf.createInOutExchange();
      ex.setOperation(UPDATE_OPERATION_NAME);
      NormalizedMessage message = ex.createMessage();
      message.setContent(messageContent);
      ex.setInMessage(message);
      if (channel.sendSync(ex)) {
        NormalizedMessage result = ex.getOutMessage();
        if (result!=null) {
          TaskState newState = SoapHelper.processResponse(TaskState.class, result.getContent());
          aState = newState;
          return newState;
        }
      }
      return aState; // Don't change state
      */
    }

    private TaskState finishRemoteTask() throws JAXBException, MyMessagingException {
      throw new UnsupportedOperationException("Not implemented");
      /*
      @SuppressWarnings("unchecked") Source messageContent = SoapHelper.createMessage(FINISH_OPERATION_NAME, Tripple.<String, Class<?>, Object>tripple("handle", long.class, aRemoteHandle), Tripple.<String, Class<?>, Object>tripple("payload", Node.class, null));
      DeliveryChannel channel = aContext.getDeliveryChannel();

      ServiceEndpoint se = null;
      {
        for(ServiceEndpoint candidate:aContext.getEndpointsForService(aEndPoint.getServiceName())) {
          if (candidate.getEndpointName().equals(aEndPoint.getEndpointName())) {
            se = candidate;
            break;
          }
        }
      }
      if (se==null) { throw new MessagingException("No endpoint found"); }

      MessageExchangeFactory exf = channel.createExchangeFactory(se);
      InOut ex = exf.createInOutExchange();
      ex.setOperation(FINISH_OPERATION_NAME);
      NormalizedMessage message = ex.createMessage();
      message.setContent(messageContent);
      ex.setInMessage(message);
      // TODO Perhaps use async communication for this
      if (channel.sendSync(ex)) {
        NormalizedMessage result = ex.getOutMessage();
        if (result!=null) {
          TaskState newState = SoapHelper.processResponse(TaskState.class, result.getContent());
          aState = newState;
          return newState;
        }
      }
      return aState; // Don't change state
      */
    }

    @XmlAttribute(name="handle")
    @Override
    public void setHandle(long pHandle) {
      aHandle = pHandle;
    }

    @Override
    public long getHandle() {
      return aHandle;
    }

    @XmlAttribute(name="remotehandle")
    public void setRemoteHandle(long pHandle) {
      aRemoteHandle = pHandle;
    }

    public long getRemoteHandle() {
      return aRemoteHandle;
    }

    @XmlAttribute(name="summary")
    public String getSummary() {
      return aSummary;
    }

    public void setSummary(String pSummary) {
      aSummary = pSummary;
    }

    /** Set the endpoint that is used for updating the task state */
    @Override
    public void setEndpoint(EndPointDescriptor pEndPoint) {
      aEndPoint = pEndPoint;
    }

    @Override
    public void setContext(AsyncMessenger context) {
      aContext = context;
    }

    @Override
    public AsyncMessenger getContext() {
      return aContext;
    }
  }

  private static final String ENDPOINT = "internal";
  public static final QName SERVICENAME = new QName(UserMessageService.UMH_NS, "userMessageHandler");
  private UserMessageService aService;

  public InternalEndpoint() {
    aService = UserMessageService.getInstance();
  }

  @Override
  public QName getService() {
    return SERVICENAME;
  }

  @Override
  public String getEndpoint() {
    return ENDPOINT;
  }

  @WebMethod
  public boolean postTask(@WebParam(name="replies", mode=Mode.IN) EndPointDescriptor pEndPoint, @WebParam(name="task", mode=Mode.IN) UserTask<?> pTask) {
    pTask.setEndpoint(pEndPoint);
    return aService.postTask(pTask);
  }
}
