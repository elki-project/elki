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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Length constraint for a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter}
 * .
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter
 */
public class StringLengthConstraint implements ParameterConstraint<String> {
  /**
   * Minimum length
   */
  int minlength;

  /**
   * Maximum length
   */
  int maxlength;

  /**
   * Constructor with minimum and maximum length.
   * 
   * @param minlength Minimum length, may be 0 for no limit
   * @param maxlength Maximum length, may be -1 for no limit
   */
  public StringLengthConstraint(int minlength, int maxlength) {
    super();
    this.minlength = minlength;
    this.maxlength = maxlength;
  }

  /**
   * Checks if the given string value of the string parameter is within the
   * length restrictions. If not, a parameter exception is thrown.
   */
  @Override
  public void test(String t) throws ParameterException {
    if(t.length() < minlength) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" + "Parameter value length must be at least " + minlength + ".");
    }
    if(maxlength > 0 && t.length() > maxlength) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" + "Parameter value length must be at most " + maxlength + ".");
    }
  }

  @Override
  public String getDescription(String parameterName) {
    if(maxlength > 0) {
      return parameterName + " has length " + minlength + " to " + maxlength + ".";
    }
    else {
      return parameterName + " has length of at least " + minlength + ".";
    }
  }
}
