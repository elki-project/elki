package de.lmu.ifi.dbs.utilities.optionhandling;

import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;
import java.util.Vector;

/**
 * Holds a parameter object, i.e. an option holding a specific value.
 *
 * @author Steffi Wanka
 * @param <T>
 * @param <O>
 */
public abstract class Parameter<T,O> extends Option<T> {


  /**
   * The default value of the parameter (may be null).
   */
  protected T defaultValue;

  /**
   * Specifies if the default value of this parameter was taken as parameter value.
   */
  private boolean defaultValueTaken;

  /**
   * Specifies if this parameter is an optional parameter.
   */
  protected boolean optionalParameter;

  /**
   * Holds parameter constraints for this parameter.
   */
  protected List<ParameterConstraint<O>> constraints;

  /**
   * Constructs a parameter with the given name and description.
   *
   * @param name        the parameter name
   * @param description the parameter description
   */
  public Parameter(String name, String description) {
    super(name, description);
    constraints = new Vector<ParameterConstraint<O>>();
    optionalParameter = false;
    defaultValueTaken = false;
  }

  /**
   * Adds a parameter constraint to the list of parameter constraints.
   *
   * @param constraint the parameter constraint to be added
   */
  protected void addConstraint(ParameterConstraint<O> constraint) {
    constraints.add(constraint);
  }

  /**
   * Adds a list of parameter constraints to the current list of parameter constraints.
   *
   * @param constraints list of parameter constraints to be added
   */
  protected void addConstraintList(List<ParameterConstraint<O>> constraints) {
    this.constraints.addAll(constraints);
  }

  /**
   * Sets the default value of this parameter.
   *
   * @param defaultValue default value of this parameter
   */
  public void setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Checks if this parameter has a default value.
   *
   * @return true, if this parameter has a default value, false otherwise
   */
  public boolean hasDefaultValue() {
    return !(defaultValue == null);
  }

  /**
   * Sets the default value of this parameter as the actual value of this parameter.
   */
  public void setDefaultValueToValue() {
    this.value = defaultValue;
    defaultValueTaken = true;
  }

  /**
   * Specifies if this parameter is an optional parameter.
   *
   * @param opt true if this parameter is optional,false otherwise
   */
  public void setOptional(boolean opt) {
    this.optionalParameter = opt;
  }

  /**
   * Checks if this parameter is an optional parameter.
   *
   * @return true if this parameter is optional, false otherwise
   */
  public boolean isOptional() {
    return this.optionalParameter;
  }

  /**
   * Checks if the default value of this parameter was taken as the actual parameter value.
   *
   * @return true, if the default value was taken as actual parameter value, false otherwise
   */
  public boolean tookDefaultValue() {
    return defaultValueTaken;
  }

  /* (non-Javadoc)
    * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#isSet()
    */
  public boolean isSet() {
    return (value != null);
  }

  /* (non-Javadoc)
    * @see de.lmu.ifi.dbs.utilities.optionhandling.Option#getValue()
    */
  public T getValue() throws UnusedParameterException {
    if (value == null && !optionalParameter)
      throw new UnusedParameterException("Parameter " + name + " is not specified " +
                                         "or not assigned to the option handler!");

    return value;
  }
}
