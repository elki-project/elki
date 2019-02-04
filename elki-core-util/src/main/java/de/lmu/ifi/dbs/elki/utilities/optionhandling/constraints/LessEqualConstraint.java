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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.NumberParameter;

/**
 * Represents a Less-Equal-Than-Number parameter constraint. The value of the
 * number parameter ({@link NumberParameter}) tested has to be less equal than
 * the specified constraint value.
 * 
 * @author Steffi Wanka
 * @since 0.1
 */
public class LessEqualConstraint extends AbstractNumberConstraint {
  /**
   * Creates a Less-Equal-Than-Number parameter constraint.
   * <p>
   * That is, the value of the appropriate number parameter has to be less equal
   * than the given constraint value.
   * 
   * @param constraintValue the constraint value
   */
  public LessEqualConstraint(Number constraintValue) {
    super(constraintValue);
  }

  /**
   * Creates a Less-Equal-Than-Number parameter constraint.
   * <p>
   * That is, the value of the appropriate number parameter has to be less equal
   * than the given constraint value.
   * 
   * @param constraintValue the constraint value
   */
  public LessEqualConstraint(double constraintValue) {
    super(Double.valueOf(constraintValue));
  }

  /**
   * Creates a Less-Equal-Than-Number parameter constraint.
   * <p>
   * That is, the value of the appropriate number parameter has to be less equal
   * than the given constraint value.
   * 
   * @param constraintValue the constraint value
   */
  public LessEqualConstraint(int constraintValue) {
    super(Integer.valueOf(constraintValue));
  }

  /**
   * Checks if the number value given by the number parameter is less equal than
   * the constraint value. If not, a parameter exception is thrown.
   */
  @Override
  public void test(Number t) throws ParameterException {
    if (t.doubleValue() > constraintValue.doubleValue()) {
      throw new WrongParameterValueException("Parameter Constraint Error: \n" + "The parameter value specified has to be less equal than " + constraintValue.toString() + ". (current value: " + t.doubleValue() + ")\n");
    }
  }

  @Override
  public String getDescription(String parameterName) {
    return parameterName + " <= " + constraintValue;
  }
}
