package nl.adaptivity.process;

import nl.adaptivity.process.exec.Task;
import nl.adaptivity.process.processModel.XmlMessage;


/**
 * Interface signifying that the object can be used to send messages.
 * @author Paul de Vrieze
 *
 * @param <T> The type signifying a message that can then be sent.
 * @param <U> The task that the message corresponds to. This allows for messages to be linked to tasks.
 */
public interface IMessageService<T,U extends Task<U>> {

  /**
   * Create a message.
   * @param pMessage The message to create (for later sending)
   * @return The sendable message that can be sent.
   */
  T createMessage(XmlMessage pMessage);

  /**
   * Send a message.
   * @param pMessage The message to send. (Created by {@link #createMessage(XmlMessage)}).
   * @param pInstance The task instance to link the sending to.
   * @return <code>true</code> on success, <code>false</code> on failure.
   */
  boolean sendMessage(T pMessage, U pInstance);
}
