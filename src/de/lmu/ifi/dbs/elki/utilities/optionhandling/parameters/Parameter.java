package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

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
 * 
 * @apiviz.composedOf OptionID
 * @apiviz.uses ParameterConstraint
 * 
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public interface Parameter<T> {
  /**
   * Sets the default value of this parameter.
   * 
   * @param defaultValue default value of this parameter
   */
  public abstract void setDefaultValue(T defaultValue);

  /**
   * Checks if this parameter has a default value.
   * 
   * @return true, if this parameter has a default value, false otherwise
   */
  public abstract boolean hasDefaultValue();

  /**
   * Sets the default value of this parameter as the actual value of this
   * parameter.
   */
  // TODO: can we do this more elegantly?
  public abstract void useDefaultValue();

  /**
   * Handle default values for a parameter.
   * 
   * @return Return code: {@code true} if it has a default value, {@code false}
   *         if it is optional without a default value. Exception if it is a
   *         required parameter!
   * @throws UnspecifiedParameterException If the parameter requires a value
   */
  public abstract boolean tryDefaultValue() throws UnspecifiedParameterException;

  /**
   * Specifies if this parameter is an optional parameter.
   * 
   * @param opt true if this parameter is optional, false otherwise
   */
  public abstract void setOptional(boolean opt);

  /**
   * Checks if this parameter is an optional parameter.
   * 
   * @return true if this parameter is optional, false otherwise
   */
  public abstract boolean isOptional();

  /**
   * Checks if the default value of this parameter was taken as the actual
   * parameter value.
   * 
   * @return true, if the default value was taken as actual parameter value,
   *         false otherwise
   */
  public abstract boolean tookDefaultValue();

  /**
   * Returns true if the value of the option is defined, false otherwise.
   * 
   * @return true if the value of the option is defined, false otherwise.
   */
  public abstract boolean isDefined();

  /**
   * Returns the default value of the parameter.
   * <p/>
   * If the parameter has no default value, the method returns <b>null</b>.
   * 
   * @return the default value of the parameter, <b>null</b> if the parameter
   *         has no default value.
   */
  // TODO: change this to return a string value?
  public abstract T getDefaultValue();

  /**
   * Whether this class has a list of default values.
   * 
   * @return whether the class has a description of valid values.
   */
  public abstract boolean hasValuesDescription();

  /**
   * Return a string explaining valid values.
   * 
   * @return String explaining valid values (e.g. a class list)
   */
  public abstract String getValuesDescription();

  /**
   * Returns the extended description of the option which includes the option's
   * type, the short description and the default value (if specified).
   * 
   * @return the option's description.
   */
  public abstract String getFullDescription();

  /**
   * Return the OptionID of this option.
   * 
   * @return Option ID
   */
  public abstract OptionID getOptionID();

  /**
   * Returns the name of the option.
   * 
   * @return the option's name.
   */
  public abstract String getName();

  /**
   * Returns the short description of the option.
   * 
   * @return the option's short description.
   */
  public abstract String getShortDescription();

  /**
   * Sets the short description of the option.
   * 
   * @param description the short description to be set
   */
  public abstract void setShortDescription(String description);

  /**
   * Sets the value of the option.
   * 
   * @param obj the option's value to be set
   * @throws ParameterException if the given value is not a valid value for this
   *         option.
   */
  public abstract void setValue(Object obj) throws ParameterException;

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
  public abstract T getValue();

  /**
   * Get the last given value. May return {@code null}
   * 
   * @return Given value
   */
  public abstract Object getGivenValue();

  /**
   * Checks if the given argument is valid for this option.
   * 
   * @param obj option value to be checked
   * @return true, if the given value is valid for this option
   * @throws ParameterException if the given value is not a valid value for this
   *         option.
   */
  public abstract boolean isValid(Object obj) throws ParameterException;

  /**
   * Returns a string representation of the parameter's type (e.g. an
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter}
   * should return {@code <int>}).
   * 
   * @return a string representation of the parameter's type
   */
  public abstract String getSyntax();

  /**
   * Get the value as string. May return {@code null}
   * 
   * @return Value as string
   */
  public abstract String getValueAsString();

  /**
   * Get the default value as string.
   * 
   * @return default value
   */
  public abstract String getDefaultValueAsString();

  /**
   * Add an additional constraint.
   * 
   * @param constraint Constraint to add.
   */
  public abstract void addConstraint(ParameterConstraint<? super T> constraint);
}
