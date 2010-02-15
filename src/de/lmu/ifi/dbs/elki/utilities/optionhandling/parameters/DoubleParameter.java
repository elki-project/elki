package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a double value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class DoubleParameter extends NumberParameter<Double> {
  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and default value.
   * 
   * @param optionID the unique optionID
   * @param cons a list of parameter constraints for this double parameter
   * @param defaultValue the default value for this double parameter
   */
  public DoubleParameter(OptionID optionID, List<ParameterConstraint<Number>> cons, Double defaultValue) {
    super(optionID, cons, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraints, and optional flag.
   * 
   * @param optionID the unique optionID
   * @param cons a list of parameter constraints for this double parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DoubleParameter(OptionID optionID, List<ParameterConstraint<Number>> cons, boolean optional) {
    this(optionID, cons);
    setOptional(optional);
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraints.
   * 
   * @param optionID the unique optionID
   * @param constraints a list of parameter constraints for this double
   *        parameter
   */
  public DoubleParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints) {
    super(optionID, constraints);
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param defaultValue the default value for this parameter
   */
  public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint, Double defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   */
  public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param defaultValue the default value for this double parameter
   */
  public DoubleParameter(OptionID optionID, Double defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param optional specifies whether this parameter is an optional parameter
   */
  public DoubleParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public DoubleParameter(OptionID optionID) {
    super(optionID);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return Double.toString(getValue());
  }

  /** {@inheritDoc} */
  @Override
  protected Double parseValue(Object obj) throws WrongParameterValueException {
    if(obj instanceof Double) {
      return (Double) obj;
    }
    try {
      return Double.parseDouble(obj.toString());
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a double value, read: " + obj + "!\n");
    }
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * @param obj the reference object with which to compare.
   * @return <code>true</code> if this double parameter has the same value as
   *         the specified object, <code>false</code> otherwise.
   */
  // TODO: comparing the parameters doesn't make sense. REMOVE.
  /*@Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    }
    if(!(obj instanceof DoubleParameter)) {
      return false;
    }
    DoubleParameter oth = (DoubleParameter) obj;
    if(this.getValue() == null) {
      return (oth.getValue() == null);
    }
    return this.getValue().equals(oth.getValue());
  }*/

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;double&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<double>";
  }
}
