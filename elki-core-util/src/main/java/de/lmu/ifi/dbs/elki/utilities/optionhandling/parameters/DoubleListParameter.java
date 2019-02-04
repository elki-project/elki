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

import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a list of double values.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 */
public class DoubleListParameter extends ListParameter<DoubleListParameter, double[]> {
  /**
   * Constructs a list parameter with the given optionID and optional flag.
   * 
   * @param optionID Option ID
   * @param optional Optional flag
   */
  public DoubleListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID Option ID
   */
  public DoubleListParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return FormatUtil.format(getValue(), LIST_SEP);
  }

  @Override
  public String getDefaultValueAsString() {
    return FormatUtil.format(getDefaultValue(), LIST_SEP);
  }

  @Override
  protected double[] parseValue(Object obj) throws ParameterException {
    if(obj instanceof double[]) {
      return double[].class.cast(obj);
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      double[] doubleValue = new double[values.length];
      for(int i = 0; i < values.length; i++) {
        doubleValue[i] = ParseUtil.parseDouble(values[i]);
      }
      return doubleValue;
    }
    if(obj instanceof Double) {
      return new double[] { (Double) obj };
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a list of Double values!");
  }

  @Override
  public int size() {
    return getValue().length;
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;double_1,...,double_n&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<double_1,...,double_n>";
  }
}
