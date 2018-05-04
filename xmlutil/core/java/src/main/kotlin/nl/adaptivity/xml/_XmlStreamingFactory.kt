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

import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import javax.xml.transform.Result
import javax.xml.transform.Source

actual interface XmlStreamingFactory {

  fun newWriter(writer: Writer, repairNamespaces: Boolean = false, omitXmlDecl: Boolean = false): XmlWriter

  fun newWriter(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = false, omitXmlDecl: Boolean= false): XmlWriter

  fun newWriter(result: Result, repairNamespaces: Boolean = false, omitXmlDecl: Boolean = false): XmlWriter

  fun newReader(source: Source): XmlReader

  fun newReader(reader: Reader): XmlReader

  fun newReader(inputStream: InputStream, encoding: String = Charsets.UTF_8.name()): XmlReader
}