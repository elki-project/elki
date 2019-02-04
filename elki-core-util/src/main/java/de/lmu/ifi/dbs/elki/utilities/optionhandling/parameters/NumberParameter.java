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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract class for defining a number parameter.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 *
 * @param <THIS> type self-reference
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class NumberParameter<THIS extends NumberParameter<THIS, T>, T extends Number> extends AbstractParameter<THIS, T> {
  /**
   * Constructs a number parameter with the given optionID and default Value.
   * 
   * @param optionID the unique id of this parameter
   * @param defaultValue the default value for this parameter
   */
  public NumberParameter(OptionID optionID, T defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a number parameter with the given optionID and optional flag.
   * 
   * @param optionID the unique id of this parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public NumberParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a number parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public NumberParameter(OptionID optionID) {
    super(optionID);
  }
}
