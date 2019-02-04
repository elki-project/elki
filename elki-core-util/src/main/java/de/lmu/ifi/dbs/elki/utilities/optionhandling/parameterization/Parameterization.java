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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.InternalParameterizationErrors;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Interface for object parameterizations.
 * 
 * See the {@link de.lmu.ifi.dbs.elki.utilities.optionhandling} package for
 * documentation!
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - - - Parameter
 * @assoc - - - ParameterException
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public interface Parameterization {
  /**
   * Get the option value from the Parameterization.
   * 
   * Note: this method returns success; the actual value can be obtained from
   * {@code opt} itself!
   * 
   * In particular {@link #grab} can return {@code true} when
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag#getValue
   * Flag.getValue()} returns {@code false}! Instead the semantics of
   * {@code grab} are those of {@code Parameter#isDefined()}.
   * 
   * This method will catch {@link ParameterException}s and store them to be
   * retrieved by {@link #getErrors}.
   * 
   * @param opt Option to add
   * @return if the value is available (= readable)
   */
  default boolean grab(Parameter<?> opt) {
    try {
      // Given value of default value instead.
      return setValueForOption(opt) || opt.tryDefaultValue();
    }
    catch(ParameterException e) {
      reportError(e);
      return false;
    }
  }

  /**
   * Assign a value for an option, but not using default values and throwing
   * exceptions on error.
   * 
   * @param opt Parameter to set
   * @return Success code
   * @throws ParameterException on assignment errors.
   */
  boolean setValueForOption(Parameter<?> opt) throws ParameterException;

  /**
   * Get the configuration errors thrown in {@link #grab}
   * 
   * @return Configuration errors encountered
   */
  Collection<ParameterException> getErrors();

  /**
   * Report a configuration error.
   * 
   * @param e Destination to report errors to
   */
  void reportError(ParameterException e);

  /**
   * Check for unused parameters
   * 
   * @return {@code true} if at least one parameter was not consumed
   */
  boolean hasUnusedParameters();

  /**
   * Descend parameterization tree into sub-option.
   * 
   * Note: this is done automatically by a
   * {@link ClassParameter#instantiateClass}.
   * You only need to call this when you want to expose the tree structure
   * without offering a class choice as parameter.
   * 
   * @param option Option subtree
   * @return Parameterization
   */
  default Parameterization descend(Object option) {
    return this;
  }

  /**
   * Return true when there have been errors.
   * 
   * @return Success code
   */
  default boolean hasErrors() {
    return !getErrors().isEmpty();
  }

  /**
   * Try to instantiate a particular class.
   * 
   * @param <C> return type
   * @param c Base class
   * @return class instance or null
   */
  default <C> C tryInstantiate(Class<C> c) {
    try {
      return ClassGenericsUtil.tryInstantiate(c, c, this);
    }
    catch(Exception e) {
      String msg = e.getMessage();
      reportError(new InternalParameterizationErrors("Error instantiating internal class: " + c.getName() //
          + " " + (msg != null ? msg : e.getClass().getName()), e));
      return null;
    }
  }
}
