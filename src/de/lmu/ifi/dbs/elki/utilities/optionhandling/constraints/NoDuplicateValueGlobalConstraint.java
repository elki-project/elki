package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Global parameter constraint specifying that parameters of a list of number
 * parameters ({@link NumberParameter}) are not allowed to have the same value.
 * 
 * @author Steffi Wanka
 */
public class NoDuplicateValueGlobalConstraint implements GlobalParameterConstraint {
  /**
   * List of number parameters to be checked.
   */
  private List<? extends Parameter<?, ?>> parameters;

  /**
   * Constructs a Not-Equal-Value global parameter constraint. That is, the
   * elements of a list of number parameters are not allowed to have equal
   * values.
   * 
   * @param parameters list of number parameters to be tested
   */
  public NoDuplicateValueGlobalConstraint(List<? extends Parameter<?, ?>> parameters) {
    this.parameters = parameters;
  }

  /**
   * Constructs a Not-Equal-Value global parameter constraint. That is, the
   * elements of a list of number parameters are not allowed to have equal
   * values.
   * 
   * @param parameters list of number parameters to be tested
   */
  public NoDuplicateValueGlobalConstraint(Parameter<?, ?>... parameters) {
    this.parameters = Arrays.asList(parameters);
  }

  /**
   * Checks if the elements of the list of number parameters do have different
   * values. If not, a parameter exception is thrown.
   * 
   */
  @Override
  public void test() throws ParameterException {
    Set<Object> numbers = new HashSet<Object>();

    for(Parameter<?, ?> param : parameters) {
      if(param.isDefined()) {
        if(!numbers.add(param.getValue())) {
          throw new WrongParameterValueException("Global Parameter Constraint Error:\n" + "Parameters " + OptionUtil.optionsNamesToString(parameters) + " must have different values. Current values: " + OptionUtil.parameterNamesAndValuesToString(parameters) + ".\n");
        }
      }
    }
  }

  @Override
  public String getDescription() {
    return "Parameters " + OptionUtil.optionsNamesToString(parameters) + " must have different values.";
  }
}