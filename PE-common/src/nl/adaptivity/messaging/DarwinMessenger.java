package nl.adaptivity.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.devrieze.util.InputStreamOutputStream;
import nl.adaptivity.messaging.ISendableMessage.IHeader;
import nl.adaptivity.process.engine.MyMessagingException;
import nl.adaptivity.util.activation.SourceDataSource;
import nl.adaptivity.util.activation.Sources;
import nl.adaptivity.ws.soap.SoapHelper;
import nl.adaptivity.ws.soap.SoapMessageHandler;


/**
 * Messenger to use in the darwin project.
 *
 * @author Paul de Vrieze
 */
public class DarwinMessenger implements IMessenger {

  /**
   * How big should the worker thread pool be initially.
   */
  private static final int INITIAL_WORK_THREADS = 1;

  /**
   * How many worker threads are there concurrently? Note that extra work will
   * not block, it will just be added to a waiting queue.
   */
  private static final int MAXIMUM_WORK_THREADS = 20;

  /**
   * How long to keep idle worker threads busy (in miliseconds).
   */
  private static final int WORKER_KEEPALIVE_MS = 60000;

  /**
   * How many queued messages should be allowed. This is also the limit of
   * pending notifications.
   */
  private static final int CONCURRENTCAPACITY = 2048; // Allow 2048 pending messages

  /** The name of the notification tread. */
  private static final String NOTIFIERTHREADNAME = DarwinMessenger.class.getName() + " - Completion Notifier";

  /**
   * How long should the notification thread wait when polling messages. This
   * should ensure that every 30 seconds it checks whether it's finished.
   */
  private static final long NOTIFICATIONPOLLTIMEOUTMS = 30000l; // Timeout polling for next message every 30 seconds


  /**
   * Marker object for null results.
   */
  private static final Object NULL = new Object();

  /**
   * Helper thread that performs (in a single tread) all notifications of
   * messaging completions. The notification can not be done on the sending
   * thread (deadlocks as that thread would be waiting for itself) and the
   * calling tread is unknown.
   *
   * @author Paul de Vrieze
   */
  private class MessageCompletionNotifier extends Thread {

    /**
     * Queue containing the notifications still to be sent. This is internally
     * synchronized so doesn't need to be manually synchronized.
     */
    private final BlockingQueue<MessageTask<?>> aPendingNotifications;

    private volatile boolean aFinished = false;


    /**
     * Create a new notifier.
     */
    public MessageCompletionNotifier() {
      super(NOTIFIERTHREADNAME);
      this.setDaemon(true); // This is just a helper thread, don't block cleanup.
      aPendingNotifications = new LinkedBlockingQueue<MessageTask<?>>(CONCURRENTCAPACITY);
    }

    /**
     * Simple message pump.
     */
    @Override
    public void run() {
      while (!aFinished) {
        try {
          final MessageTask<?> future = aPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS);
          if (future != null) {
            notififyCompletion(future);
          }
        } catch (final InterruptedException e) {
          // Ignore the interruption. Just continue
        }
      }

    }

    /**
     * Allow for shutting down the thread. As aFinished is volatile, this should
     * not need further synchronization.
     */
    public void shutdown() {
      aFinished = true;
      interrupt();
    }

    /**
     * Add a notification to the message queue.
     *
     * @param pFuture The future whose completion should be communicated.
     */
    public void addNotification(final MessageTask<?> pFuture) {
      // aPendingNotifications is threadsafe!
      aPendingNotifications.add(pFuture);

    }

    /**
     * Helper method to notify of future completion.
     *
     * @param pFuture The future to notify completion of.
     */
    private <T extends DataSource> void notififyCompletion(final MessageTask<?> pFuture) {
      pFuture.aCompletionListener.onMessageCompletion(pFuture);
    }


  }

  /**
   * Future that encapsulates a future that represents the sending of a message.
   * This is a message that
   * @author Paul de Vrieze
   *
   * @param <T>
   */
  private class MessageTask<T> implements Future<T>, Runnable {

    private URI aDestURL;

    private ISendableMessage aMessage;

    private CompletionListener aCompletionListener;

    private T aResult = null;

    private boolean aCancelled = false;

    private int aResponseCode;

    private Exception aError = null;

    private boolean aStarted = false;

    private final Class<T> aReturnType;


    public MessageTask(final URI pDestURL, final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType) {
      aDestURL = pDestURL;
      aMessage = pMessage;
      aCompletionListener = pCompletionListener;
      aReturnType = pReturnType;
    }

    /**
     * Simple constructor that creates a future encapsulating the exception
     *
     * @param pE
     */
    public MessageTask(final Exception pE) {
      aError = pE;
      aReturnType = null;
    }

    /**
     * Create a future that just contains the value without computation/ waiting
     * possible
     *
     * @param pUnmarshal
     */
    public MessageTask(final T pResult) {
      if (pResult == null) {
        aResult = (T) NULL;
      } else {
        aResult = pResult;
      }
      aReturnType = null;
    }

    @Override
    public void run() {
      boolean cancelled;
      synchronized (this) {
        aStarted = true;
        cancelled = aCancelled;
      }
      try {
        if (!cancelled) {
          final T result = sendMessage();
          synchronized (this) {
            aResult = result;
          }
        }
      } catch (final MessagingException e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message", e);
        throw e;
      } catch (final Exception e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message", e);
        synchronized (this) {
          aError = e;
        }
      } finally {
        notifyCompletionListener(this);
      }
    }


    private T sendMessage() throws IOException, ProtocolException {
      URL destination;

      try {
        destination = aDestURL.toURL();
      } catch (final MalformedURLException e) {
        throw new MessagingException(e);
      }

      final URLConnection connection = destination.openConnection();
      if (connection instanceof HttpURLConnection) {
        final HttpURLConnection httpConnection = (HttpURLConnection) connection;
        final boolean hasPayload = aMessage.getBodySource() != null;
        connection.setDoOutput(hasPayload);
        String method = aMessage.getMethod();
        if (method == null) {
          method = hasPayload ? "POST" : "GET";
        }
        httpConnection.setRequestMethod(method);

        boolean contenttypeset = false;
        for (final IHeader header : aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getName(), header.getValue());
          contenttypeset |= "Content-Type".equals(header.getName());
        }
        if (hasPayload && (!contenttypeset)) { // Set the content type from the source if not yet set.
          final String contentType = aMessage.getBodySource().getContentType();
          if ((contentType != null) && (contentType.length() > 0)) {
            httpConnection.addRequestProperty("Content-Type", contentType);
          }
        }
        try {
          httpConnection.connect();
        } catch (final ConnectException e) {
          throw new MessagingException("Error connecting to " + destination, e);
        }
        try {
          if (hasPayload) {

            final OutputStream out = httpConnection.getOutputStream();

            try {
              InputStreamOutputStream.writeToOutputStream(aMessage.getBodySource().getInputStream(), out);
            } finally {
              out.close();
            }
          }
          aResponseCode = httpConnection.getResponseCode();
          if ((aResponseCode < 200) || (aResponseCode >= 400)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getErrorStream(), baos);
            final String errorMessage = "Error in sending message with " + method + " to (" + destination + ") [" + aResponseCode + "]:\n"
                + new String(baos.toByteArray());
            Logger.getLogger(DarwinMessenger.class.getName()).info(errorMessage);
            throw new HttpResponseException(httpConnection.getResponseCode(), errorMessage);
          }
          if (aReturnType.isAssignableFrom(SourceDataSource.class)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getInputStream(), baos);
            return aReturnType.cast(new SourceDataSource(httpConnection.getContentType(), new StreamSource(new ByteArrayInputStream(baos.toByteArray()))));
          } else {
            return JAXB.unmarshal(httpConnection.getInputStream(), aReturnType);
          }

        } finally {
          httpConnection.disconnect();
        }

      } else {
        throw new UnsupportedOperationException("No support yet for non-http connections");
      }
    }


    @Override
    public synchronized boolean cancel(final boolean pMayInterruptIfRunning) {
      if (aCancelled) {
        return true;
      }
      if (!aStarted) {
        aCancelled = true;
        return true;
      }
      // TODO support interrupt running process
      return false;
    }

    @Override
    public synchronized boolean isCancelled() {
      return aCancelled;
    }

    @Override
    public synchronized boolean isDone() {
      return aCancelled || (aResult != null) || (aError != null);
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      if (aCancelled) {
        throw new CancellationException();
      }
      if (aError != null) {
        throw new ExecutionException(aError);
      }
      if (aResult == NULL) {
        return null;
      }
      if (aResult != null) {
        return aResult;
      }
      wait();
      // wait for the result
      return aResult;
    }

    @Override
    public synchronized T get(final long pTimeout, final TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      if (aCancelled) {
        throw new CancellationException();
      }
      if (aError != null) {
        throw new ExecutionException(aError);
      }
      if (aResult == NULL) {
        return null;
      }
      if (aResult != null) {
        return aResult;
      }
      if (pTimeout == 0) {
        throw new TimeoutException();
      }


      try {
        if (pUnit == TimeUnit.NANOSECONDS) {
          wait(pUnit.toMillis(pTimeout), (int) (pUnit.toNanos(pTimeout) % 1000000));
        } else {
          wait(pUnit.toMillis(pTimeout));
        }
      } catch (final InterruptedException e) {
        if (isDone()) {
          return get(0, TimeUnit.MILLISECONDS);// Don't wait, even if somehow the state is wrong.
        } else {
          throw e;
        }
      }
      throw new TimeoutException();
    }

  }

  /**
   * Let the class loader do the nasty synchronization for us, but still
   * initialise ondemand.
   */
  private static class MessengerHolder {

    static final DarwinMessenger globalMessenger = new DarwinMessenger();
  }

  ExecutorService aExecutor;

  private ConcurrentMap<QName, ConcurrentMap<String, Endpoint>> aServices;

  private final MessageCompletionNotifier aNotifier;

  private URI aLocalUrl;

  /**
   * Get the singleton instance. This also updates the base URL.
   *
   * @return The singleton instance.
   */
  public static void register() {
    MessagingRegistry.registerMessenger(MessengerHolder.globalMessenger);
  }


  private DarwinMessenger() {
    aExecutor = new ThreadPoolExecutor(INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    aNotifier = new MessageCompletionNotifier();
    aServices = new ConcurrentHashMap<QName, ConcurrentMap<String, Endpoint>>();
    aNotifier.start();

    final String localUrl = System.getProperty("nl.adaptivity.messaging.localurl");

    if (localUrl == null) {
      final StringBuilder msg = new StringBuilder();
      msg.append("DarwinMessenger\n" + "------------------------------------------------\n" + "                    WARNING\n"
          + "------------------------------------------------\n" + "  Please set the nl.adaptivity.messaging.localurl property in\n"
          + "  catalina.properties (or a method appropriate for a non-tomcat\n"
          + "  container) to the base url used to contact the messenger by\n"
          + "  other components of the system. The public base url can be set as:\n"
          + "  nl.adaptivity.messaging.baseurl, this should be accessible by\n" + "  all clients of the system.\n"
          + "================================================");
      Logger.getAnonymousLogger().warning(msg.toString());
    } else {
      try {
        aLocalUrl = URI.create(localUrl);
      } catch (final IllegalArgumentException e) {
        Logger.getAnonymousLogger().log(Level.SEVERE, "The given local url is not a valid uri.", e);
      }
    }

  }


  @Override
  public void registerEndpoint(final QName pService, final String pEndPoint, final URI pTarget) {
    registerEndpoint(new EndPointDescriptor(pService, pEndPoint, pTarget));
  }


  @Override
  public synchronized void registerEndpoint(final Endpoint pEndpoint) {
    // Note that even though it's a concurrent map we still need to synchronize to
    // prevent race conditions with multiple registrations.
    ConcurrentMap<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service == null) {
      service = new ConcurrentHashMap<String, Endpoint>();
      aServices.put(pEndpoint.getServiceName(), service);
    }
    if (service.containsKey(pEndpoint.getEndpointName())) {
      service.remove(pEndpoint.getEndpointName());
    }
    service.put(pEndpoint.getEndpointName(), pEndpoint);
  }


  @Override
    public <T> Future<T> sendMessage(final ISendableMessage pMessage, final CompletionListener pCompletionListener, final Class<T> pReturnType) {
      Endpoint registeredEndpoint = getEndpoint(pMessage.getDestination());

      if (registeredEndpoint instanceof DirectEndpoint) {
        return ((DirectEndpoint) registeredEndpoint).deliverMessage(pMessage, pCompletionListener, pReturnType);
      }

      if (registeredEndpoint!=null) { // Direct delivery TODO make this work.
        if ("application/soap+xml".equals(pMessage.getBodySource().getContentType())) {
          final SoapMessageHandler handler = SoapMessageHandler.newInstance(registeredEndpoint);
          Source resultSource;
          try {
            resultSource = handler.processMessage(pMessage.getBodySource(), null); // TODO do something with attachments
          } catch (final Exception e) {
            final Future<T> resultfuture = new MessageTask<T>(e);
            if (pCompletionListener!=null) {
              pCompletionListener.onMessageCompletion(resultfuture);
            }
            return resultfuture;
          }

          final MessageTask<T> resultfuture;
          if (pReturnType.isAssignableFrom(SourceDataSource.class)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
              InputStreamOutputStream.writeToOutputStream(Sources.toInputStream(resultSource), baos);
            } catch (final IOException e) {
              throw new MyMessagingException(e);
            }
            resultfuture = new MessageTask<T>(pReturnType.cast(new SourceDataSource("application/soap+xml", new StreamSource(new ByteArrayInputStream(baos.toByteArray())))));
          } else {
            final T resultval = SoapHelper.processResponse(pReturnType, resultSource);
            resultfuture = new MessageTask<T>(resultval);
          }

  //        resultfuture = new MessageTask<T>(JAXB.unmarshal(resultSource, pReturnType));
          if (pCompletionListener!=null) {
            pCompletionListener.onMessageCompletion(resultfuture);
          }
          return resultfuture;
        }
      }

      if (registeredEndpoint==null) {
        registeredEndpoint = pMessage.getDestination();
      }

      final URI destURL;
      if (aLocalUrl==null) {
        destURL = registeredEndpoint.getEndpointLocation();
      } else {
        destURL = aLocalUrl.resolve(registeredEndpoint.getEndpointLocation());
      }

    final MessageTask<T> messageTask = new MessageTask<T>(destURL, pMessage, pCompletionListener, pReturnType);
      aExecutor.execute(messageTask);
      return messageTask;
    }


  @Override
  public void shutdown() {
    MessagingRegistry.registerMessenger(null); // Unregister this messenger
    aNotifier.shutdown();
    aExecutor.shutdown();
    aServices = null;
  }


  void notifyCompletionListener(final MessageTask<?> pFuture) {
    aNotifier.addNotification(pFuture);
  }


  public Endpoint getEndpoint(final QName pServiceName, final String pEndpointName) {
    Map<String, Endpoint> service = aServices.get(pServiceName);
    if (service==null) { return null; }
    return service.get(pEndpointName);
  }

  public Endpoint getEndpoint(final Endpoint pEndpoint) {
    final Map<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service == null) {
      return null;
    }

    return service.get(pEndpoint.getEndpointName());
  }

}
