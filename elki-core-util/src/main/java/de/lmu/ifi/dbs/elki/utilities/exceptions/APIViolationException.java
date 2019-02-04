/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.utilities.exceptions;

/**
 * This class indicates an exception likely caused by an API not implemented
 * correctly. ELKI has some API interface restrictions that cannot be expressed
 * in the Java language and that are checked at runtime. This in particular
 * includes parameterization and dynamic binding of algorithms to data.
 * 
 * This exception indicates that such an API might not have been implemented
 * correctly; you should check the documentation of interfaces you implement for
 * API descriptions that are not expressible in Java (and thus not checked by
 * the Java compiler).
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class APIViolationException extends AbortException {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   * 
   * @param message Error message
   */
  public APIViolationException(String message) {
    super(message);
  }

  /**
   * Constructor.
   * 
   * @param message Error message
   * @param cause Reason
   */
  public APIViolationException(String message, Throwable cause) {
    super(message, cause);
  }
}
