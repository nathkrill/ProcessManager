package nl.adaptivity.process.engine;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import net.devrieze.util.CachingDBHandleMap;
import net.devrieze.util.MemHandleMap;
import net.devrieze.util.db.AbstractElementFactory;
import net.devrieze.util.security.SecurityProvider;
import net.devrieze.util.security.SimplePrincipal;

import nl.adaptivity.process.engine.processModel.ProcessNodeInstance;
import nl.adaptivity.process.engine.processModel.ProcessNodeInstanceMap;
import nl.adaptivity.process.processModel.ProcessModel;


public class ProcessInstanceMap extends CachingDBHandleMap<ProcessInstance> {

  public static final String TABLE = "processinstances";
  public static final String COL_HANDLE = "pihandle";
  public static final String COL_OWNER = "owner";
  public static final String COL_NAME = "name";
  public static final String COL_HPROCESSMODEL = "pmhandle";

  static class ProcessInstanceElementFactory extends AbstractElementFactory<ProcessInstance> {

    private static final String QUERY_GET_NODEINSTHANDLES_FROM_PROCINSTANCE = "SELECT "+ProcessNodeInstanceMap.COL_HANDLE+
    " FROM "+ProcessNodeInstanceMap.TABLE+
    " WHERE "+ProcessNodeInstanceMap.COL_HPROCESSINSTANCE +" = ? AND "+
    ProcessNodeInstanceMap.COL_HANDLE+" NOT IN ( SELECT "+ProcessNodeInstanceMap.COL_PREDECESSOR+" FROM "+ProcessNodeInstanceMap.TABLE_PREDECESSORS+" );";
    private int aColNoHandle;
    private int aColNoOwner;
    private int aColNoHProcessModel;
    private int aColNoName;
    private final ProcessEngine aProcessEngine;

    public ProcessInstanceElementFactory(ProcessEngine pProcessEngine) {
      aProcessEngine = pProcessEngine;
    }

    @Override
    public void initResultSet(ResultSetMetaData pMetaData) throws SQLException {
      final int columnCount = pMetaData.getColumnCount();
      for (int i=1; i<=columnCount;++i) {
        String colName = pMetaData.getColumnName(i);
        if (COL_HANDLE.equals(colName)) {
          aColNoHandle = i;
        } else if (COL_OWNER.equals(colName)) {
          aColNoOwner = i;
        } else if (COL_HPROCESSMODEL.equals(colName)) {
          aColNoHProcessModel = i;
        } else if (COL_NAME.equals(colName)) {
          aColNoName = i;
        } // ignore other columns
      }
    }

    @Override
    public CharSequence getHandleCondition(long pElement) {
      return COL_HANDLE+" = ?";
    }

    @Override
    public int setHandleParams(PreparedStatement pStatement, long pHandle, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pHandle);
      return 1;
    }

    @Override
    public CharSequence getTableName() {
      return TABLE;
    }

    @Override
    public int setFilterParams(PreparedStatement pStatement, int pOffset) throws SQLException {
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public ProcessInstance create(DataSource pConnectionProvider, ResultSet pRow) throws SQLException {
      Principal owner = new SimplePrincipal(pRow.getString(aColNoOwner));
      Handle<ProcessModel> hProcessModel = MemHandleMap.handle(pRow.getLong(aColNoHProcessModel));
      ProcessModel processModel = aProcessEngine.getProcessModel(hProcessModel, SecurityProvider.SYSTEMPRINCIPAL);
      String instancename = pRow.getString(aColNoName);
      long piHandle = pRow.getLong(aColNoHandle);

      final ProcessInstance result = new ProcessInstance(piHandle, owner, processModel, instancename, aProcessEngine);
      return result;
    }

    @Override
    public void postCreate(Connection pConnection, ProcessInstance pElement) throws SQLException {
      try (PreparedStatement statement = pConnection.prepareStatement(QUERY_GET_NODEINSTHANDLES_FROM_PROCINSTANCE)) {
        List<Handle<ProcessNodeInstance>> handles = new ArrayList<>();
        if (statement.execute()) {
          try (ResultSet resultset = statement.getResultSet()){
            while (resultset.next()) {
              handles.add(MemHandleMap.<ProcessNodeInstance>handle(resultset.getLong(1)));
            }
          }
        }
        pElement.setThreads(handles);
      }
    }

    @Override
    public CharSequence getPrimaryKeyCondition(ProcessInstance pObject) {
      return getHandleCondition(pObject.getHandle());
    }

    @Override
    public int setPrimaryKeyParams(PreparedStatement pStatement, ProcessInstance pObject, int pOffset) throws SQLException {
      return setHandleParams(pStatement, pObject.getHandle(), pOffset);
    }

    @Override
    public ProcessInstance asInstance(Object pO) {
      if (pO instanceof ProcessInstance) {
        return (ProcessInstance) pO;
      }
      return null;
    }

    @Override
    public CharSequence getCreateColumns() {
      return COL_HANDLE+", "+COL_HPROCESSMODEL+", "+COL_NAME+", "+COL_OWNER;
    }

    @Override
    public CharSequence getStoreColumns() {
      return COL_HPROCESSMODEL+", "+COL_NAME+", "+COL_OWNER;
    }

    @Override
    public CharSequence getStoreParamHolders() {
      return "?, ?, ?";
    }

    @Override
    public int setStoreParams(PreparedStatement pStatement, ProcessInstance pElement, int pOffset) throws SQLException {
      pStatement.setLong(pOffset, pElement.getProcessModel().getHandle());
      pStatement.setString(pOffset+1, COL_NAME);
      pStatement.setString(pOffset+2, pElement.getOwner().getName());
      return 3;
    }

  }

  public ProcessInstanceMap(ProcessEngine pProcessEngine, String pResourceName) {
    super(resourceNameToDataSource(pResourceName), new ProcessInstanceElementFactory(pProcessEngine));
  }

}
