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
 * Parameter class for a parameter specifying a list of integer values.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.1
 */
public class IntListParameter extends ListParameter<IntListParameter, int[]> {
  /**
   * Constructs an integer list parameter
   * 
   * @param optionID the unique id of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public IntListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs an integer list parameter
   * 
   * @param optionID the unique id of this parameter
   */
  public IntListParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    int[] val = getValue();
    if(val.length == 0) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    buf.append(val[0]);
    for(int i = 1; i < val.length; i++) {
      buf.append(LIST_SEP);
      buf.append(val[i]);
    }
    return buf.toString();
  }

  @Override
  public String getDefaultValueAsString() {
    int[] val = getDefaultValue();
    if(val.length == 0) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    buf.append(val[0]);
    for(int i = 1; i < val.length; i++) {
      buf.append(LIST_SEP);
      buf.append(val[i]);
    }
    return buf.toString();
  }

  @Override
  protected int[] parseValue(Object obj) throws ParameterException {
    if(obj instanceof int[]) {
      return (int[]) obj;
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      int[] intValue = new int[values.length];
      for(int i = 0; i < values.length; i++) {
        intValue[i] = ParseUtil.parseIntBase10(values[i]);
      }
      return intValue;
    }
    if(obj instanceof Integer) {
      return new int[] { (Integer) obj };
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a list of Integer values!");
  }

  @Override
  public int size() {
    return getValue().length;
  }

  /**
   * Returns a string representation of the parameter's type.
   *
   * @return &quot;&lt;int_1,...,int_n&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<int_1,...,int_n>";
  }

  /**
   * Get the values as a bitmask.
   *
   * See also: {@link de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil}
   *
   * @return Bitmask
   */
  public long[] getValueAsBitSet() {
    int[] value = getValue();
    int maxd = 0;
    for(int d : value) {
      maxd = (d > maxd) ? d : maxd;
    }
    long[] dimensions = new long[(maxd >>> 6) + 1];
    for(int d : value) {
      dimensions[d >>> 6] |= 1L << (d & 0x3F);
    }
    return dimensions;
  }
}
