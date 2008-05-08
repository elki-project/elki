package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Parameter class for a parameter specifying a double value.
 *
 * @author Steffi Wanka
 */
public class DoubleParameter extends NumberParameter<Double> {

  /**
   * Constructs a double parameter with the given name and description
   *
   * @param name        the parameter name
   * @param description the parameter description
   */
  public DoubleParameter(String name, String description) {
    super(name, description);

  }

  /**
   * Constructs a double parameter with the given name, description, and parameter constraint.
   *
   * @param name        the parameter name
   * @param description the parameter description
   * @param cons        the constraint for this double parameter
   */
  public DoubleParameter(String name, String description, ParameterConstraint<Number> cons) {
    this(name, description);
    addConstraint(cons);
  }

  /**
   * Constructs a double parameter with the given name, description, and list of parameter constraints.
   *
   * @param name        the parameter name
   * @param description the parameter description
   * @param cons        a list of parameter constraints for this double parameter
   */
  public DoubleParameter(String name, String description, List<ParameterConstraint<Number>> cons) {
    this(name, description);
    addConstraintList(cons);
  }

  /**
   * @see Option#setValue(String)
   */
  public void setValue(String value) throws ParameterException {
    if (isValid(value)) {
      this.value = Double.parseDouble(value);
    }
  }

  /**
   * @see Option#isValid(String)
   */
  public boolean isValid(String value) throws ParameterException {
    try {
      Double.parseDouble(value);
    }

    catch (NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \""
                                             + getName() + "\" requires a double value, read: " + value + "!\n");
    }

    try {
      for (ParameterConstraint<Number> cons : this.constraints) {

        cons.test(Double.parseDouble(value));
      }
    }
    catch (ParameterException ex) {
      throw new WrongParameterValueException("Specified parameter value for parameter \""
                                             + getName() + "\" breaches parameter constraint!\n" + ex.getMessage());
    }

    return true;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param obj the reference object with which to compare.
   * @return <code>true</code> if this double parameter has the same
   *         value as the specified object, <code>false</code> otherwise.
   */
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DoubleParameter)) {
      return false;
    }
    return this.value.equals(((DoubleParameter)obj).value);
	}
}
