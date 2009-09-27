package nl.adaptivity.process.exec;

import javax.xml.bind.annotation.XmlRootElement;

import net.devrieze.util.HandleMap.HandleAware;

import nl.adaptivity.process.IMessageService;

public interface Task extends HandleAware<Task>{

  @XmlRootElement(name="taskState", namespace="http://adaptivity.nl/userMessageHandler")
  public static enum TaskState {
    Available,
    Taken,
    Started,
    Complete,
    Failed;
  }

  public TaskState getState();

  public void setState(TaskState aNewState);

  public <T> boolean provideTask(IMessageService<T> pMessageService);

  public <T> boolean takeTask(IMessageService<T> pMessageService);

  public <T> boolean startTask(IMessageService<T> pMessageService);

  public void finishTask(Object pPayload);

  public void failTask();

}
