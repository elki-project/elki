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

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract parameter class defining a parameter for a list of objects.
 *
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 *
 * @param <THIS> Type self-reference
 * @param <T> List type
 */
public abstract class ListParameter<THIS extends ListParameter<THIS, T>, T> extends AbstractParameter<THIS, T> {
  /**
   * A pattern defining a &quot;,&quot;.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * List separator character - &quot;:&quot;
   */
  public static final String LIST_SEP = ",";

  /**
   * A pattern defining a &quot;:&quot; or &quot;;&quot;.
   */
  public static final Pattern VECTOR_SPLIT = Pattern.compile("[:;]");

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
  public ListParameter(OptionID optionID, T defaultValue) {
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
   * Size of the list.
   *
   * @return Size
   */
  abstract public int size();
}
