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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a list of vectors.
 *
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 */
public class DoubleArrayListParameter extends ListParameter<DoubleArrayListParameter, List<double[]>> {
  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param constraint Constraint
   * @param defaultValue Default value
   */
  public DoubleArrayListParameter(OptionID optionID, ParameterConstraint<List<double[]>> constraint, List<double[]> defaultValue) {
    super(optionID, defaultValue);
    addConstraint(constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param constraint Constraint
   * @param optional Optional flag
   */
  public DoubleArrayListParameter(OptionID optionID, ParameterConstraint<List<double[]>> constraint, boolean optional) {
    super(optionID, optional);
    addConstraint(constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param constraint Constraint
   */
  public DoubleArrayListParameter(OptionID optionID, ParameterConstraint<List<double[]>> constraint) {
    super(optionID);
    addConstraint(constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param defaultValue Default value
   */
  public DoubleArrayListParameter(OptionID optionID, List<double[]> defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   * @param optional Optional flag
   */
  public DoubleArrayListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   *
   * @param optionID Option ID
   */
  public DoubleArrayListParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    StringBuilder buf = new StringBuilder();
    List<double[]> val = getValue();
    Iterator<double[]> valiter = val.iterator();
    while(valiter.hasNext()) {
      buf.append(FormatUtil.format(valiter.next(), LIST_SEP));
      // Append separation character
      if(valiter.hasNext()) {
        buf.append(VECTOR_SEP);
      }
    }
    return buf.toString();
  }

  @Override
  public String getDefaultValueAsString() {
    StringBuilder buf = new StringBuilder();
    List<double[]> val = getDefaultValue();
    Iterator<double[]> valiter = val.iterator();
    while(valiter.hasNext()) {
      buf.append(FormatUtil.format(valiter.next(), LIST_SEP));
      // Append separation character
      if(valiter.hasNext()) {
        buf.append(VECTOR_SEP);
      }
    }
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<double[]> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for(Object o : l) {
        if(!(o instanceof double[])) {
          throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getOptionID().getName() + "\". Given list contains objects of different type!");
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list and vectors?
      return (List<double[]>) l;
    }
    catch(ClassCastException e) {
      // continue with other attempts.
    }
    if(obj instanceof String) {
      String[] vectors = VECTOR_SPLIT.split((String) obj);
      if(vectors.length == 0) {
        throw new WrongParameterValueException("Wrong parameter format! Given list of vectors for parameter \"" + getOptionID().getName() + "\" is empty!");
      }
      ArrayList<double[]> vecs = new ArrayList<>();

      double[] buf = new double[11];
      int used = 0;
      for(String vector : vectors) {
        used = 0;
        String[] coordinates = SPLIT.split(vector);
        for(String coordinate : coordinates) {
          try {
            if(used == buf.length) {
              buf = Arrays.copyOf(buf, buf.length << 1);
            }
            buf[used++] = ParseUtil.parseDouble(coordinate);
          }
          catch(NumberFormatException e) {
            throw new WrongParameterValueException("Wrong parameter format! Coordinates of vector \"" + vector + "\" are not valid!");
          }
        }
        vecs.add(Arrays.copyOf(buf, used));
      }
      return vecs;
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a list of double values!");
  }

  @Override
  public int size() {
    return getValue().size();
  }

  /**
   * Returns a string representation of the parameter's type.
   *
   * @return Syntax string
   */
  @Override
  public String getSyntax() {
    return "<double_11,...,double_1n:...:double_m1,...,double_mn>";
  }
}
