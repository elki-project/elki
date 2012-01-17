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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Represents a global parameter constraint specifying that at least one
 * parameter value of a given list of parameters ({@link Parameter}) has to be
 * set.
 * 
 * @author Steffi Wanka
 */
public class OneMustBeSetGlobalConstraint implements GlobalParameterConstraint {
  /**
   * List of parameters to be checked.
   */
  private List<Parameter<?,?>> parameters;

  /**
   * Creates a One-Must-Be-Set global parameter constraint. That is, at least
   * one parameter value of the given list of parameters has to be set.
   * 
   * @param params list of parameters
   */
  public OneMustBeSetGlobalConstraint(List<Parameter<?,?>> params) {
    parameters = params;
  }

  /**
   * Checks if at least one parameter value of the list of parameters specified
   * is set. If not, a parameter exception is thrown.
   * 
   */
  @Override
  public void test() throws ParameterException {
    for(Parameter<?,?> p : parameters) {
      if(p.isDefined()) {
        return;
      }
    }
    throw new WrongParameterValueException("Global Parameter Constraint Error.\n" + "At least one of the parameters " + OptionUtil.optionsNamesToString(parameters) + " has to be set.");
  }

  @Override
  public String getDescription() {
    return "At least one of the parameters " + OptionUtil.optionsNamesToString(parameters) + " has to be set.";
  }
}