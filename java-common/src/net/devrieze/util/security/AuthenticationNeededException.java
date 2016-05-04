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

package net.devrieze.util.security;

/**
 * Exception to signify that no user was associated, but a user should be specified.
 */
public class AuthenticationNeededException extends RuntimeException {

  public AuthenticationNeededException(final String message) {
    super(message);
  }

  public AuthenticationNeededException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public AuthenticationNeededException(final Throwable cause) {
    super(cause);
  }

  public AuthenticationNeededException() {
  }
}