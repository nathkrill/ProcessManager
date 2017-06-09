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
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.processModel

import nl.adaptivity.process.engine.ProcessData
import nl.adaptivity.xml.Namespace
import nl.adaptivity.xml.XmlSerializable
import org.w3c.dom.Node


interface IXmlResultType : XmlSerializable {

  val content: CharArray

  /**
   * The value of the name property.
   */
  fun getName(): String?

  fun setName(value:String?)

  /**
   * Gets the value of the path property.
   *
   * @return possible object is [String]
   */
  fun getPath(): String?

  /**
   * Sets the value of the path property.
   *
   * @param namespaceContext
   *
   * @param value allowed object is [String]
   */
  fun setPath(namespaceContext: Iterable<Namespace>, value: String?)

  fun apply(payload: Node?): ProcessData

  /**
   * Get the namespace context for evaluating the xpath expression.
   * @return the context
   */
  fun getOriginalNSContext(): Iterable<Namespace>
}