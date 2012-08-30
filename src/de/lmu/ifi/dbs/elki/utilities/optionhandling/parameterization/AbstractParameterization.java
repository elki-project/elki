package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.InternalParameterizationErrors;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Abstract class with shared code for parameterization handling.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractParameterization implements Parameterization {
  // TODO: refactor "tryInstantiate" even in a higher class?

  /**
   * Errors
   */
  java.util.Vector<ParameterException> errors = new java.util.Vector<ParameterException>();

  /**
   * The logger of the class.
   */
  private static final Logging LOG = Logging.getLogger(AbstractParameterization.class);

  @Override
  public Collection<ParameterException> getErrors() {
    return errors;
  }

  @Override
  public boolean hasErrors() {
    return errors.size() > 0;
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
    errors = new java.util.Vector<ParameterException>();
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
   * Report the internal parameterization errors to another parameterization
   * 
   * @param config Other parameterization
   */
  public synchronized void reportInternalParameterizationErrors(Parameterization config) {
    final int numerror = getErrors().size();
    if(numerror > 0) {
      config.reportError(new InternalParameterizationErrors(numerror + " internal (re-) parameterization errors prevented execution.", getErrors()));
      this.clearErrors();
    }
  }

  @Override
  public final boolean grab(Parameter<?, ?> opt) {
    if(opt.isDefined()) {
      LOG.warning("Option " + opt.getName() + " is already set!");
    }
    try {
      if(setValueForOption(opt)) {
        return true;
      }
      // Try default value instead.
      if(opt.tryDefaultValue()) {
        return true;
      }
      // No value available.
      return false;
    }
    catch(ParameterException e) {
      reportError(e);
      return false;
    }
  }

  /**
   * Perform the actual parameter assignment.
   * 
   * @param opt Option to be set
   * @return Success code (value available)
   * @throws ParameterException on assignment errors.
   */
  @Override
  public abstract boolean setValueForOption(Parameter<?, ?> opt) throws ParameterException;

  /** Upon destruction, report any errors that weren't handled yet.
   *  
   * @throws Throwable Errors */
  @Override
  public void finalize() throws Throwable {
    failOnErrors();
    super.finalize();
  }

  @Override
  public boolean checkConstraint(GlobalParameterConstraint constraint) {
    try {
      constraint.test();
    }
    catch(ParameterException e) {
      reportError(e);
      return false;
    }
    return true;
  }

  @Override
  public <C> C tryInstantiate(Class<C> r, Class<?> c) {
    try {
      return ClassGenericsUtil.tryInstantiate(r, c, this);
    }
    catch(Exception e) {
      LOG.exception(e);
      reportError(new InternalParameterizationErrors("Error instantiating internal class: "+c.getName(), e));
      return null;
    }
  }

  @Override
  public <C> C tryInstantiate(Class<C> c) {
    try {
      return ClassGenericsUtil.tryInstantiate(c, c, this);
    }
    catch(Exception e) {
      LOG.exception(e);
      reportError(new InternalParameterizationErrors("Error instantiating internal class: "+c.getName(), e));
      return null;
    }
  }
}