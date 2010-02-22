package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameterization handler that doesn't set any parameters.
 * 
 * This is mostly useful for documentation purposes, listing all parameters
 * in a non-recursive way.
 * 
 * @author Erich Schubert
 */
public class UnParameterization implements Parameterization {  
  /**
   * Errors
   */
  java.util.Vector<ParameterException> errors = new java.util.Vector<ParameterException>();

  @Override
  public boolean hasUnusedParameters() {
    return false;
  }

  @Override
  @SuppressWarnings("unused")
  public boolean checkConstraint(GlobalParameterConstraint constraint) {
    return false;
  }

  @Override
  public Collection<ParameterException> getErrors() {
    return errors;
  }

  @Override
  @SuppressWarnings("unused")
  public boolean grab(Object owner, Parameter<?, ?> opt) {
    return false;
  }

  @Override
  public void reportError(ParameterException e) {
    errors.add(e);
  }

  @Override
  @SuppressWarnings("unused")
  public boolean setValueForOption(Object owner, Parameter<?, ?> opt) throws ParameterException {
    return false;
  }
}