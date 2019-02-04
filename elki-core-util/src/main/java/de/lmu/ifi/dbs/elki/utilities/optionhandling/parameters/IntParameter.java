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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying an integer value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 */
public class IntParameter extends NumberParameter<IntParameter, Integer> {
  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   * @param defaultValue the default value
   */
  public IntParameter(OptionID optionID, int defaultValue) {
    super(optionID, Integer.valueOf(defaultValue));
  }

  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   */
  public IntParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Integer parseValue(Object obj) throws ParameterException {
    if(obj instanceof Integer) {
      return (Integer) obj;
    }
    if(obj instanceof Number) {
      return ((Number) obj).intValue();
    }
    try {
      return ParseUtil.parseIntBase10(obj.toString());
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires an integer value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;int&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<int>";
  }

  /**
   * Get the parameter value as integer
   * 
   * @return Parameter value
   */
  public int intValue() {
    return getValue().intValue();
  }
}
