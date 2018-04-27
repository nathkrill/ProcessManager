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

package nl.adaptivity.xml

import nl.adaptivity.util.xml.CompactFragment
import nl.adaptivity.xml.XmlStreaming.deSerialize

//XXX EXPECT
//expect interface XmlStreamingFactory

/**
 * Utility class with factories and constants for the [XmlReader] and [XmlWriter] interfaces.
 * Created by pdvrieze on 15/11/15.
 */
/*
expect object XmlStreaming {

  fun setFactory(factory: XmlStreamingFactory?)

  inline fun <reified T> deSerialize(input: String): T

  fun toString(value: XmlSerializable): String

}
*/


/** Flag to indicate that the xml declaration should be omitted, when possible.  */
const val FLAG_OMIT_XMLDECL = 1
const val FLAG_REPAIR_NS = 2
const val DEFAULT_FLAGS = FLAG_OMIT_XMLDECL


/*
XXX EXPECT
expect inline fun<reified T : Any>  deserialize(input:String): T

expect fun CompactFragment(content:String): CompactFragment
expect fun CompactFragment(namespaces:Iterable<Namespace>, content:CharArray?): CompactFragment
expect fun CompactFragment(namespaces:Iterable<Namespace>, content:String?): CompactFragment
*/