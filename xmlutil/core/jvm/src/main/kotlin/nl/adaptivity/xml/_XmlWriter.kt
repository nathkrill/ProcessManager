/*
 * Copyright (c) 2018.
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
@file:JvmMultifileClass
@file:JvmName("XmlWriterUtilJVM")
package nl.adaptivity.xml

import org.w3c.dom.Node
import javax.xml.transform.dom.DOMSource

fun XmlWriter.writeChild(node: Node) {
    serialize(node)
}

fun XmlWriter.serialize(node: Node) {
    val xmlReader = XmlStreaming.newReader(DOMSource(node))
    serialize(xmlReader)
}
