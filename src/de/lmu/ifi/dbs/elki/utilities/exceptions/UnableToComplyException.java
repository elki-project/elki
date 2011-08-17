package de.lmu.ifi.dbs.elki.utilities.exceptions;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * General Exception to state inability to execute an operation.
 * 
 * @author Arthur Zimek
 */
@SuppressWarnings("serial")
public class UnableToComplyException extends Exception {
  /**
   * Exception to state inability to execute an operation.
   * 
   */
  public UnableToComplyException() {
    super();
  }

  /**
   * Exception to state inability to execute an operation.
   * 
   * @param message a message to describe cause of exception
   */
  public UnableToComplyException(String message) {
    super(message);
  }

  /**
   * Exception to state inability to execute an operation.
   * 
   * @param message a message to describe cause of exception
   * @param cause cause of exception
   */
  public UnableToComplyException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Exception to state inability to execute an operation.
   * 
   * @param cause cause of exception
   */
  public UnableToComplyException(Throwable cause) {
    super(cause);
  }

}
