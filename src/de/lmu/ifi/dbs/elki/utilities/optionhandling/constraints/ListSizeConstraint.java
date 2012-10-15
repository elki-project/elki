package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;

/**
 * Represents a list-size parameter constraint. The size of the list parameter (
 * {@link ListParameter}) to be tested has to be equal to the specified list
 * size constraint.
 * 
 * @author Steffi Wanka
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter
 */
public class ListSizeConstraint implements ParameterConstraint<List<?>> {
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
  public void test(List<?> t) throws ParameterException {
    if(t.size() != sizeConstraint) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" + "List parameter has not the required size. (Requested size: " + +sizeConstraint + ", current size: " + t.size() + ").\n");
    }
  }

  @Override
  public String getDescription(String parameterName) {
    return "size(" + parameterName + ") = " + sizeConstraint;
  }
}
