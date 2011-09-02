package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstract parameter class defining a parameter for a list of objects.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @param <T> List type
 */
public abstract class ListParameter<T> extends Parameter<List<T>, List<T>> {
  /**
   * A pattern defining a &quot,&quot.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * List separator character - &quot;:&quot;
   */
  public static final String LIST_SEP = ",";

  /**
   * A pattern defining a &quot:&quot.
   */
  public static final Pattern VECTOR_SPLIT = Pattern.compile(":");
  
  /**
   * Vector separator character - &quot;:&quot;
   */
  public static final String VECTOR_SEP = ":";

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be null
   * @param defaultValue the default value of this parameter (may be null)
   */
  public ListParameter(OptionID optionID, List<ParameterConstraint<List<T>>> constraints, List<T> defaultValue) {
    super(optionID, constraints, defaultValue);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraints of this parameter, may be null
   * @param optional specifies if this parameter is an optional parameter
   */
  public ListParameter(OptionID optionID, List<ParameterConstraint<List<T>>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param constraints the constraint of this parameter
   */
  // NOTE: we cannot have this, because it has the same erasure as optionID, defaults!
  // Use optional=false!
  /*public ListParameter(OptionID optionID, List<ParameterConstraint<List<T>>> constraints) {
    super(optionID, constraints);
  }*/

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter, may be null
   * @param defaultValue the default value of this parameter (may be null)
   */
  public ListParameter(OptionID optionID, ParameterConstraint<List<T>> constraint, List<T> defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter, may be null
   * @param optional specifies if this parameter is an optional parameter
   */
  public ListParameter(OptionID optionID, ParameterConstraint<List<T>> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   */
  public ListParameter(OptionID optionID, ParameterConstraint<List<T>> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   * @param defaultValue the default value of this parameter (may be null)
   */
  // NOTE: we cannot have this, because it has the same erasure as optionID, defaults!
  // Use full constructor, constraints = null!
  /*public ListParameter(OptionID optionID, List<T> defaultValue) {
    super(optionID, defaultValue);
  }*/

  /**
   * Constructs a list parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param optional Optional flag
   */
  public ListParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a list parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public ListParameter(OptionID optionID) {
    super(optionID);
  }

  /**
   * Returns the size of this list parameter.
   * 
   * @return the size of this list parameter.
   */
  public int getListSize() {
    if(getValue() == null && isOptional()) {
      return 0;
    }

    return getValue().size();
  }

  /**
   * Returns a string representation of this list parameter. The elements of
   * this list parameters are given in &quot;[ ]&quot;, comma separated.
   */
  // TODO: keep? remove?
  protected String asString() {
    if(getValue() == null) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append("[");

    for(int i = 0; i < getValue().size(); i++) {
      buffer.append(getValue().get(i).toString());
      if(i != getValue().size() - 1) {
        buffer.append(",");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }
}
