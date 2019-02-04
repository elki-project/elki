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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Abstract class with shared code for parameterization handling.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public abstract class AbstractParameterization implements Parameterization {
  /**
   * The logger of the class.
   */
  private static final Logging LOG = Logging.getLogger(AbstractParameterization.class);

  /**
   * Errors
   */
  List<ParameterException> errors = new ArrayList<>();

  @Override
  public Collection<ParameterException> getErrors() {
    return errors;
  }

  @Override
  public void reportError(ParameterException e) {
    errors.add(e);
  }

  /**
   * Log any error that has accumulated.
   */
  public synchronized void logAndClearReportedErrors() {
    for(ParameterException e : getErrors()) {
      if(LOG.isDebugging()) {
        LOG.warning(e.getMessage(), e);
      }
      else {
        LOG.warning(e.getMessage());
      }
    }
    clearErrors();
  }

  /**
   * Clear errors.
   */
  public synchronized void clearErrors() {
    // Do NOT use errors.clear(), since we might have an error report
    // referencing the collection!
    errors = new ArrayList<>();
  }

  /**
   * Fail on errors, log any error that had occurred.
   * 
   * @throws RuntimeException if any error has occurred.
   */
  // TODO: make a multi-exception class?
  public void failOnErrors() throws AbortException {
    final int numerror = getErrors().size();
    if(numerror > 0) {
      logAndClearReportedErrors();
      throw new AbortException(numerror + " errors occurred during parameterization.");
    }
  }

  /**
   * Upon destruction, report any errors that weren't handled yet.
   * 
   * @throws Throwable Errors
   */
  @Override
  protected void finalize() throws Throwable {
    failOnErrors();
    super.finalize();
  }
}
