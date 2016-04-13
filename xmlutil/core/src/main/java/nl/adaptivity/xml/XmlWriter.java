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

package nl.adaptivity.xml;

import javax.xml.namespace.NamespaceContext;


/**
 * Created by pdvrieze on 15/11/15.
 */
public interface XmlWriter {

  void setPrefix(CharSequence prefix, CharSequence namespaceUri) throws XmlException;

  void namespaceAttr(CharSequence namespacePrefix, CharSequence namespaceUri) throws XmlException;

  // Property accessors start
  int getDepth();

  void close() throws XmlException;

  /**
   * Flush all state to the underlying buffer
   */
  void flush() throws XmlException;

  /**
   * Write a start tag.
   * @param namespace The namespace to use for the tag.
   * @param localName The local name for the tag.
   * @param prefix The prefix to use, or <code>null</code> for the namespace to be assigned automatically
   */
  void startTag(CharSequence namespace, CharSequence localName, CharSequence prefix) throws XmlException;

  /**
   * Write a comment.
   * @param text The comment text
   */
  void comment(CharSequence text) throws XmlException;

  /**
   * Write text.
   * @param text The text content.
   */
  void text(CharSequence text) throws XmlException;

  /**
   * Write a CDATA section
   * @param text The text of the section.
   */
  void cdsect(CharSequence text) throws XmlException;

  void entityRef(CharSequence text) throws XmlException;

  void processingInstruction(CharSequence text) throws XmlException;

  void ignorableWhitespace(CharSequence text) throws XmlException;

  void attribute(CharSequence namespace, CharSequence name, final CharSequence prefix, CharSequence value) throws XmlException;

  void docdecl(CharSequence text) throws XmlException;

  void startDocument(final CharSequence version, CharSequence encoding, Boolean standalone) throws XmlException;

  void endDocument() throws XmlException;

  void endTag(CharSequence namespace, CharSequence localName, CharSequence prefix) throws XmlException;

  NamespaceContext getNamespaceContext();

  CharSequence getNamespaceUri(CharSequence prefix) throws XmlException;

  CharSequence getPrefix(CharSequence namespaceUri) throws XmlException;
}