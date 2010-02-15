package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying an integer value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class IntParameter extends NumberParameter<Integer> {
  /**
   * Constructs an integer parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID optionID the unique id of the option
   * @param constraints the constraint for this integer parameter
   * @param defaultValue the default value
   */
  public IntParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints, Integer defaultValue) {
    super(optionID, constraints, defaultValue);
  }

  /**
   * Constructs an integer parameter with the given optionID, parameter
   * constraint, and optional flag.
   * 
   * @param optionID optionID the unique id of the option
   * @param constraints the constraint for this integer parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public IntParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs an integer parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID optionID the unique id of the option
   * @param constraints the constraint for this integer parameter
   */
  public IntParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints) {
    super(optionID, constraints);
  }

  /**
   * Constructs an integer parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID optionID the unique id of the option
   * @param constraint the constraint for this integer parameter
   * @param defaultValue the default value
   */
  public IntParameter(OptionID optionID, ParameterConstraint<Number> constraint, Integer defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs an integer parameter with the given optionID, parameter
   * constraint, and optional flag.
   * 
   * @param optionID optionID the unique id of the option
   * @param constraint the constraint for this integer parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public IntParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs an integer parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID optionID the unique id of the option
   * @param constraint the constraint for this integer parameter
   */
  public IntParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   * @param defaultValue the default value
   */
  public IntParameter(OptionID optionID, Integer defaultValue) {
    super(optionID);
  }

  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   * @param optional specifies if this parameter is an optional parameter
   */
  public IntParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   */
  public IntParameter(OptionID optionID) {
    super(optionID);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return Integer.toString(getValue());
  }

  /** {@inheritDoc} */
  @Override
  protected Integer parseValue(Object obj) throws ParameterException {
    if(obj instanceof Integer) {
      return (Integer) obj;
    }
    try {
      return Integer.parseInt(obj.toString());
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a double value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;int&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<int>";
  }
}
