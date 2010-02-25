package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameterization handler that only allows the use of default values.
 * 
 * @author Erich Schubert
 */
public class EmptyParameterization extends AbstractParameterization {  
  @Override
  public boolean hasUnusedParameters() {
    return false;
  }

  @SuppressWarnings("unused")
  @Override
  public boolean setValueForOption(Object owner, Parameter<?,?> opt) throws ParameterException {
    // Always return false, we don't have extra parameters,
    // This will cause {@link AbstractParameterization} to use the default values
    return false;
  }

  /** {@inheritDoc}
   * Default implementation, for flat parameterizations. 
   */
  @Override
  public Parameterization descend(@SuppressWarnings("unused") Parameter<?, ?> option) {
    return this;
  }
}