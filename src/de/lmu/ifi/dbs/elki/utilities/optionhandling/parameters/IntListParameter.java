package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a list of integer values.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 */
public class IntListParameter extends ListParameter<Integer> {
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
    StringBuilder buf = new StringBuilder();
    List<Integer> val = getValue();
    Iterator<Integer> veciter = val.iterator();
    while(veciter.hasNext()) {
      buf.append(veciter.next().toString());
      if (veciter.hasNext()) {
        buf.append(LIST_SEP);
      }
    }
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Integer> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for (Object o : l) {
        if (!(o instanceof Integer)) {
          throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given list contains objects of different type!");
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list?
      return (List<Integer>)l;
    } catch (ClassCastException e) {
      // continue with others
    }
    if(obj instanceof String) {
      String[] values = SPLIT.split((String) obj);
      ArrayList<Integer> intValue = new ArrayList<Integer>(values.length);
      for(String val : values) {
        intValue.add(Integer.valueOf(val));
      }
      return intValue;
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of Integer values!");
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
}
