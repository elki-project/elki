package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;

/**
 * Global parameter constraint defining that a number of list parameters (
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter}
 * ) must have equal list sizes.
 * 
 * @author Steffi Wanka
 */
public class EqualSizeGlobalConstraint implements GlobalParameterConstraint {
  /**
   * List parameters to be tested
   */
  private List<ListParameter<?>> parameters;

  /**
   * Creates a global parameter constraint for testing if a number of list
   * parameters have equal list sizes.
   * 
   * @param params list parameters to be tested for equal list sizes
   */
  public EqualSizeGlobalConstraint(List<ListParameter<?>> params) {
    this.parameters = params;
  }

  /**
   * Checks if the list parameters have equal list sizes. If not, a parameter
   * exception is thrown.
   * 
   */
  public void test() throws ParameterException {
    boolean first = false;
    int constraintSize = -1;

    for(ListParameter<?> listParam : parameters) {
      if(listParam.isDefined()) {
        if(!first) {
          constraintSize = listParam.getListSize();
          first = true;
        }
        else if(constraintSize != listParam.getListSize()) {
          throw new WrongParameterValueException("Global constraint errror.\n" + "The list parameters " + OptionUtil.optionsNamesToString(parameters) + " must have equal list sizes.");
        }
      }
    }
  }

  public String getDescription() {
    return "The list parameters " + OptionUtil.optionsNamesToString(parameters) + " must have equal list sizes.";
  }
}
