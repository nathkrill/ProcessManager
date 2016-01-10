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

package nl.adaptivity.process.processModel;


import nl.adaptivity.process.ProcessConsts.Engine;

import javax.xml.namespace.QName;


public interface Join<T extends ProcessNode<T, M>, M extends ProcessModel<T, M>> extends ProcessNode<T, M>, JoinSplit<T, M> {

  String ELEMENTLOCALNAME = "join";
  QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);
  QName PREDELEMNAME = new QName(Engine.NAMESPACE, "predecessor", Engine.NSPREFIX);
  // No methods beyond JoinSplit
}