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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;

/**
 * Represents a Less-Than global parameter constraint. The value of the first
 * number parameter ({@link NumberParameter}) given has to be less than the
 * value of the second number parameter ({@link NumberParameter}) given.
 * 
 * @author Steffi Wanka
 * @param <T> Number type
 */
public class LessGlobalConstraint<T extends Number> implements GlobalParameterConstraint {
  /**
   * First number parameter.
   */
  private NumberParameter<T> first;

  /**
   * Second number parameter.
   */
  private NumberParameter<T> second;

  /**
   * Creates a Less-Than global parameter constraint. That is the value of the
   * first number parameter given has to be less than the value of the second
   * number parameter given.
   * 
   * @param first first number parameter
   * @param second second number parameter
   */
  public LessGlobalConstraint(NumberParameter<T> first, NumberParameter<T> second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Checks if the value of the first number parameter is less than the value of
   * the second number parameter. If not, a parameter exception is thrown.
   * 
   */
  @Override
  public void test() throws ParameterException {
    if(first.isDefined() && second.isDefined()) {
      if(first.getValue().doubleValue() >= second.getValue().doubleValue()) {
        throw new WrongParameterValueException("Global Parameter Constraint Error: \n" + "The value of parameter \"" + first.getName() + "\" has to be less than the" + "value of parameter \"" + second.getName() + "\"" + "(Current values: " + first.getName() + ": " + first.getValue().doubleValue() + ", " + second.getName() + ": " + second.getValue().doubleValue() + ")\n");
      }
    }
  }

  @Override
  public String getDescription() {
    return first.getName() + " < " + second.getName();
  }
}