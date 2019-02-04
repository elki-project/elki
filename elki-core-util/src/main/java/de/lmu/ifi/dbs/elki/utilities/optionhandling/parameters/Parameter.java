/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Interface for the parameter of a class.
 * 
 * A parameter is defined as an option having a specific value.
 * 
 * See the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling} package for
 * documentation!
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 * 
 * @composed - - - OptionID
 * @assoc - - - ParameterConstraint
 * 
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public interface Parameter<T> {
  /**
   * Sets the default value of this parameter.
   *
   * @param defaultValue default value of this parameter
   * @return the parameter itself, for chaining
   */
  Parameter<T> setDefaultValue(T defaultValue);

  /**
   * Checks if this parameter has a default value.
   *
   * @return true, if this parameter has a default value, false otherwise
   */
  boolean hasDefaultValue();

  /**
   * Handle default values for a parameter.
   *
   * @return Return code: {@code true} if it has a default value, {@code false}
   *         if it is optional without a default value. Exception if it is a
   *         required parameter!
   * @throws UnspecifiedParameterException If the parameter requires a value
   */
  boolean tryDefaultValue() throws UnspecifiedParameterException;

  /**
   * Specifies if this parameter is an optional parameter.
   *
   * @param opt true if this parameter is optional, false otherwise
   * @return the parameter itself, for chaining
   */
  Parameter<T> setOptional(boolean opt);

  /**
   * Checks if this parameter is an optional parameter.
   *
   * @return true if this parameter is optional, false otherwise
   */
  boolean isOptional();

  /**
   * Checks if the default value of this parameter was taken as the actual
   * parameter value.
   *
   * @return true, if the default value was taken as actual parameter value,
   *         false otherwise
   */
  boolean tookDefaultValue();

  /**
   * Returns true if the value of the option is defined, false otherwise.
   *
   * @return true if the value of the option is defined, false otherwise.
   */
  boolean isDefined();

  /**
   * Returns the default value of the parameter.
   * <p>
   * If the parameter has no default value, the method returns <b>null</b>.
   *
   * @return the default value of the parameter, <b>null</b> if the parameter
   *         has no default value.
   */
  // TODO: change this to return a string value?
  T getDefaultValue();

  /**
   * Return the OptionID of this option.
   *
   * @return Option ID
   */
  OptionID getOptionID();

  /**
   * Returns the short description of the option.
   *
   * @return the option's short description.
   */
  String getShortDescription();

  /**
   * Sets the short description of the option.
   *
   * @param description the short description to be set
   */
  void setShortDescription(String description);

  /**
   * Sets the value of the option.
   *
   * @param obj the option's value to be set
   * @throws ParameterException if the given value is not a valid value for this
   *         option.
   */
  void setValue(Object obj) throws ParameterException;

  /**
   * Returns the value of the option.
   *
   * You should use either
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization#grab}
   * or {@link #isDefined} to test if getValue() will return a well-defined
   * value.
   *
   * @return the option's value.
   */
  T getValue();

  /**
   * Checks if the given argument is valid for this option.
   * 
   * @param obj option value to be checked
   * @return true, if the given value is valid for this option
   * @throws ParameterException if the given value is not a valid value for this
   *         option.
   */
  boolean isValid(Object obj) throws ParameterException;

  /**
   * Returns a string representation of the parameter's type (e.g. an
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter}
   * should return {@code <int>}).
   *
   * @return a string representation of the parameter's type
   */
  String getSyntax();

  /**
   * Get the value as string. May return {@code null}
   *
   * @return Value as string
   */
  String getValueAsString();

  /**
   * Get the default value as string.
   *
   * @return default value
   */
  String getDefaultValueAsString();

  /**
   * Describe the valid values.
   *
   * @param description Buffer to append to
   * @return Buffer
   */
  StringBuilder describeValues(StringBuilder description);

  /**
   * Add an additional constraint.
   *
   * @param constraint Constraint to add.
   * @return the parameter itself, for chaining
   */
  Parameter<T> addConstraint(ParameterConstraint<? super T> constraint);

  /**
   * Get the parameter constraints.
   *
   * @return Parameter constraints
   */
  List<ParameterConstraint<? super T>> getConstraints();
}
