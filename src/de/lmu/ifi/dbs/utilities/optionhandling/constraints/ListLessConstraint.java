package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Represents a Less-Than-Number parameter constraint for a list of number values.
 * All values of the list parameter ({@link de.lmu.ifi.dbs.utilities.optionhandling.ListParameter})
 * tested have to be less than the specified constraint value.
 *
 * @author Elke Achtert
 */
public class ListLessConstraint extends AbstractNumberConstraint<List<Number>> {
  /**
   * Creates a Less-Than-Number parameter constraint.
   * <p/>
   * That is, all values of the list parameter
   * tested have to be less than the specified constraint value.
   *
   * @param constraintValue parameter constraint value
   */
  public ListLessConstraint(Number constraintValue) {
    super(constraintValue);
  }

  /**
   * Checks if all number values of the specified list parameter
   * are less than the constraint value.
   * If not, a parameter exception is thrown.
   *
   * @see ParameterConstraint#test(Object)
   */
  public void test(List<Number> t) throws ParameterException {
    for (Number n : t) {
      if (n.doubleValue() >= constraintValue.doubleValue()) {
        throw new WrongParameterValueException("Parameter Constraint Error: \n"
                                               + "The parameter values specified have to be less than " + constraintValue.toString()
                                               + ". (current value: " + t + ")\n");
      }
    }
  }

}
