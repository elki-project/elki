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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a string.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class StringParameter extends AbstractParameter<StringParameter, String> {
  /**
   * Constructs a string parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public StringParameter(OptionID optionID, String defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a string parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   */
  public StringParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue();
  }

  @Override
  protected String parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(obj instanceof String) {
      return (String) obj;
    }
    // TODO: allow anything convertible by toString()?
    throw new WrongParameterValueException("String parameter " + getOptionID().getName() + " is not a string.");
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;string&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<string>";
  }
}
