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

import net.devrieze.util.Transaction;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.messaging.GenericEndpoint;
import nl.adaptivity.process.util.Constants;
import nl.adaptivity.rest.annotations.RestMethod;
import nl.adaptivity.rest.annotations.RestMethod.HttpMethod;
import nl.adaptivity.rest.annotations.RestParam;
import nl.adaptivity.rest.annotations.RestParam.ParamType;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletConfig;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;


@XmlSeeAlso(XmlTask.class)
public class ExternalEndpoint implements GenericEndpoint {

  public static final String ENDPOINT = "external";

  public static final String SERVICE_LOCALNAME = "userMessageHandler";
  public static final QName SERVICENAME = new QName(Constants.USER_MESSAGE_HANDLER_NS, SERVICE_LOCALNAME);

  private final UserMessageService<?> mService;

  private URI mURI;

  public ExternalEndpoint() {
    this(UserMessageService.getInstance());
  }

  public ExternalEndpoint(@NotNull UserMessageService<?> service) {
    mService = service;
  }

  @Override
  public QName getServiceName() {
    return SERVICENAME;
  }

  @Override
  public String getEndpointName() {
    return ENDPOINT;
  }

  @Override
  public URI getEndpointLocation() {
    return mURI;
  }

  @Override
  public boolean isSameService(final EndpointDescriptor other) {
    return Constants.USER_MESSAGE_HANDLER_NS.equals(other.getServiceName().getNamespaceURI()) &&
           SERVICE_LOCALNAME.equals(other.getServiceName().getLocalPart()) &&
           getEndpointName().equals(other.getEndpointName());
  }

  @Override
  public void initEndpoint(final ServletConfig config) {
    final StringBuilder path = new StringBuilder(config.getServletContext().getContextPath());
    path.append("/UserMessageService");
    try {
      mURI = new URI(null, null, path.toString(), null);
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e); // Should never happen
    }
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

  @XmlElementWrapper(name = "tasks", namespace = Constants.USER_MESSAGE_HANDLER_NS)
  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks")
  public Collection<XmlTask> getPendingTasks() throws SQLException {
    return getPendingTasks(mService);
  }

  /**
   * Helper method that is generic that can record the "right" transaction type.
   */
  private static <T extends Transaction> Collection<XmlTask> getPendingTasks(UserMessageService<T> service) throws SQLException {
    try (T transaction = service.newTransaction()) {
      return transaction.commit(service.getPendingTasks(transaction));
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error retrieving tasks", e);
      throw e;
    }
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}")
  public XmlTask updateTask(
      @RestParam(name="handle", type=ParamType.VAR) final String handle,
      @RestParam(type=ParamType.BODY) final XmlTask partialNewTask,
      @RestParam(type = ParamType.PRINCIPAL) final Principal user) throws SQLException, FileNotFoundException
  {
    return updateTask(mService, handle, partialNewTask, user);
  }

  private static <T extends Transaction> XmlTask updateTask(UserMessageService<T> service, String handle, XmlTask partialNewTask, Principal user) throws SQLException, FileNotFoundException {
    try (T transaction = service.newTransaction()) {
      final XmlTask result = service.updateTask(transaction, Long.parseLong(handle), partialNewTask, user);
      if (result==null) { throw new FileNotFoundException(); }
      transaction.commit();
      return result;
    } catch (Exception e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "Error updating task", e);
      throw e;
    }
  }

  @RestMethod(method = HttpMethod.GET, path = "/pendingTasks/${handle}")
  public XmlTask getPendingTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.getPendingTask(Long.parseLong(handle), user);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Started" })
  public NodeInstanceState startTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.startTask(Long.parseLong(handle), user);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Taken" })
  public NodeInstanceState takeTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.takeTask(Long.parseLong(handle), user);
  }

  @RestMethod(method = HttpMethod.POST, path = "/pendingTasks/${handle}", post = { "state=Finished" })
  public NodeInstanceState finishTask(@RestParam(name = "handle", type = ParamType.VAR) final String handle, @RestParam(type = ParamType.PRINCIPAL) final Principal user) {
    return mService.finishTask(Long.parseLong(handle), user);
  }

  @Override
  public void destroy() {
    mService.destroy();
    MessagingRegistry.getMessenger().registerEndpoint(this);
  }

}
