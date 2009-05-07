package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Abstract class for specifying a parameter.
 * <p/>
 * A parameter is defined as an option having a specific value.
 * </p>
 * 
 * @author Steffi Wanka
 * @param <T> the type of a possible value (i.e., the type of the option)
 * @param <C> the type of a possible parameter constraint
 */
public abstract class Parameter<T, C> extends Option<T> {
  /**
   * The default value of the parameter (may be null).
   */
  protected T defaultValue = null;

  /**
   * Specifies if the default value of this parameter was taken as parameter
   * value.
   */
  private boolean defaultValueTaken = false;

  /**
   * Specifies if this parameter is an optional parameter.
   */
  protected boolean optionalParameter = false;

  /**
   * Holds parameter constraints for this parameter.
   */
  protected final List<ParameterConstraint<C>> constraints = new Vector<ParameterConstraint<C>>();

  /**
   * Constructs a parameter with the given optionID, and constraints.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be empty if there
   *        are no constraints
   */
  public Parameter(OptionID optionID, List<ParameterConstraint<C>> constraints) {
    super(optionID);

    // constraints
    if(constraints != null && !constraints.isEmpty()) {
      this.constraints.addAll(constraints);
    }
  }

  /**
   * Constructs a parameter with the given optionID, and constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   */
  public Parameter(OptionID optionID, ParameterConstraint<C> constraint) {
    super(optionID);

    // constraints
    if(constraint != null) {
      this.constraints.add(constraint);
    }
  }

  /**
   * Constructs a parameter with the given optionID, constraints, and optional
   * flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be empty if there
   *        are no constraints
   * @param optional specifies if this parameter is an optional parameter
   * @param defaultValue the default value of this parameter (may be null)
   */
  public Parameter(OptionID optionID, List<ParameterConstraint<C>> constraints, boolean optional, T defaultValue) {
    this(optionID, constraints);
    // optional
    this.optionalParameter = optional;
    // defaultValue
    this.defaultValue = defaultValue;
  }

  /**
   * Constructs a parameter with the given optionID, constraint, and optional
   * flag.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @param optional specifies if this parameter is an optional parameter
   * @param defaultValue the default value of this parameter (may be null)
   */
  public Parameter(OptionID optionID, ParameterConstraint<C> constraint, boolean optional, T defaultValue) {
    this(optionID, constraint);
    // optional
    this.optionalParameter = optional;
    // defaultValue
    this.defaultValue = defaultValue;
  }

  /**
   * Constructs a parameter with the given optionID.
   * 
   * @param optionID the unique id of the option
   */
  @SuppressWarnings("unchecked")
  public Parameter(OptionID optionID) {
    this(optionID, Collections.EMPTY_LIST);
  }

  /**
   * Adds a parameter constraint to the list of parameter constraints.
   * 
   * @param constraint the parameter constraint to be added
   */
  // todo private setzen
  protected void addConstraint(ParameterConstraint<C> constraint) {
    constraints.add(constraint);
  }

  /**
   * Adds a list of parameter constraints to the current list of parameter
   * constraints.
   * 
   * @param constraints list of parameter constraints to be added
   */
  // todo remove
  protected void addConstraintList(List<ParameterConstraint<C>> constraints) {
    this.constraints.addAll(constraints);
  }

  /**
   * Sets the default value of this parameter.
   * 
   * @param defaultValue default value of this parameter
   */
  // todo remove
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
   * Sets the default value of this parameter as the actual value of this
   * parameter.
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
   * Checks if the default value of this parameter was taken as the actual
   * parameter value.
   * 
   * @return true, if the default value was taken as actual parameter value,
   *         false otherwise
   */
  public boolean tookDefaultValue() {
    return defaultValueTaken;
  }

  @Override
  public boolean isSet() {
    return (value != null);
  }

  @Override
  public T getValue() throws UnusedParameterException {
    if(value == null && !optionalParameter) {
      throw new UnusedParameterException("Value of parameter " + getName() + " has not been specified.");
    }

    return value;
  }

  /**
   * Returns the default value of the parameter.
   * <p/>
   * If the parameter has no default value, the method returns <b>null</b>.
   * 
   * @return the default value of the parameter, <b>null</b> if the parameter
   *         has no default value.
   */
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   * Whether this class has a list of default values.
   * 
   * @return whether the class has a description of valid values.
   */
  public boolean hasValuesDescription() {
    return false;
  }

  /**
   * Return a string explaining valid values.
   * 
   * @return String explaining valid values (e.g. a class list)
   */
  public String getValuesDescription() {
    return "";
  }

  /**
   * Resets the value of the parameter to null.
   */
  public void reset() {
    this.value = null;
  }

  /**
   * @return the option's description.
   */
  @Override
  public final String getFullDescription() {
    StringBuffer description = new StringBuffer();
    // description.append(getParameterType()).append(" ");
    description.append(shortDescription);
    description.append(FormatUtil.NEWLINE);
    if(hasValuesDescription()) {
      description.append(getValuesDescription());
    }
    if(hasDefaultValue()) {
      description.append("Default: ").append(getDefaultValue().toString()).append("." + FormatUtil.NEWLINE);
    }
    if(!constraints.isEmpty()) {
      if(constraints.size() == 1) {
        description.append("Constraint: ");
      }
      else if(constraints.size() > 1) {
        description.append("Constraints: ");
      }
      for(int i = 0; i < constraints.size(); i++) {
        ParameterConstraint<C> constraint = constraints.get(i);
        if(i > 0) {
          description.append(", ");
        }
        description.append(constraint.getDescription(getName()));
        if(i == constraints.size() - 1) {
          description.append(".");
        }
      }
      description.append(FormatUtil.NEWLINE);
    }
    return description.toString();
  }
}
