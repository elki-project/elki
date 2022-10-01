/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.utilities.optionhandling.parameters;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import elki.logging.LoggingUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.ParameterException;
import elki.utilities.optionhandling.UnspecifiedParameterException;
import elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Abstract class for specifying a parameter.
 * <p>
 * A parameter is defined as an option having a specific value.
 * <p>
 * See the {@link elki.utilities.optionhandling} package for documentation!
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 * 
 * @composed - - - OptionID
 * @assoc - - - ParameterConstraint
 * 
 * @param <P> type self-reference
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class AbstractParameter<P extends AbstractParameter<P, T>, T> implements Parameter<T> {
  /**
   * The option name.
   */
  protected final OptionID optionid;

  /**
   * The default value of the parameter (may be null).
   */
  protected T defaultValue = null;

  /**
   * Whether the default value of this parameter was used.
   */
  private boolean defaultValueTaken = false;

  /**
   * Specifies if this parameter is an optional parameter.
   */
  protected boolean optionalParameter = false;

  /**
   * Holds parameter constraints for this parameter.
   */
  protected List<ParameterConstraint<? super T>> constraints;

  /**
   * The value last passed to this option.
   */
  protected T givenValue = null;

  /**
   * The value of this option.
   */
  private T value;

  /**
   * Constructs a parameter with the given optionID, constraints, and default
   * value.
   * 
   * @param optionID the unique id of this parameter
   * @param defaultValue the default value of this parameter (may be null)
   */
  public AbstractParameter(OptionID optionID, T defaultValue) {
    this.optionid = optionID;
    this.optionalParameter = true;
    this.defaultValue = defaultValue;
  }

  /**
   * Constructs a parameter with the given optionID, and constraints.
   * 
   * @param optionID the unique id of this parameter
   */
  public AbstractParameter(OptionID optionID) {
    this.optionid = optionID;
    this.optionalParameter = false;
    this.defaultValue = null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public P setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    this.optionalParameter = true;
    return (P) this;
  }

  @Override
  public boolean hasDefaultValue() {
    return defaultValue != null;
  }

  @Override
  public boolean tryDefaultValue() throws UnspecifiedParameterException {
    // Assume default value instead.
    if(hasDefaultValue()) {
      setValueInternal(defaultValue);
      defaultValueTaken = true;
      return true;
    }
    if(isOptional()) {
      // Optional is fine, but not successful
      return false;
    }
    // A missing value is an error.
    throw new UnspecifiedParameterException(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public P setOptional(boolean opt) {
    this.optionalParameter = opt;
    return (P) this;
  }

  @Override
  public boolean isOptional() {
    return this.optionalParameter;
  }

  @Override
  public boolean tookDefaultValue() {
    return defaultValueTaken;
  }

  @Override
  public boolean isDefined() {
    return (this.value != null);
  }

  @Override
  public T getDefaultValue() {
    return defaultValue;
  }

  @Override
  public StringBuilder describeValues(StringBuilder description) {
    return description;
  }

  /**
   * Validate a value after parsing (e.g., do constraint checks!)
   * 
   * @param obj Object to validate
   * @return true iff the object is valid for this parameter.
   * @throws ParameterException when the object is not valid.
   */
  protected boolean validate(T obj) throws ParameterException {
    if(constraints != null) {
      for(ParameterConstraint<? super T> cons : this.constraints) {
        cons.test(obj);
      }
    }
    return true;
  }

  @Override
  public OptionID getOptionID() {
    return optionid;
  }

  @Override
  public void setValue(Object obj) throws ParameterException {
    if(obj != null) {
      T val = parseValue(obj);
      if(validate(val)) {
        setValueInternal(val);
        return;
      }
    }
    throw new InvalidParameterException("Value for option \"" + getOptionID().getName() + "\" did not validate: " + obj);
  }

  /**
   * Internal setter for the value.
   * 
   * @param val Value
   */
  protected void setValueInternal(T val) {
    this.value = this.givenValue = val;
  }

  @Override
  public final T getValue() {
    if(this.value == null) {
      LoggingUtil.warning("Programming error: Parameter#getValue() called for unset parameter \"" + getOptionID().getName() + "\"", new Throwable());
    }
    return this.value;
  }

  @Override
  public final boolean isValid(Object obj) throws ParameterException {
    return obj != null && validate(parseValue(obj));
  }

  /**
   * Parse a given value into the destination type.
   * 
   * @param obj Object to parse (may be a string representation!)
   * @return Parsed object
   * @throws ParameterException when the object cannot be parsed.
   */
  protected abstract T parseValue(Object obj) throws ParameterException;

  @Override
  public String getDefaultValueAsString() {
    return getDefaultValue().toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  public P addConstraint(ParameterConstraint<? super T> constraint) {
    (constraints != null ? constraints : (constraints = new ArrayList<>(2))).add(constraint);
    return (P) this;
  }

  @Override
  public List<ParameterConstraint<? super T>> getConstraints() {
    return this.constraints != null ? Collections.unmodifiableList(this.constraints) : Collections.emptyList();
  }
}
