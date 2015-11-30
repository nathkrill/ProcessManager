package nl.adaptivity.process.userMessageHandler.server;

import net.devrieze.util.TransactionFactory;
import net.devrieze.util.db.DBTransaction;
import net.devrieze.util.db.DbSet;
import net.devrieze.util.security.PermissionDeniedException;
import nl.adaptivity.messaging.CompletionListener;
import nl.adaptivity.process.engine.processModel.IProcessNodeInstance.TaskState;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;


public class UserMessageService implements CompletionListener<Boolean /*<TODO Placeholder type*/> {

  private static class MyDBTransactionFactory implements TransactionFactory<DBTransaction> {
    private final Context mContext;

    private javax.sql.DataSource mDBResource = null;

    MyDBTransactionFactory() {
      InitialContext ic = null;
      try {
        ic = new InitialContext();
        mContext = (Context) ic.lookup("java:/comp/env");
      } catch (NamingException e) {
        throw new RuntimeException(e);
      }
    }

    private javax.sql.DataSource getDBResource() {
      if (mDBResource ==null) {
        if (mContext!=null) {
          mDBResource = DbSet.resourceNameToDataSource(mContext, DB_RESOURCE);
        } else {
          mDBResource = DbSet.resourceNameToDataSource(mContext, DB_RESOURCE);
        }
      }
      return mDBResource;
    }

    @Override
    public DBTransaction startTransaction() {
      try {
        return new DBTransaction(getDBResource());
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Connection getConnection() throws SQLException {
      return mDBResource.getConnection();
    }

    @Override
    public boolean isValidTransaction(final DBTransaction transaction) {
      return transaction.providerEquals(mDBResource);
    }
  }

  public static final String CONTEXT_PATH = "java:/comp/env";
  public static final String DB_RESOURCE = "jdbc/usertasks";
  public static final String DBRESOURCENAME= CONTEXT_PATH +'/'+ DB_RESOURCE;


  private static class InstantiationHelper {

    public static final UserMessageService INSTANCE = new UserMessageService();

  }

  private UserTaskMap mTasks;
  private TransactionFactory<DBTransaction> mTransactionFactory = new MyDBTransactionFactory();

  public UserMessageService() {
  }

  private UserTaskMap getTasks() {
    if (mTasks==null) {
      mTasks = new UserTaskMap(mTransactionFactory);
    }
    return mTasks;
  }

  public boolean postTask(final XmlTask task) {
    return getTasks().put(task) >= 0;
  }

  public Collection<XmlTask> getPendingTasks(DBTransaction transaction) {
    final Iterable<XmlTask> tasks = getTasks().iterable(transaction);
    ArrayList<XmlTask> result = new ArrayList<>();
    for(XmlTask task:tasks) {
      result.add(task);
    }
    return result;
  }

  public XmlTask getPendingTask(long handle, Principal user) {
    return getTasks().get(handle);
  }

  public TaskState finishTask(final long handle, final Principal user) {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    final UserTask<?> task = getTask(handle);
    task.setState(TaskState.Complete, user);
    if ((task.getState() == TaskState.Complete) || (task.getState() == TaskState.Failed)) {
      getTasks().remove(task);
    }
    return task.getState();
  }

  private XmlTask getTask(final long handle) {
    return getTasks().get(handle);
  }

  public TaskState takeTask(final long handle, final Principal user) {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    getTask(handle).setState(TaskState.Taken, user);
    return TaskState.Taken;
  }

  public XmlTask updateTask(DBTransaction transaction, long handle, XmlTask partialNewTask, Principal user) throws SQLException {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    // This needs to be a copy otherwise the cache will interfere with the changes
    XmlTask currentTask;
    {
      final XmlTask t = getTask(handle);
      if (t==null) { return null; }
      currentTask = new XmlTask(t);
    }
    for(XmlItem newItem: partialNewTask.getItems()) {
      if (newItem.getName()!=null && newItem.getName().length()>0) {
        XmlItem currentItem = currentTask.getItem(newItem.getName());
        if (currentItem!=null) {
          currentItem.setValue(newItem.getValue());
        }
      }
    }

    // This may update the server.
    currentTask.setState(partialNewTask.getState(), user);

    getTasks().set(transaction, handle, currentTask);
    return currentTask;
  }

  public TaskState startTask(final long handle, final Principal user) {
    if (user==null) { throw new PermissionDeniedException("There is no user associated with this request"); }
    getTask(handle).setState(TaskState.Started, user);
    return TaskState.Taken;
  }

  public static UserMessageService getInstance() {
    return InstantiationHelper.INSTANCE;
  }

  public void destroy() {
    // For now do nothing. Put deinitialization here.
  }

  @Override
  public void onMessageCompletion(final Future<? extends Boolean> future) {
    // TODO Auto-generated method stub
    //
  }

  public DBTransaction newTransaction() throws SQLException {
    return mTransactionFactory.startTransaction();
  }

}
