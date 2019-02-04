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
 * Exception for aborting some process and transporting a message.
 * 
 * @author Arthur Zimek
 * @since 0.1
 */
public class AbortException extends RuntimeException {
  /**
   * Serial UID.
   */
  private static final long serialVersionUID = -1128409354869276998L;

  /**
   * Exception for aborting some process and transporting a message.
   * 
   * @param message message to be transported
   */
  public AbortException(String message) {
    super(message);
  }

  /**
   * Exception for aborting some process and transporting a message.
   * 
   * @param message message to be transported
   * @param cause cause of this exception
   */
  public AbortException(String message, Throwable cause) {
    super(message, cause);
  }
}
