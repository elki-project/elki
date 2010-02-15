package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Global parameter constraint for specifying the default value of a parameter
 * dependent on the parameter value of another parameter.
 * 
 * @author Steffi Wanka
 * @param <T> Parameter type
 */
// FIXME: Does this actually work?
public class DefaultValueGlobalConstraint<T extends Comparable<T>> implements GlobalParameterConstraint {
  /**
   * Parameter to be set.
   */
  private Parameter<? super T, T> needsValue;

  /**
   * Parameter providing the value.
   */
  private Parameter<? super T, T> hasValue;

  /**
   * Creates a global parameter constraint for specifying the default value of a
   * parameter dependent on the value of an another parameter.
   * 
   * @param needsValue the parameter whose default value is to be set
   * @param hasValue the parameter providing the value
   */
  public DefaultValueGlobalConstraint(Parameter<? super T, T> needsValue, Parameter<? super T, T> hasValue) {
    this.needsValue = needsValue;
    this.hasValue = hasValue;
  }

  /**
   * Checks if the parameter providing the default value is already set, and if
   * the two parameters have the same parameter type. If so, the default value
   * of one parameter is set as the default value of the other parameter. If not
   * so, a parameter exception is thrown.
   * 
   */
  public void test() throws ParameterException {
    if(!hasValue.isDefined()) {
      throw new WrongParameterValueException("Parameter " + hasValue.getName() + " is currently not set but must be set!");
    }

    if(!hasValue.getClass().equals(needsValue.getClass())) {
      throw new WrongParameterValueException("Global Parameter Constraint Error!\n" + "Parameters " + hasValue.getName() + " and " + needsValue.getName() + "" + " must be of the same parameter type!");
    }

    if(!needsValue.isDefined()) {
      needsValue.setDefaultValue(hasValue.getValue());
      needsValue.useDefaultValue();
    }
  }

  public String getDescription() {
    return "If parameter " + needsValue.getName() + " is not specified, " + " its value will be set to the value of parameter " + hasValue.getName();
  }
}
