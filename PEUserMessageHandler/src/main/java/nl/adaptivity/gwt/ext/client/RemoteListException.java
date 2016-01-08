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

package nl.adaptivity.gwt.ext.client;

import com.google.gwt.http.client.RequestException;


public class RemoteListException extends RequestException {

  private static final long serialVersionUID = -4607613016180815639L;

  private final int mStatusCode;

  public RemoteListException(){mStatusCode=-1;}

  public RemoteListException(final int statusCode, final String statusText) {
    super("Error (" + statusCode + "): " + statusText);
    mStatusCode = statusCode;
  }

  public int getStatusCode() {
    return mStatusCode;
  }

}
