package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;

/**
 * Represents a global parameter constraint for testing if the size of the list
 * parameter ({@link ListParameter}) given is equal to the constraint size
 * specified by the integer parameter ({@link IntParameter}) given.
 * 
 * @author Steffi Wanka
 */
public class GlobalListSizeConstraint implements GlobalParameterConstraint {
  /**
   * List parameter to be tested.
   */
  private ListParameter<?> list;

  /**
   * Integer parameter specifying the constraint list size.
   */
  private IntParameter length;

  /**
   * Creates a List-Size global parameter constraint.
   * <p/>
   * That is, the size of the given list parameter hat to be equal to the
   * constraint list size specified by the integer parameter given.
   * 
   * @param v the list parameter to be tested.
   * @param i integer parameter specifying the constraint list size.
   */
  public GlobalListSizeConstraint(ListParameter<?> v, IntParameter i) {
    this.list = v;
    this.length = i;
  }

  /**
   * Checks is the size of the list parameter is equal to the constraint list
   * size specified. If not, a parameter exception is thrown.
   * 
   */
  @Override
  public void test() throws ParameterException {
    if(!list.isDefined() || !length.isDefined()) {
      return;
    }

    if(list.getListSize() != length.getValue()) {
      throw new WrongParameterValueException("Global Parameter Constraint Error." + "\nThe size of the list parameter \"" + list.getName() + "\" must be " + length.getValue() + ", current size is " + list.getListSize() + ". The value is defined by the integer parameter " + length.getName() + ".\n");
    }
  }

  @Override
  public String getDescription() {
    return "size(" + list.getName() + ") == " + length.getValue();
  }
}
