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
 * Exception thrown when no compatible data was found.
 *
 * Often this is caused by trying to run e.g. an algorithm that requires a
 * vector field of fixed dimensionality on a data set that is of varying
 * dimensionality, or when trying to run an algorithm that only supports
 * numerical attributes on non-numerical data.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class IncompatibleDataException extends AbortException {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   *
   * @param message Error message.
   */
  public IncompatibleDataException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message Error message
   * @param cause Cause
   */
  public IncompatibleDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
