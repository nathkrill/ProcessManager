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

package net.devrieze.util.db

import net.devrieze.util.AutoCloseableIterator
import net.devrieze.util.HandleMap.ComparableHandle
import net.devrieze.util.HandleMap.Handle
import net.devrieze.util.Handles
import net.devrieze.util.TransactionFactory
import java.io.Closeable
import java.sql.*
import java.util.*
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.NamingException
import javax.sql.DataSource


open class DbSet<T:Any>(pTransactionFactory: TransactionFactory<DBTransaction>, protected open val elementFactory: ElementFactory<T>) : AutoCloseable, Closeable {


  /**
   * Iterable that automatically closes
   * @author pdvrieze
   */
  inner class ClosingIterable : Iterable<T>, AutoCloseable, Closeable {

    override fun close() {
      this@DbSet.close()
    }

    override fun iterator(): MutableIterator<T> {
      return this@DbSet.unsafeIterator()
    }

  }

  private inner class ResultSetIterator @Throws(SQLException::class)
  constructor(private val mTransaction: DBTransaction, private val mStatement: PreparedStatement, private val mResultSet: ResultSet) : AutoCloseableIterator<T> {
    private var mNextElem: T? = null
    private var mFinished = false
    private val mCloseOnFinish: Boolean

    init {
      elementFactory.initResultSet(mResultSet.metaData)
      mCloseOnFinish = false
    }

    @Throws(SQLException::class)
    fun size(): Int {
      val pos = mResultSet.row
      try {
        mResultSet.last()
        return mResultSet.row
      } finally {
        mResultSet.absolute(pos)
      }
    }

    override fun hasNext(): Boolean {
      if (mFinished) {
        return false
      }
      if (mNextElem != null) {
        return true
      }

      try {
        var success = mResultSet.next()
        if (success) {
          mNextElem = elementFactory.create(mTransaction, mResultSet)
          while (success && mNextElem == null) {
            elementFactory.preRemove(mTransaction, mResultSet)
            mResultSet.deleteRow()
            success = mResultSet.next()
            if (success) {
              mNextElem = elementFactory.create(mTransaction, mResultSet)
            }
          }

        }
        if (!success) {
          mFinished = true
          mTransaction.commit()
          if (mCloseOnFinish) {
            closeResultSet(mTransaction, mStatement, mResultSet)
          } else {
            closeResultSet(null, mStatement, mResultSet)
          }
          return false
        }
        // TODO hope that this works
        elementFactory.postCreate(mTransaction, mNextElem)
        return true
      } catch (ex: SQLException) {
        closeResultSet(mTransaction, mStatement, mResultSet)
        throw RuntimeException(ex)
      }

    }

    override fun next(): T {
      val nextElem = mNextElem
      mNextElem = null
      if (nextElem != null) {
        return nextElem
      }
      if (!hasNext()) { // hasNext will actually update mNextElem;
        throw IllegalStateException("Reading beyond iterator")
      }
      return mNextElem!!
    }

    override fun remove() {
      try {
        mResultSet.deleteRow()
      } catch (ex: SQLException) {
        closeResultSet(mTransaction, mStatement, mResultSet)
        throw RuntimeException(ex)
      }

    }

    override fun close() {
      try {
        try {
          try {
            mResultSet.close()
          } finally {
            mStatement.close()
          }
        } finally {
          mIterators.remove(this)
          if (mIterators.isEmpty()) {
            this@DbSet.close()
          }
        }
      } catch (e: SQLException) {
        throw RuntimeException(e)
      }

    }
  }


  protected val transactionFactory: TransactionFactory<DBTransaction>

  private val mIterators = ArrayList<ResultSetIterator>()

  init {
    transactionFactory = pTransactionFactory
  }

  fun closingIterable(): ClosingIterable {
    return ClosingIterable()
  }


  @Deprecated("")
  fun unsafeIterator(): AutoCloseableIterator<T> {
    return unsafeIterator(false)
  }

  @Deprecated("")
  @SuppressWarnings("resource")
  fun unsafeIterator(pReadOnly: Boolean): AutoCloseableIterator<T> {
    var transaction: DBTransaction? = null
    var statement: PreparedStatement? = null
    try {
      transaction = transactionFactory.startTransaction()
      //      connection = mTransactionFactory.getConnection();
      //      connection.setAutoCommit(false);
      val columns = elementFactory.createColumns

      val sql = addFilter("SELECT " + columns + " FROM `" + elementFactory.tableName + "`", " WHERE ")

      statement = transaction!!.prepareStatement(sql,
                                                 ResultSet.TYPE_FORWARD_ONLY,
                                                 if (pReadOnly) ResultSet.CONCUR_READ_ONLY else ResultSet.CONCUR_UPDATABLE)
      setFilterParams(statement, 1)

      statement!!.execute()
      val it = ResultSetIterator(transaction, statement, statement.resultSet)
      mIterators.add(it)
      return it
    } catch (e: Exception) {
      try {
        if (statement != null) {
          statement.close()
        }
      } catch (ex: SQLException) {
        val runtimeException = RuntimeException(ex)
        (runtimeException as java.lang.Throwable).addSuppressed(e)
        throw runtimeException
      } finally {
        if (transaction != null) {
          rollbackConnection(transaction, null, e)
        }
      }

      if (e is RuntimeException) {
        throw e
      }
      throw RuntimeException(e)
    }

  }

  @SuppressWarnings("resource")
  @Throws(SQLException::class)
  open fun iterator(pTransaction: DBTransaction, pReadOnly: Boolean): AutoCloseableIterator<T> {
    try {
      val columns = elementFactory.createColumns

      val sql = addFilter("SELECT " + columns + " FROM `" + elementFactory.tableName + "`", " WHERE ")

      val statement = pTransaction.prepareStatement(sql,
                                                    ResultSet.TYPE_FORWARD_ONLY,
                                                    if (pReadOnly) ResultSet.CONCUR_READ_ONLY else ResultSet.CONCUR_UPDATABLE)
      setFilterParams(statement, 1)

      statement.execute()
      val it = ResultSetIterator(pTransaction, statement, statement.resultSet)
      mIterators.add(it)
      return it
    } catch (e: RuntimeException) {
      rollbackConnection(pTransaction, e)
      close()
      throw e
    } catch (e: SQLException) {
      close()
      throw e
    }

  }

  fun size(): Int {
    try {
      transactionFactory.startTransaction().use { transaction -> return size(transaction) }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  fun size(connection: DBTransaction): Int {
    val columns = elementFactory.createColumns

    val sql = addFilter("SELECT COUNT( " + columns + " ) FROM `" + elementFactory.tableName + "`", " WHERE ")

    connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
      setFilterParams(statement, 1)

      val result = statement.execute()
      if (result) {
        statement.resultSet.use { resultset ->
          if (resultset.next()) {
            return resultset.getInt(1)
          } else {
            throw RuntimeException("Retrieving row count failed")
          }
        }
      } else {
        throw RuntimeException("Retrieving row count failed")
      }
    }
  }

  operator fun contains(`object`: Any): Boolean {
    if (elementFactory.asInstance(`object`) == null) {
      return false
    }
    try {
      transactionFactory.startTransaction().use { transaction ->
        val result = contains(transaction, `object`)
        transaction.commit()
        return result
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  open fun contains(connection: DBTransaction, pO: Any): Boolean {
    val `object` = elementFactory.asInstance(pO)
    if (`object` != null) {
      val sql = addFilter("SELECT COUNT(*) FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getPrimaryKeyCondition(
            `object`) + ")", " AND ")

      connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
        val cnt = elementFactory.setPrimaryKeyParams(statement, `object`, 1)
        setFilterParams(statement, cnt)

        val result = statement.execute()
        if (!result) {
          return false
        }
        statement.getResultSet().use({ resultset -> return resultset.next() })
      }
    } else {
      return false
    }
  }

  fun add(pE: T): Boolean {
    try {
      transactionFactory.startTransaction().use { transaction ->
        return commitIfTrue(transaction,
                            add(transaction, pE))
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  fun add(pTransaction: DBTransaction, pE: T?): Boolean {
    assert(transactionFactory.isValidTransaction(pTransaction))
    if (pE == null) {
      throw NullPointerException()
    }

    try {
      val sql = "INSERT INTO " + elementFactory.tableName + " ( " + join(elementFactory.storeColumns,
                                                                         ", ") + " ) VALUES ( " + join(elementFactory.storeParamHolders,
                                                                                                       ", ") + " )"

      pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
        elementFactory.setStoreParams(statement, pE, 1)

        val changecount = statement.executeUpdate()
        if (changecount > 0) {
          val handle: Long = statement.generatedKeys.use { keys ->
            keys.next()
            keys.getLong(1)
          }
          elementFactory.postStore(pTransaction, Handles.handle<T>(handle), null, pE)
          pTransaction.commit()
          return true
        } else {
          return false
        }
      }
    } catch (e: SQLException) {
      throw RuntimeException(e)
    }

  }

  fun addAll(pC: Collection<T>): Boolean {
    try {
      transactionFactory.startTransaction().use { transaction ->
        return commitIfTrue(transaction,
                            addAll(transaction, pC))
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  fun addAll(pTransaction: DBTransaction, pC: Collection<T>?): Boolean {
    if (pC == null) {
      throw NullPointerException()
    }
    val connection = pTransaction

    val sql = "INSERT INTO " + elementFactory.tableName + " ( " + join(elementFactory.storeColumns,
                                                                       ", ") + " ) VALUES ( " + join(elementFactory.storeParamHolders,
                                                                                                     ", ") + " )"

    connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
      for (element in pC) {
        elementFactory.setStoreParams(statement, element, 1)
        statement.addBatch()
      }
      val result = statement.executeBatch()

      for (c in result) {
        if (c < 1) {
          connection.rollback()
          return false // Error, we just roll back and don't change a thing
        }
      }
      statement.getGeneratedKeys().use({ keys ->
                                         for (element in pC) {
                                           keys.next()
                                           val handle = Handles.handle<T>(keys.getLong(1))
                                           elementFactory.postStore(connection, handle, null, element)
                                         }
                                       })
      connection.commit()
      return result.size > 0
    }
  }

  fun remove(pO: Any): Boolean {
    if (elementFactory.asInstance(pO) == null) {
      return false
    }
    try {
      transactionFactory.startTransaction().use { transaction ->
        return commitIfTrue(transaction,
                            remove(transaction, pO))
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  fun remove(pTransaction: DBTransaction, pO: Any): Boolean {
    val `object` = elementFactory.asInstance(pO)
    if (`object` != null) {
      elementFactory.preRemove(pTransaction, `object`)
      val sql = addFilter("DELETE FROM " + elementFactory.tableName + " WHERE (" + elementFactory.getPrimaryKeyCondition(
            `object`) + ")", " AND ")

      pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
        val cnt = elementFactory.setPrimaryKeyParams(statement, `object`, 1)
        setFilterParams(statement, cnt)

        val changecount = statement.executeUpdate()
        pTransaction.commit()
        return changecount > 0
      }

    } else {
      return false
    }
  }

  fun clear() {
    try {
      transactionFactory.startTransaction().use { transaction ->
        clear(transaction)
        transaction.commit()
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  fun clear(transaction: DBTransaction) {
    elementFactory.preClear(transaction)
    val sql = addFilter("DELETE FROM " + elementFactory.tableName, " WHERE ")

    transaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
      setFilterParams(statement, 1)

      statement.executeUpdate()
      transaction.commit()
    }
  }

  override fun close() {
    var errors: MutableList<RuntimeException>? = null
    for (iterator in mIterators) {
      try {
        iterator.close()
      } catch (e: RuntimeException) {
        if (errors == null) {
          errors = ArrayList<RuntimeException>()
        }
        errors.add(e)
      }

    }
    if (errors != null) {
      val it = errors.iterator()
      val ex = it.next()
      while (it.hasNext()) {
        (ex as java.lang.Throwable).addSuppressed(it.next())
      }
      throw ex
    }
  }

  val isEmpty: Boolean
    get() {
      (unsafeIterator(true) as ResultSetIterator).use { it -> return it.hasNext() }
    }

  @Throws(SQLException::class)
  fun isEmpty(pTransaction: DBTransaction): Boolean {
    (iterator(pTransaction, true) as ResultSetIterator).use { it -> return it.hasNext() }
  }

  fun toArray(): Array<Any> {
    return toArray<Any>(emptyArray())
  }

  fun <T> toArray(pA: Array<T>): Array<T> {
    try {
      (unsafeIterator(true) as DbSet.ResultSetIterator).use { it ->
        val size = it.size()

        if (size ==pA.size) pA else {java.lang.reflect.Array.newInstance(pA.javaClass, size) as Array<T>}.let { result:Array<T> ->

          var i=0
          while (it.hasNext()) {
            result[i] = it.next() as T
            ++i
          }
          return result
        }
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  // TODO do this smarter
  fun containsAll(pC: Collection<*>): Boolean {
    val cpy = HashSet<Any>(pC)
    closingIterable().use { iterable: Iterable<T> ->
      cpy.removeAll(iterable)
    }
    return cpy.isEmpty()
  }

  // TODO do this smarter
  fun retainAll(pC: Collection<*>): Boolean {
    var changed = false
    closingIterable().use { col ->
      val it = col.iterator()
      while (it.hasNext()) {
        if (!pC.contains(it.next())) {
          it.remove()
          changed = true
        }
      }
    }
    return changed
  }

  // TODO do this smarter
  fun removeAll(pC: Collection<*>): Boolean {
    var changed = false
    closingIterable().use { col ->
      val it = col.iterator()
      while (it.hasNext()) {
        if (pC.contains(it.next())) {
          it.remove()
          changed = true
        }
      }
    }
    return changed
  }

  @Throws(SQLException::class)
  fun removeAll(pTransaction: DBTransaction, pSelection: String, vararg pSelectionArgs: Any): Boolean {
    run {
      // First call pre-remove for all elements
      // TODO can this hook into a cache/not require creation
      val sql = addFilter("SELECT " + elementFactory.createColumns + " FROM " + elementFactory.tableName + " WHERE (" + pSelection + ")",
                          " AND ")
      pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
        var cnt = 1
        for (param in pSelectionArgs) {
          statement.setObject(cnt, param)
          ++cnt
        }
        setFilterParams(statement, cnt)

        val result = statement.execute()
        if (!result) {
          // There are no nodes, so no change.
          return false
        }

        statement.getResultSet().use({ resultset ->
                                       val elementFactory = elementFactory
                                       elementFactory.initResultSet(resultset.getMetaData())
                                       while (resultset.next()) {
                                         elementFactory.preRemove(pTransaction, resultset)
                                       }
                                     })
      }
    }

    run {
      val sql = addFilter("DELETE FROM " + elementFactory.tableName + " WHERE (" + pSelection + ")", " AND ")

      pTransaction.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { statement ->
        var cnt = 1
        for (param in pSelectionArgs) {
          statement.setObject(cnt, param)
          ++cnt
        }
        setFilterParams(statement, cnt)

        val changecount = statement.executeUpdate()
        pTransaction.commit()
        return changecount > 0
      }
    }
  }

  protected fun addWithKey(pE: T): Handle<T> {
    try {
      transactionFactory.startTransaction().use { transaction ->
        val handle = addWithKey(transaction, pE)
        if (handle != null) {
          transaction.commit()
        }
        return handle
      }
    } catch (ex: SQLException) {
      throw RuntimeException(ex)
    }

  }

  @Throws(SQLException::class)
  protected fun <W : T> addWithKey(transaction: DBTransaction, element: W): ComparableHandle<W> {
    val sql = "INSERT INTO " + elementFactory.tableName + " ( " + join(elementFactory.storeColumns,
                                                                       ", ") + " ) VALUES ( " + join(elementFactory.storeParamHolders,
                                                                                                     ", ") + " )"

    transaction.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
      elementFactory.setStoreParams(statement, element, 1)

      val fullSql = statement.toString()
      try {
        val changecount = statement.executeUpdate()
        if (changecount > 0) {
          val handle = statement.getGeneratedKeys().use({ keys ->
                                             keys.next()
                                             Handles.handle<W>(keys.getLong(1))
                                           })
          elementFactory.postStore(transaction, handle, null, element)
          return handle
        } else {
          throw SQLException("No handle for the store available")
        }
      } catch (e: SQLException) {
        throw SQLException("Error executing the query: " + fullSql, e)
      }
    }

  }

  protected val connection: Connection
    get() {
      try {
        @SuppressWarnings("resource")
        val connection = transactionFactory.connection
        try {
          connection.autoCommit = false
        } catch (ex: SQLException) {
          connection.close()
          throw ex
        }

        return connection
      } catch (e: SQLException) {
        throw RuntimeException(e)
      }

    }

  protected fun addFilter(pSQL: CharSequence, pConnector: CharSequence): String {
    val filterExpression = elementFactory.filterExpression
    if (filterExpression == null || filterExpression.length == 0) {
      return StringBuilder(pSQL.length + 1).append(pSQL).append(';').toString()
    }
    val result = StringBuilder(pSQL.length + pConnector.length + filterExpression.length)
    return result.append(pSQL).append(pConnector).append(filterExpression).append(';').toString()
  }

  @Throws(SQLException::class)
  protected fun setFilterParams(pStatement: PreparedStatement, pOffset: Int) {
    val filterExpression = elementFactory.filterExpression
    if (filterExpression != null && filterExpression.length >= 0) {
      elementFactory.setFilterParams(pStatement, pOffset)
    }
  }

  override fun toString(): String {
    return "DbSet <SELECT * FROM `" + elementFactory.tableName + "`>"
  }

  companion object {

    @Throws(SQLException::class)
    private fun commitIfTrue(pTransaction: DBTransaction, pValue: Boolean): Boolean {
      if (pValue) {
        pTransaction.commit()
      }
      return pValue
    }

    private fun join(pList: List<CharSequence>?, pSeparator: CharSequence): CharSequence? {
      if (pList == null) {
        return null
      }
      if (pList.isEmpty()) {
        return ""
      }
      val result = StringBuilder()
      val it = pList.iterator()
      result.append(it.next())
      while (it.hasNext()) {
        result.append(pSeparator).append(it.next())
      }
      return result
    }

    @JvmStatic
    protected fun join(pList1: List<CharSequence>,
                       pList2: List<CharSequence>,
                       pOuterSeparator: CharSequence,
                       pInnerSeparator: CharSequence): CharSequence {
      if (pList1.size != pList2.size) {
        throw IllegalArgumentException("List sizes must match")
      }
      if (pList1.isEmpty()) {
        return ""
      }
      val result = StringBuilder()
      val it1 = pList1.iterator()
      val it2 = pList2.iterator()

      result.append(it1.next()).append(pInnerSeparator).append(it2.next())
      while (it1.hasNext()) {
        result.append(pOuterSeparator).append(it1.next()).append(pInnerSeparator).append(it2.next())
      }
      return result
    }

    @JvmStatic
    protected fun rollbackConnection(pConnection: DBTransaction, pCause: Throwable) {
      rollbackConnection(pConnection, null, pCause)
    }

    private fun rollbackConnection(pConnection: DBTransaction, pSavepoint: Savepoint?, pCause: Throwable?) {
      try {
        if (pSavepoint == null) {
          pConnection.rollback()
        } else {
          pConnection.rollback(pSavepoint)
        }
      } catch (ex: SQLException) {
        if (pCause != null) {
          (pCause as java.lang.Throwable).addSuppressed(ex)
        } else {
          throw RuntimeException(ex)
        }
      }

      if (pCause is RuntimeException) {
        throw pCause
      }
      throw RuntimeException(pCause)
    }

    @JvmStatic
    protected fun rollbackConnection(pConnection: Connection, pSavepoint: Savepoint?, pCause: Throwable?) {
      try {
        if (pSavepoint == null) {
          pConnection.rollback()
        } else {
          pConnection.rollback(pSavepoint)
        }
      } catch (ex: SQLException) {
        if (pCause != null) {
          (pCause as java.lang.Throwable).addSuppressed(ex)
        } else {
          throw RuntimeException(ex)
        }
      }

      if (pCause is RuntimeException) {
        throw pCause
      }
      throw RuntimeException(pCause)
    }


    @JvmStatic
    @Deprecated("Use try-with")
    protected fun closeConnection(pConnection: Connection?, e: Exception) {
      try {
        pConnection?.close()
      } catch (ex: Exception) {
        (e as java.lang.Throwable).addSuppressed(ex)
      }

    }

    @JvmStatic
    protected fun closeResultSet(pConnection: DBTransaction?, pStatement: PreparedStatement, pResultSet: ResultSet) {
      try {
        try {
          try {
            try {
              pResultSet.close()
            } finally {
              pStatement.close()
            }
          } finally {
            if (pConnection != null) {
              if (!pConnection.isClosed) {
                pConnection.rollback()
              }
            }
          }
        } finally {
          pConnection?.close()
        }
      } catch (e: SQLException) {
        throw RuntimeException(e)
      }

    }


    @JvmStatic
    @Deprecated("use {@link #resourceNameToDataSource(Context, String)}, that is more reliable as the context can be gained earlier",
                ReplaceWith("resourceNameToDataSource(context, pResourceName)"))
    fun resourceNameToDataSource2(pResourceName: String): DataSource {
      try {
        return InitialContext().lookup(pResourceName) as DataSource
      } catch (e: NamingException) {
        throw RuntimeException(e)
      }

    }

    @JvmStatic
    fun resourceNameToDataSource(pContext: Context?, pDbresourcename: String): DataSource {
      try {
        if (pContext == null) {
          try {
            return InitialContext().lookup(pDbresourcename) as DataSource
          } catch (e: NamingException) {
            return InitialContext().lookup("java:/comp/env/" + pDbresourcename) as DataSource
          }

        } else {
          return pContext.lookup(pDbresourcename) as DataSource
        }
      } catch (e: NamingException) {
        throw RuntimeException(e)
      }

    }
  }
}

internal inline fun <R> Connection.use(block: (Connection)-> R):R {
  try {
    return block(this)
  } finally {
    close()
  }
}

internal inline fun <R> PreparedStatement.use(block: (PreparedStatement)-> R):R {
  try {
    return block(this)
  } finally {
    close()
  }
}

internal inline fun <R> ResultSet.use(block: (ResultSet)-> R):R {
  try {
    return block(this)
  } finally {
    close()
  }
}
