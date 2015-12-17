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
import nl.adaptivity.process.engine.XmlHandle;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.NodeInstanceState;
import nl.adaptivity.process.engine.processModel.XmlProcessNodeInstance;
import nl.adaptivity.process.processModel.ProcessModelBase;
import nl.adaptivity.process.processModel.engine.ProcessModelRef;
import nl.adaptivity.ws.soap.SoapHelper;
import nl.adaptivity.xml.XmlException;

import org.w3c.dom.Node;

@SuppressWarnings("all")
public class ServletProcessEngineClient {

  private static final QName SERVICE = new QName("http://adaptivity.nl/ProcessEngine/", "ProcessEngine", "");
  private static final String ENDPOINT = "soap";
  private static final URI LOCATION = null;

  private ServletProcessEngineClient() { }

  public static Future<NodeInstanceState> finishTask(long handle, Node payload, Principal principal, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<Node>, Node> param1 = Tripple.<String, Class<Node>, Node>tripple("payload", Node.class, payload);

    Source message = SoapHelper.createMessage(new QName("finishTask"), Arrays.asList(new JAXBElement<String>(new QName("http://adaptivity.nl/ProcessEngine/","principal"), String.class, principal.getName())), Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(param0, param1));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, NodeInstanceState.class, jaxbcontext);
  }

  public static Future<XmlProcessNodeInstance> getProcessNodeInstance(long handle, Principal user, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<Principal>, Principal> param1 = Tripple.<String, Class<Principal>, Principal>tripple("user", Principal.class, user);

    Source message = SoapHelper.createMessage(new QName("getProcessNodeInstance"), Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(param0, param1));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, XmlProcessNodeInstance.class, jaxbcontext);
  }

  public static Future<ProcessModelRef> postProcessModel(ProcessModelBase processModel, Principal principal, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {
    final Tripple<String, Class<ProcessModelBase>, ProcessModelBase> param0 = Tripple.<String, Class<ProcessModelBase>, ProcessModelBase>tripple("processModel", ProcessModelBase.class, processModel);

    Source message = SoapHelper.createMessage(new QName("postProcessModel"), Arrays.asList(new JAXBElement<String>(new QName("http://adaptivity.nl/ProcessEngine/","principal"), String.class, principal.getName())), Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(param0));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ProcessModelRef.class, jaxbcontext);
  }

  public static Future<XmlHandle<?>> startProcess(long handle, String name, String uuid, Principal owner, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<String>, String> param1 = Tripple.<String, Class<String>, String>tripple("name", String.class, name);
    final Tripple<String, Class<String>, String> param2 = Tripple.<String, Class<String>, String>tripple("uuid", String.class, uuid);

    Source message = SoapHelper.createMessage(new QName(""), Arrays.asList(new JAXBElement<String>(new QName("http://adaptivity.nl/ProcessEngine/","principal"), String.class, owner.getName())), Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(param0, param1, param2));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, XmlHandle.class, jaxbcontext);
  }

  public static Future<ProcessModelRef> updateProcessModel(long handle, ProcessModelBase processModel, Principal principal, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<ProcessModelBase>, ProcessModelBase> param1 = Tripple.<String, Class<ProcessModelBase>, ProcessModelBase>tripple("processModel", ProcessModelBase.class, processModel);

    Source message = SoapHelper.createMessage(new QName("updateProcessModel"), Arrays.asList(new JAXBElement<String>(new QName("http://adaptivity.nl/ProcessEngine/","principal"), String.class, principal.getName())), Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(param0, param1));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ProcessModelRef.class, jaxbcontext);
  }

  public static Future<NodeInstanceState> updateTaskState(long handle, NodeInstanceState state, Principal user, CompletionListener completionListener, Class<?>... jaxbcontext) throws JAXBException, XmlException {
    final Tripple<String, Class<Long>, Long> param0 = Tripple.<String, Class<Long>, Long>tripple("handle", long.class, handle);
    final Tripple<String, Class<NodeInstanceState>, NodeInstanceState> param1 = Tripple.<String, Class<NodeInstanceState>, NodeInstanceState>tripple("state", NodeInstanceState.class, state);
    final Tripple<String, Class<Principal>, Principal> param2 = Tripple.<String, Class<Principal>, Principal>tripple("user", Principal.class, user);

    Source message = SoapHelper.createMessage(new QName("updateTaskState"), Arrays.<Tripple<String, ? extends Class<?>, ?>>asList(param0, param1, param2));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, NodeInstanceState.class, jaxbcontext);
  }

}
