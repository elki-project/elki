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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a list of vectors.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class VectorListParameter extends ListParameter<List<Double>> {
  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param constraints Constraint
   * @param defaultValue Default value
   */
  public VectorListParameter(OptionID optionID, List<ParameterConstraint<List<List<Double>>>> constraints, List<List<Double>> defaultValue) {
    super(optionID, constraints, defaultValue);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param constraints Constraints
   * @param optional Optional flag
   */
  public VectorListParameter(OptionID optionID, List<ParameterConstraint<List<List<Double>>>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param constraints Constraints
   */
  // Indiscernible from optionID, defaults
  /*
   * public VectorListParameter(OptionID optionID, List<ParameterConstraint<?
   * super List<List<Double>>>> constraints) { super(optionID, constraints); }
   */

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param constraint Constraint
   * @param defaultValue Default value
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint, List<List<Double>> defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param constraint Constraint
   * @param optional Optional flag
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param constraint Constraint
   */
  public VectorListParameter(OptionID optionID, ParameterConstraint<List<List<Double>>> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param defaultValue Default value
   */
  // Indiscernible from optionID, constraints
  /*
   * public VectorListParameter(OptionID optionID, List<List<Double>>
   * defaultValue) { super(optionID, defaultValue); }
   */

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   * @param optional Optional flag
   */
  public VectorListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a vector list parameter with the given name and description.
   * 
   * @param optionID Option ID
   */
  public VectorListParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    StringBuilder buf = new StringBuilder();
    List<List<Double>> val = getValue();
    Iterator<List<Double>> valiter = val.iterator();
    while(valiter.hasNext()) {
      List<Double> vec = valiter.next();
      Iterator<Double> veciter = vec.iterator();
      while(veciter.hasNext()) {
        buf.append(veciter.next().toString());
        if (veciter.hasNext()) {
          buf.append(LIST_SEP);
        }
      }
      // Append separation character
      if (valiter.hasNext()) {
        buf.append(VECTOR_SEP);
      }
    }
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<List<Double>> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for(Object o : l) {
        List<?> v = List.class.cast(o);
        for(Object c : v) {
          if(!(c instanceof Double)) {
            throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getName() + "\". Given list contains objects of different type!");
          }
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list and vectors?
      return (List<List<Double>>) l;
    }
    catch(ClassCastException e) {
      // continue with other attempts.
    }
    if(obj instanceof String) {
      String[] vectors = VECTOR_SPLIT.split((String) obj);
      if(vectors.length == 0) {
        throw new UnspecifiedParameterException("Wrong parameter format! Given list of vectors for parameter \"" + getName() + "\" is empty!");
      }
      ArrayList<List<Double>> vecs = new ArrayList<List<Double>>();

      for(String vector : vectors) {
        String[] coordinates = SPLIT.split(vector);
        ArrayList<Double> vectorCoord = new ArrayList<Double>();
        for(String coordinate : coordinates) {
          try {
            vectorCoord.add(Double.valueOf(coordinate));
          }
          catch(NumberFormatException e) {
            throw new WrongParameterValueException("Wrong parameter format! Coordinates of vector \"" + vector + "\" are not valid!");
          }
        }
        vecs.add(vectorCoord);
      }
      return vecs;
    }
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a list of Double values!");
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return 
   *         &quot;&lt;double_11,...,double_1n:...:double_m1,...,double_mn&gt;&quot
   *         ;
   */
  @Override
  public String getSyntax() {
    return "<double_11,...,double_1n:...:double_m1,...,double_mn>";
  }
}
