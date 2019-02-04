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
 * Parameter class for a parameter specifying a long value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class LongParameter extends NumberParameter<LongParameter, Long> {
  /**
   * Constructs a long parameter with the given optionID and default value.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param defaultValue the default value
   */
  public LongParameter(OptionID optionID, long defaultValue) {
    super(optionID, Long.valueOf(defaultValue));
  }

  /**
   * Constructs a long parameter with the given optionID.
   * 
   * @param optionID the unique OptionID for this parameter
   */
  public LongParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Long parseValue(Object obj) throws ParameterException {
    if(obj instanceof Long) {
      return (Long) obj;
    }
    if(obj instanceof Number) {
      return ((Number) obj).longValue();
    }
    try {
      return ParseUtil.parseLongBase10(obj.toString());
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a long value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;long&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<long>";
  }
}
