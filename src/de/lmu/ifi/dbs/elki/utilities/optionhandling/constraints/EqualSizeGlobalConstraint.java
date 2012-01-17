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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter;

/**
 * Global parameter constraint defining that a number of list parameters (
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter}
 * ) must have equal list sizes.
 * 
 * @author Steffi Wanka
 */
public class EqualSizeGlobalConstraint implements GlobalParameterConstraint {
  /**
   * List parameters to be tested
   */
  private List<ListParameter<?>> parameters;

  /**
   * Creates a global parameter constraint for testing if a number of list
   * parameters have equal list sizes.
   * 
   * @param params list parameters to be tested for equal list sizes
   */
  public EqualSizeGlobalConstraint(List<ListParameter<?>> params) {
    this.parameters = params;
  }

  /**
   * Checks if the list parameters have equal list sizes. If not, a parameter
   * exception is thrown.
   * 
   */
  @Override
  public void test() throws ParameterException {
    boolean first = false;
    int constraintSize = -1;

    for(ListParameter<?> listParam : parameters) {
      if(listParam.isDefined()) {
        if(!first) {
          constraintSize = listParam.getListSize();
          first = true;
        }
        else if(constraintSize != listParam.getListSize()) {
          throw new WrongParameterValueException("Global constraint errror.\n" + "The list parameters " + OptionUtil.optionsNamesToString(parameters) + " must have equal list sizes.");
        }
      }
    }
  }

  @Override
  public String getDescription() {
    return "The list parameters " + OptionUtil.optionsNamesToString(parameters) + " must have equal list sizes.";
  }
}
