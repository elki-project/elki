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

import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract parameter class defining a parameter for a list of objects.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 *
 * @param <T> List type
 */
public abstract class ListParameter<T> extends AbstractParameter<List<T>> {
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
   * @param defaultValue the default value of this parameter (may be null)
   */
  public ListParameter(OptionID optionID, List<T> defaultValue) {
    super(optionID, defaultValue);
  }

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
    if (getValue() == null && isOptional()) {
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
    if (getValue() == null) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append('[');

    for (int i = 0; i < getValue().size(); i++) {
      buffer.append(getValue().get(i).toString());
      if (i != getValue().size() - 1) {
        buffer.append(',');
      }
    }
    buffer.append(']');
    return buffer.toString();
  }
}
