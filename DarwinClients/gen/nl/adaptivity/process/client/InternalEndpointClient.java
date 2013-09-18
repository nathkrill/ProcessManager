/*
 * Generated by MessagingSoapClientGenerator.
 * Source class: nl.adaptivity.process.userMessageHandler.server.InternalEndpoint
 */

package nl.adaptivity.process.client;

import java.net.URI;
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
import nl.adaptivity.process.messaging.ActivityResponse;
import nl.adaptivity.process.userMessageHandler.server.UserTask;
import nl.adaptivity.ws.soap.SoapHelper;

@SuppressWarnings("all")
public class InternalEndpointClient {

  private static final QName SERVICE = new QName("http://adaptivity.nl/userMessageHandler", "userMessageHandler", "");
  private static final String ENDPOINT = "internal";
  private static final URI LOCATION = null;

  private InternalEndpointClient() { }

  public static Future<ActivityResponse<Boolean>> postTask(EndpointDescriptorImpl replies, UserTask<?> task, CompletionListener completionListener) throws JAXBException {
    final Tripple<String, Class<EndpointDescriptorImpl>, EndpointDescriptorImpl> param0 = Tripple.<String, Class<EndpointDescriptorImpl>, EndpointDescriptorImpl>tripple("replies", EndpointDescriptorImpl.class, replies);
    final Tripple<String, Class<UserTask>, UserTask<?>> param1 = Tripple.<String, Class<UserTask>, UserTask<?>>tripple("task", UserTask.class, task);

    Source message = SoapHelper.createMessage(new QName(""), Arrays.asList(param0, param1));

    EndpointDescriptor endpoint = new EndpointDescriptorImpl(SERVICE, ENDPOINT, LOCATION);

    return (Future) MessagingRegistry.sendMessage(new SendableSoapSource(endpoint, message), completionListener, ActivityResponse.class);
  }

}
