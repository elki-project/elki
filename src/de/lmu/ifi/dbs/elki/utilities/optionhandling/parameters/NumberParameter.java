package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;

/**
 * Abstract class for defining a number parameter.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class NumberParameter<T extends Number> extends Parameter<Number, T> {
  /**
   * Constructs a number parameter with the given optionID, constraint, and
   * optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter
   * @param defaultValue the default value for this parameter
   */
  public NumberParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints, T defaultValue) {
    super(optionID, constraints, defaultValue);
  }

  /**
   * Constructs a number parameter with the given optionID, constraint, and
   * optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraint of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public NumberParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a number parameter with the given optionID, and constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be empty if there
   *        are no constraints
   */
  public NumberParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints) {
    super(optionID, constraints);
  }

  /**
   * Constructs a number parameter with the given optionID, constraint, and
   * optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param defaultValue the default value for this parameter
   */
  public NumberParameter(OptionID optionID, ParameterConstraint<Number> constraint, T defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a number parameter with the given optionID, constraint, and
   * optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public NumberParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a number parameter with the given optionID, and constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   */
  public NumberParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a number parameter with the given optionID and default Value.
   * 
   * @param optionID the unique id of this parameter
   * @param defaultValue the default value for this parameter
   */
  public NumberParameter(OptionID optionID, T defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a number parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public NumberParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a number parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public NumberParameter(OptionID optionID) {
    super(optionID);
  }
}
