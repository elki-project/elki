package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Abstract class for specifying a parameter.
 * 
 * A parameter is defined as an option having a specific value.
 * 
 * See the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling} package for
 * documentation!
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * 
 * @apiviz.composedOf OptionID
 * @apiviz.uses ParameterConstraint
 * 
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class AbstractParameter<T> implements Parameter<T> {
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
  protected List<ParameterConstraint<? super T>> constraints;

  /**
   * The option name.
   */
  protected final OptionID optionid;

  /**
   * The short description of the option. An extended description is provided by
   * the method {@link #getFullDescription()}
   */
  protected String shortDescription;

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
    this.shortDescription = optionID.getDescription();
    this.optionalParameter = true;
    this.defaultValue = defaultValue;
  }

  /**
   * Constructs a parameter with the given optionID, constraints, and optional
   * flag.
   * 
   * @param optionID the unique id of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public AbstractParameter(OptionID optionID, boolean optional) {
    this.optionid = optionID;
    this.shortDescription = optionID.getDescription();
    this.optionalParameter = optional;
    this.defaultValue = null;
  }

  /**
   * Constructs a parameter with the given optionID, and constraints.
   * 
   * @param optionID the unique id of this parameter
   */
  public AbstractParameter(OptionID optionID) {
    this(optionID, false);
  }

  @Override
  public void setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    this.optionalParameter = true;
  }

  @Override
  public boolean hasDefaultValue() {
    return !(defaultValue == null);
  }

  // TODO: can we do this more elegantly?
  @Override
  public void useDefaultValue() {
    setValueInternal(defaultValue);
    defaultValueTaken = true;
  }

  @Override
  public boolean tryDefaultValue() throws UnspecifiedParameterException {
    // Assume default value instead.
    if (hasDefaultValue()) {
      useDefaultValue();
      return true;
    } else if (isOptional()) {
      // Optional is fine, but not successful
      return false;
    } else {
      throw new UnspecifiedParameterException(this);
    }
  }

  @Override
  public void setOptional(boolean opt) {
    this.optionalParameter = opt;
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
  public boolean hasValuesDescription() {
    return false;
  }

  @Override
  public String getValuesDescription() {
    return "";
  }

  @Override
  public String getFullDescription() {
    StringBuilder description = new StringBuilder();
    // description.append(getParameterType()).append(" ");
    description.append(shortDescription);
    description.append(FormatUtil.NEWLINE);
    if (hasValuesDescription()) {
      final String valuesDescription = getValuesDescription();
      description.append(valuesDescription);
      if (!valuesDescription.endsWith(FormatUtil.NEWLINE)) {
        description.append(FormatUtil.NEWLINE);
      }
    }
    if (hasDefaultValue()) {
      description.append("Default: ");
      description.append(getDefaultValueAsString());
      description.append(FormatUtil.NEWLINE);
    }
    if (constraints != null && !constraints.isEmpty()) {
      if (constraints.size() == 1) {
        description.append("Constraint: ");
      } else if (constraints.size() > 1) {
        description.append("Constraints: ");
      }
      for (int i = 0; i < constraints.size(); i++) {
        ParameterConstraint<? super T> constraint = constraints.get(i);
        if (i > 0) {
          description.append(", ");
        }
        description.append(constraint.getDescription(getName()));
        if (i == constraints.size() - 1) {
          description.append('.');
        }
      }
      description.append(FormatUtil.NEWLINE);
    }
    return description.toString();
  }

  /**
   * Validate a value after parsing (e.g. do constrain checks!)
   * 
   * @param obj Object to validate
   * @return true iff the object is valid for this parameter.
   * @throws ParameterException when the object is not valid.
   */
  protected boolean validate(T obj) throws ParameterException {
    if (constraints != null) {
      for (ParameterConstraint<? super T> cons : this.constraints) {
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
  public String getName() {
    return optionid.getName();
  }

  @Override
  public String getShortDescription() {
    return shortDescription;
  }

  @Override
  public void setShortDescription(String description) {
    this.shortDescription = description;
  }

  @Override
  public void setValue(Object obj) throws ParameterException {
    T val = parseValue(obj);
    if (validate(val)) {
      setValueInternal(val);
    } else {
      throw new InvalidParameterException("Value for option \"" + getName() + "\" did not validate: " + obj.toString());
    }
  }

  /**
   * Internal setter for the value.
   * 
   * @param val Value
   */
  protected final void setValueInternal(T val) {
    this.value = this.givenValue = val;
  }

  @Override
  public final T getValue() {
    if (this.value == null) {
      LoggingUtil.warning("Programming error: Parameter#getValue() called for unset parameter \"" + this.optionid.getName() + "\"", new Throwable());
    }
    return this.value;
  }

  @Override
  public Object getGivenValue() {
    return this.givenValue;
  }

  @Override
  public final boolean isValid(Object obj) throws ParameterException {
    T val = parseValue(obj);
    return validate(val);
  }

  @Override
  public abstract String getSyntax();

  /**
   * Parse a given value into the destination type.
   * 
   * @param obj Object to parse (may be a string representation!)
   * @return Parsed object
   * @throws ParameterException when the object cannot be parsed.
   */
  protected abstract T parseValue(Object obj) throws ParameterException;

  @Override
  public abstract String getValueAsString();

  @Override
  public String getDefaultValueAsString() {
    return getDefaultValue().toString();
  }

  @Override
  public void addConstraint(ParameterConstraint<? super T> constraint) {
    if (constraints == null) {
      this.constraints = new ArrayList<ParameterConstraint<? super T>>(1);
    }
    constraints.add(constraint);
  }

  /**
   * Add a collection of constraints.
   * 
   * @param constraints Constraints to add
   */
  public void addConstraints(Collection<? extends ParameterConstraint<? super T>> cs) {
    if (constraints == null) {
      this.constraints = new ArrayList<ParameterConstraint<? super T>>(cs.size());
    }
    constraints.addAll(cs);
  }
}
