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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;

/**
 * Represents a list-size parameter constraint. The size of the list parameter (
 * {@link ListParameter}) to be tested has to be equal to the specified list
 * size constraint.
 * <p>
 * FIXME: Unfortunately, we cannot have good type safety anymore right now.
 *
 * @author Steffi Wanka
 * @since 0.1
 *
 * @assoc - - - ListParameter
 */
public class ListSizeConstraint implements ParameterConstraint<Object> {
  /**
   * The list size constraint.
   */
  private int sizeConstraint;

  /**
   * Constructs a list size constraint with the given constraint size.
   *
   * @param size the size constraint for the list parameter
   */
  public ListSizeConstraint(int size) {
    sizeConstraint = size;
  }

  /**
   * Checks if the list parameter fulfills the size constraint. If not, a
   * parameter exception is thrown.
   *
   * @throws ParameterException, if the size of the list parameter given is not
   *         equal to the list size constraint specified.
   */
  @Override
  public void test(Object t) throws ParameterException {
    if(t instanceof List && ((List<?>) t).size() != sizeConstraint) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" //
          + "List parameter has not the required size. (Requested size: " + sizeConstraint //
          + ", current size: " + ((List<?>) t).size() + ").\n");
    }
    if(t instanceof int[] && ((int[]) t).length != sizeConstraint) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" //
          + "List parameter has not the required size. (Requested size: " + sizeConstraint //
          + ", current size: " + ((int[]) t).length + ").\n");
    }
    if(t instanceof double[] && ((double[]) t).length != sizeConstraint) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" //
          + "List parameter has not the required size. (Requested size: " + sizeConstraint //
          + ", current size: " + ((double[]) t).length + ").\n");
    }
  }

  @Override
  public String getDescription(String parameterName) {
    return "size(" + parameterName + ") = " + sizeConstraint;
  }
}
