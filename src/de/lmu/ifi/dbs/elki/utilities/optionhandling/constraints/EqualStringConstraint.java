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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a parameter constraint for testing if the string value of the
 * string parameter (
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter}
 * ) to be tested is equal to the specified constraint-strings.
 * 
 * @author Steffi Wanka
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter
 */
public class EqualStringConstraint implements ParameterConstraint<String> {
  /**
   * Constraint-strings.
   */
  private String[] testStrings;

  /**
   * Creates an Equal-To-String parameter constraint.
   * <p/>
   * That is, the string value of the parameter to be tested has to be equal to
   * one of the given constraint-strings.
   * 
   * @param testStrings constraint-strings.
   */
  public EqualStringConstraint(String[] testStrings) {
    this.testStrings = testStrings;
  }

  private String constraintStrings() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[");
    for(int i = 0; i < testStrings.length; i++) {
      buffer.append(testStrings[i]);
      if(i != testStrings.length - 1) {
        buffer.append(",");
      }
    }

    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Checks if the given string value of the string parameter is equal to one of
   * the constraint strings. If not, a parameter exception is thrown.
   */
  @Override
  public void test(String t) throws ParameterException {
    for(String constraint : testStrings) {
      if(t.equalsIgnoreCase(constraint)) {
        return;
      }
    }

    throw new WrongParameterValueException("Parameter Constraint Error.\n" + "Parameter value must be one of the following values: " + constraintStrings());
  }

  @Override
  public String getDescription(String parameterName) {
    return parameterName + " in " + Arrays.asList(testStrings).toString();
  }
}
