/*
 * Generated by MessagingSoapClientGenerator.
 * Source class: nl.adaptivity.process.engine.servlet.ServletProcessEngine
 */

package nl.adaptivity.process.client;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import net.devrieze.util.Tripple;

import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.messaging.Endpoint;
import nl.adaptivity.messaging.EndpointDescriptor;
import nl.adaptivity.messaging.EndpointDescriptorImpl;
import nl.adaptivity.messaging.MessagingRegistry;
import nl.adaptivity.messaging.SendableSoapSource;
import nl.adaptivity.process.exec.IProcessNodeInstance.TaskState;
import nl.adaptivity.process.exec.XmlProcessNodeInstance;
import nl.adaptivity.ws.soap.SoapHelper;

import org.w3c.dom.Node;

@SuppressWarnings("all")
public class ServletProcessEngineClient {

  private static final QName SERVICE = new QName("http://adaptivity.nl/ProcessEngine/", "ProcessEngine", "");
  private static final String ENDPOINT = "soap";
  private static final URI LOCATION = null;

  private ServletProcessEngineClient() { }

  public static Future<TaskState> finishTask(long handle, Node payload, Principal principal, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<Node>, Node> param1 = Tripple.<String, Class<Node>, Node>tripple("payload", Node.class, payload);

    Source message = SoapHelper.createMessage(new QName("finishTask"), Arrays.asList(new JAXBElement<String>(new QName("http://adaptivity.nl/ProcessEngine/","principal"), String.class, principal.getName())), Arrays.asList(param0, param1));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, TaskState.class, jaxbcontext);
  }

  public static Future<TaskState> finishTask(long handle, Node payload, String principal, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<Node>, Node> param1 = Tripple.<String, Class<Node>, Node>tripple("payload", Node.class, payload);
    final Tripple<String, Class<String>, String> param2 = Tripple.<String, Class<String>, String>tripple("principal", String.class, principal);

    Source message = SoapHelper.createMessage(new QName("finishTask"), Arrays.asList(param0, param1, param2));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, TaskState.class, jaxbcontext);
  }

  public static Future<XmlProcessNodeInstance> getProcessNodeInstance(long handle, Principal user, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<Principal>, Principal> param1 = Tripple.<String, Class<Principal>, Principal>tripple("user", Principal.class, user);

    Source message = SoapHelper.createMessage(new QName("getProcessNodeInstance"), Arrays.asList(param0, param1));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, XmlProcessNodeInstance.class, jaxbcontext);
  }

  public static Future<TaskState> updateTaskState(long handle, TaskState state, Principal user, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<TaskState>, TaskState> param1 = Tripple.<String, Class<TaskState>, TaskState>tripple("state", TaskState.class, state);
    final Tripple<String, Class<Principal>, Principal> param2 = Tripple.<String, Class<Principal>, Principal>tripple("user", Principal.class, user);

    Source message = SoapHelper.createMessage(new QName("updateTaskState"), Arrays.asList(param0, param1, param2));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, TaskState.class, jaxbcontext);
  }

}
