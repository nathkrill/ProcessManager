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

package nl.adaptivity.process.engine

import net.devrieze.util.Transaction

/**
 * Created by pdvrieze on 20/11/16.
 */
interface ProcessTransaction<T: ProcessTransaction<T>> : Transaction {
  val readableEngineData: ProcessEngineDataAccess<T>
  val writableEngineData: MutableProcessEngineDataAccess<T>
}

interface ProcessTransactionFactory<T: ProcessTransaction<T>> {
  fun startTransaction(engineData: IProcessEngineData<T>): T;
}