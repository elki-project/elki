package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Interface for object parameterizations.
 * 
 * @author Erich Schubert
 */
public interface Parameterization {
  /**
   * Get the option value from the Parameterization.
   * 
   * Note: this method returns success; the actual value can be obtained from
   * {@code opt} itself!
   * 
   * In particular {@link grab(Flag foo)} can return {@code true} when
   * foo.isSet() is {@code false}!
   * 
   * This method will catch {@link ParameterException}s and store them to be
   * retrieved by {@link #getErrors}.
   * 
   * @param owner Owner of the option.
   * @param opt Option to add
   * @return if the value is available (= readable)
   */
  public boolean grab(Object owner, Parameter<?,?> opt);

  /**
   * Assign a value for an option, but not using default values and throwing
   * exceptions on error.
   * 
   * @param owner Owner of the option.
   * @param opt Parameter to set
   * @return Success code
   * @throws ParameterException on assignment errors.
   */
  public boolean setValueForOption(Object owner, Parameter<?,?> opt) throws ParameterException;

  /**
   * Get the configuration errors thrown in {@link #grab}
   * 
   * @return Configuration errors encountered
   */
  public Collection<ParameterException> getErrors();

  /**
   * Report a configuration error.
   * 
   * @param e
   */
  public void reportError(ParameterException e);

  /**
   * Check for unused parameters
   */
  public boolean hasUnusedParameters();

  /**
   * Check a parameter constraint.
   * 
   * @param constraint Parameter constraint
   * @return test result
   */
  public boolean checkConstraint(GlobalParameterConstraint constraint);
}