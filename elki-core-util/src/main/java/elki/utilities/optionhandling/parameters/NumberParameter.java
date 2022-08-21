/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.utilities.optionhandling.parameters;

import elki.utilities.optionhandling.OptionID;

/**
 * Abstract class for defining a number parameter.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 *
 * @param <P> type self-reference
 * @param <T> the type of a possible value (i.e., the type of the option)
 */
public abstract class NumberParameter<P extends NumberParameter<P, T>, T extends Number> extends AbstractParameter<P, T> {
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
   * Constructs a number parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public NumberParameter(OptionID optionID) {
    super(optionID);
  }
}
