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
import java.util.Vector;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Global parameter constraint specifying that either all elements of a list of
 * parameters ({@link Parameter}) must be set, or none of them.
 * 
 * @author Steffi Wanka
 */
public class AllOrNoneMustBeSetGlobalConstraint implements GlobalParameterConstraint {
  /**
   * List of parameters to be checked
   */
  private List<Parameter<?, ?>> parameterList;

  /**
   * Constructs a global parameter constraint for testing if either all elements
   * of a list of parameters are set or none of them.
   * 
   * @param parameters list of parameters to be checked
   */
  public AllOrNoneMustBeSetGlobalConstraint(List<Parameter<?, ?>> parameters) {
    this.parameterList = parameters;
  }

  /**
   * Checks if either all elements of a list of parameters are set, or none of
   * them. If not, a parameter exception is thrown.
   */
  @Override
  public void test() throws ParameterException {

    Vector<String> set = new Vector<String>();
    Vector<String> notSet = new Vector<String>();

    for(Parameter<?, ?> p : parameterList) {
      if(p.isDefined()) {
        set.add(p.getName());
      }
      else {
        notSet.add(p.getName());
      }
    }
    if(!set.isEmpty() && !notSet.isEmpty()) {
      throw new WrongParameterValueException("Global Constraint Error.\n" + "Either all of the parameters " + OptionUtil.optionsNamesToString(parameterList) + " must be set or none of them. " + "Parameter(s) currently set: " + set.toString() + ", parameters currently " + "not set: " + notSet.toString());
    }
  }

  @Override
  public String getDescription() {
    return "Either all of the parameters " + OptionUtil.optionsNamesToString(parameterList) + " must be set or none of them. ";
  }
}
