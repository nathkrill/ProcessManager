package nl.adaptivity.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
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
import javax.xml.namespace.QName;

import net.devrieze.util.InputStreamOutputStream;
import nl.adaptivity.messaging.ISendableMessage.Header;
import nl.adaptivity.process.messaging.AsyncMessenger;
import nl.adaptivity.util.HttpMessage;


public class DarwinMessenger implements IMessenger {

  /**
   * How big should the worker thread pool be initially.
   */
  private static final int INITIAL_WORK_THREADS = 1;

  /**
   * How many worker threads are there concurrently? Note that extra work will not block,
   * it will just be added to a waiting queue.
   */
  private static final int MAXIMUM_WORK_THREADS = 20;

  /**
   * How long to keep idle worker threads busy (in miliseconds).
   */
  private static final int WORKER_KEEPALIVE_MS = 60000;

  /**
   * How many queued messages should be allowed. This is also the limit of pending notifications.
   */
  private static final int CONCURRENTCAPACITY = 2048; // Allow 2048 pending messages

  /** The name of the notification tread. */
  private static final String NOTIFIERTHREADNAME = DarwinMessenger.class.getName()+" - Completion Notifier";

  /**
   * How long should the notification thread wait when polling messages. This
   * should ensure that every 30 seconds it checks whether it's finished.
   */
  private static final long NOTIFICATIONPOLLTIMEOUTMS = 30000l; // Timeout polling for next message every 30 seconds


  /**
   * Helper thread that performs (in a single tread) all notifications of messaging completions.
   * The notification can not be done on the sending thread (deadlocks as that thread would be waiting for itself) and the calling tread is unknown.
   * @author Paul de Vrieze
   */
  private class MessageCompletionNotifier extends Thread {

    private final BlockingQueue<MessageTask<?>> aPendingNotifications;
    private volatile boolean aFinished = false;


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
      while (! aFinished) {
        try {
          MessageTask<?> future = aPendingNotifications.poll(NOTIFICATIONPOLLTIMEOUTMS, TimeUnit.MILLISECONDS);
          if (future!=null) // Null when timeout.
            notififyCompletion(future);
        } catch (InterruptedException e) {
          // Ignore the interruption. Just continue
        }
      }

    }

    private <T extends DataSource> void notififyCompletion(MessageTask<T> pFuture) {
      ArrayList<CompletionListener> listeners;
      pFuture.aCompletionListener.onMessageCompletion(pFuture);
    }

    /**
     * Allow for shutting down the thread.
     */
    public void shutdown() {
      aFinished = true;
      interrupt();
    }

    /**
     * Add a notification to the message queue.
     * @param pFuture The future whose completion should be communicated.
     */
    public void addNotification(MessageTask<?> pFuture) {
      // aPendingNotifications is threadsafe!
      aPendingNotifications.add(pFuture);

    }


  }

  private class MessageTask<T extends DataSource> implements Future<T>, Runnable {

    private URI aDestURL;
    private ISendableMessage aMessage;
    private CompletionListener aCompletionListener;
    private DataSource aResult = null;
    private boolean aCancelled = false;
    private int aResponseCode;
    private Exception aError = null;
    private boolean aStarted = false;



    public MessageTask(URI pDestURL, ISendableMessage pMessage, CompletionListener pCompletionListener) {
      aDestURL = pDestURL;
      aMessage = pMessage;
      aCompletionListener = pCompletionListener;
    }

    @Override
    public void run() {
      boolean cancelled;
      synchronized(this) {
        aStarted = true;
        cancelled = aCancelled;
      }
      try {
        if (! cancelled ) {
          DataSource result = sendMessage();
          synchronized(this) {
            aResult = result;
          }
        } else {

        }
      } catch (MessagingException e) {
        Logger.getLogger(DarwinMessenger.class.getName()).log(Level.WARNING, "Error sending message",e);
        throw e;
      } catch (Exception e) {
        synchronized(this) {
          aError = e;
        }
      } finally {
        notifyCompletionListener(this);
      }
    }


    private DataSource sendMessage() throws IOException, ProtocolException {
      URL destination;

      try {
        destination = aDestURL.toURL();
      } catch (MalformedURLException e) {
        throw new MessagingException(e);
      }

      final URLConnection connection = destination.openConnection();
      if (connection instanceof HttpURLConnection){
        final HttpURLConnection httpConnection = (HttpURLConnection) connection;
        boolean hasPayload = aMessage.getBodySource()!=null;
        connection.setDoOutput(hasPayload);
        String method = aMessage.getMethod();
        if (method==null) {
          method = hasPayload ? "POST" : "GET";
        }
        httpConnection.setRequestMethod(method);

        boolean contenttypeset = false;
        for(Header header: aMessage.getHeaders()) {
          httpConnection.addRequestProperty(header.getName(), header.getValue());
          contenttypeset |= "Content-Type".equals(header.getName());
        }
        try {
          httpConnection.connect();
        } catch (ConnectException e) {
          throw new MessagingException("Error connecting to "+destination, e);
        }
        try {
          if (hasPayload) {
            if (! contenttypeset) { // Set the content type from the source if not yet set.
              String contentType = aMessage.getBodySource().getContentType();
              if (contentType!=null && contentType.length()>0) {
                httpConnection.addRequestProperty("Content-Type", contentType);
              }
            }

            OutputStream out = httpConnection.getOutputStream();

            try {
              InputStreamOutputStream.writeToOutputStream(aMessage.getBodySource().getInputStream(), out);
            } finally {
              out.close();
            }
          }
          aResponseCode = httpConnection.getResponseCode();
          if (aResponseCode<200 || aResponseCode>=400) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamOutputStream.writeToOutputStream(httpConnection.getErrorStream(), baos);
            String errorMessage = "Error in sending message with "+method+" to ("+destination+") ["+aResponseCode+"]:\n"+new String(baos.toByteArray());
            Logger.getLogger(AsyncMessenger.class.getName()).info(errorMessage);
            throw new HttpResponseException(httpConnection.getResponseCode(), errorMessage);
          }
          ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
          InputStream in = httpConnection.getInputStream();
          try {
            InputStreamOutputStream.writeToOutputStream(in, resultBuffer);
          } finally {
            in.close();
          }
          return new HttpMessage.ByteContentDataSource(null, httpConnection.getContentType(), resultBuffer.toByteArray());

        } finally {
          httpConnection.disconnect();
        }

      } else {
        throw new UnsupportedOperationException("No support yet for non-http connections");
      }
    }


    @Override
    public boolean cancel(boolean pMayInterruptIfRunning) {
      if (aCancelled) { return true; }
      if (! aStarted) {
        aCancelled = true;
      }
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public synchronized boolean isCancelled() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public synchronized boolean isDone() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
      if (aCancelled) { throw new CancellationException(); }
      if (aError!=null) { throw new ExecutionException(aError); }
      if (aResult!=null) { return aResult; }

      // wait for the result
    }

    @Override
    public synchronized T get(long pTimeout, TimeUnit pUnit) throws InterruptedException, ExecutionException, TimeoutException {
      // TODO Auto-generated method stub
      return null;
    }

  }

  private DarwinMessenger() {
    aExecutor = new ThreadPoolExecutor(INITIAL_WORK_THREADS, MAXIMUM_WORK_THREADS, WORKER_KEEPALIVE_MS, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(CONCURRENTCAPACITY, true));
    aNotifier = new MessageCompletionNotifier();
    aNotifier.start();
  }

  ExecutorService aExecutor;
  private Map<QName,Map<String, Endpoint>> aServices;

  private MessageCompletionNotifier aNotifier;


  void notifyCompletionListener(MessageTask<?> pFuture) {
    aNotifier.addNotification(pFuture);
  }


  @Override
  public void registerEndpoint(QName pService, String pEndPoint, URI pTarget) {
    registerEndpoint(new EndPointDescriptor(pService, pEndPoint, pTarget));
  }

  @Override
  public void registerEndpoint(Endpoint pEndpoint) {
    Map<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service==null) {
      service = new TreeMap<String, Endpoint>();
      aServices.put(pEndpoint.getServiceName(), service);
    }
    if (service.containsKey(pEndpoint.getEndpointName())) {
      service.remove(pEndpoint.getEndpointName());
    }
    service.put(pEndpoint.getEndpointName(), pEndpoint);
  }

  @Override
  public <T> Future<T> sendMessage(ISendableMessage pMessage, CompletionListener pCompletionListener) {
    Endpoint registeredEndpoint = getEndpoint(pMessage.getDestination());

    if (registeredEndpoint instanceof DirectEndpoint) {
      return ((DirectEndpoint) registeredEndpoint).<T>deliverMessage(pMessage, pCompletionListener);
    }

    if (registeredEndpoint==null) {
      registeredEndpoint = pMessage.getDestination();
    }

    final URI destURL = registeredEndpoint.getEndpointLocation();

    MessageTask messageTask = new MessageTask(destURL, pMessage, pCompletionListener);
    aExecutor.execute(messageTask);
    return messageTask;
  }

  public Endpoint getEndpoint(QName pServiceName, String pEndpointName) {
    Map<String, Endpoint> service = aServices.get(pServiceName);
    if (service==null) { return null; }
    return service.get(pEndpointName);
  }

  public Endpoint getEndpoint(Endpoint pEndpoint) {
    Map<String, Endpoint> service = aServices.get(pEndpoint.getServiceName());
    if (service==null) { return null; }
    return service.get(pEndpoint.getServiceName());
  }

}
