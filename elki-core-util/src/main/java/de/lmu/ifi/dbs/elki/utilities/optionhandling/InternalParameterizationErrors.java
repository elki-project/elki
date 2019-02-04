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
package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Pseudo error class that wraps multiple error reports into one.
 * 
 * This is meant for reporting re-parameterization errors.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class InternalParameterizationErrors extends ParameterException {
  /**
   * Serial version ID
   */
  private static final long serialVersionUID = 1L;

  /**
   * The errors that occurred.
   */
  private Collection<? extends Exception> internalErrors;

  /**
   * Constructor.
   * 
   * @param message Error message
   * @param internalErrors internal errors
   */
  public InternalParameterizationErrors(String message, Collection<? extends Exception> internalErrors) {
    super(message);
    this.internalErrors = internalErrors;
  }
  
  /**
   * Constructor.
   * 
   * @param message Error message
   * @param internalError internal error
   */
  public InternalParameterizationErrors(String message, Exception internalError) {
    super(message);
    final ArrayList<Exception> errors = new ArrayList<>(1);
    errors.add(internalError);
    this.internalErrors = errors;
  }
  
  /**
   * @return the internalErrors
   */
  protected Collection<? extends Exception> getInternalErrors() {
    return internalErrors;
  }
}