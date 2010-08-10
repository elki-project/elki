package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.InternalParameterizationErrors;
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
  public boolean hasErrors() {
    return errors.size() > 0;
  }

  @Override
  @SuppressWarnings("unused")
  public boolean grab(Parameter<?, ?> opt) {
    return false;
  }

  @Override
  public void reportError(ParameterException e) {
    errors.add(e);
  }

  @Override
  @SuppressWarnings("unused")
  public boolean setValueForOption(Parameter<?, ?> opt) throws ParameterException {
    return false;
  }

  @Override
  public Parameterization descend(@SuppressWarnings("unused") Object option) {
    return this;
  }
  
  @Override
  public <C> C tryInstantiate(Class<C> r, Class<?> c) {
    try {
      return ClassGenericsUtil.tryInstantiate(r, c, this);
    }
    catch(Exception e) {
      reportError(new InternalParameterizationErrors("Error instantiating internal class.", e));
      return null;
    }
  }
}