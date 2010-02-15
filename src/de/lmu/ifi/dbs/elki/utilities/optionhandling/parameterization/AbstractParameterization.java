package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
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
   * Handle default values for a parameter.
   * 
   * @param par Parameter
   * @return Return code: {@code true} if it has a default value, {@code false} if it is optional. 
   * @throws UnspecifiedParameterException If the parameter requires a value
   */
  protected boolean tryDefaultValue(Parameter<?, ?> par) throws UnspecifiedParameterException {
    // Assume default value instead.
    if(par.hasDefaultValue()) {
      logger.debugFinest("Fall-back to default values for parameter "+par.getName());
      par.useDefaultValue();
      return true;
    }
    else if(par.isOptional()) {
      // Optional is fine, but not successful
      return false;
    }
    else {
      throw new UnspecifiedParameterException("Parameter " + par.getName() + " requires parameter value.");
    }
  }
  
  /**
   * Log any error that has accumulated.
   */
  public synchronized void logAndClearReportedErrors() {
    for (ParameterException e : getErrors()) {
      logger.warning(e.getMessage());
    }
    errors.clear();
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
  
  @Override
  public final boolean grab(Object owner, Parameter<?,?> opt) {
    if(opt.isDefined()) {
      logger.warning("Option " + opt.getName() + " is already set!");
    }
    try {
      if (setValueForOption(owner, opt)) {
        return true;
      }
      // Try default value instead.
      if (tryDefaultValue(opt)) {
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
   * @param owner Owner object
   * @param opt Option to be set
   * @return Success code (value available)
   * @throws ParameterException
   */
  public abstract boolean setValueForOption(Object owner, Parameter<?,?> opt) throws ParameterException;
  
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