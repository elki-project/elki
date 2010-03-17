package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.InternalParameterizationErrors;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Abstract class with shared code for parameterization handling.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractParameterization extends AbstractLoggable implements Parameterization {
  /**
   * Errors
   */
  java.util.Vector<ParameterException> errors = new java.util.Vector<ParameterException>();

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<ParameterException> getErrors() {
    return errors;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reportError(ParameterException e) {
    errors.add(e);
  }

  /**
   * Log any error that has accumulated.
   */
  public synchronized void logAndClearReportedErrors() {
    for (ParameterException e : getErrors()) {
      logger.warning(e.getMessage());
    }
    clearErrors();
  }
  
  /**
   * Clear errors.
   */
  public synchronized void clearErrors() {
    // Do NOT use errors.clear(), since we might have an error report referencing the collection!
    errors = new java.util.Vector<ParameterException>();
  }
  
  /**
   * Fail on errors, log any error that had occurred.
   * 
   * @throws RuntimeException if any error has occurred.
   */
  // TODO: make a multi-exception class?
  public void failOnErrors() throws RuntimeException {
    final int numerror = getErrors().size();
    if (numerror > 0) {
      logAndClearReportedErrors();
      throw new RuntimeException(numerror + " errors occurred during parameterization.");
    }
  }
  
  /**
   * Report the internal parameterization errors to another parameterization
   * 
   * @param config Other parameterization
   */
  public synchronized void reportInternalParameterizationErrors(Parameterization config) {
    final int numerror = getErrors().size();
    if (numerror > 0) {
      config.reportError(new InternalParameterizationErrors(numerror + " internal (re-) parameterization errors prevented execution.", getErrors()));
      this.clearErrors();
    }
  }
  
  @Override
  public final boolean grab(Parameter<?,?> opt) {
    if(opt.isDefined()) {
      logger.warning("Option " + opt.getName() + " is already set!");
    }
    try {
      if (setValueForOption(opt)) {
        return true;
      }
      // Try default value instead.
      if (opt.tryDefaultValue()) {
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
  public abstract boolean setValueForOption(Parameter<?,?> opt) throws ParameterException;
  
  /** Upon destruction, report any errors that weren't handled yet. */
  @Override
  public void finalize() {
    failOnErrors();
  }

  /** {@inheritDoc} */
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
}