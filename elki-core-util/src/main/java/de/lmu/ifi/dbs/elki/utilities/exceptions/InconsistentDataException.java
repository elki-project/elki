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
 * Data inconsistency exception.
 * 
 * Thrown when inconsistent data was detected e.g. in an index.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class InconsistentDataException extends RuntimeException {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   * 
   * @param msg Error message
   */
  public InconsistentDataException(String msg) {
    super(msg);
  }

  /**
   * Constructor.
   * 
   * @param msg Error message
   * @param cause Error cause
   */
  public InconsistentDataException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
