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
import java.util.regex.PatternSyntaxException;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a pattern.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class PatternParameter extends AbstractParameter<PatternParameter, Pattern> {
  /**
   * Constructs a pattern parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, Pattern defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a pattern parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, String defaultValue) {
    super(optionID, Pattern.compile(defaultValue, Pattern.CASE_INSENSITIVE));
  }

  /**
   * Constructs a pattern parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   */
  public PatternParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Pattern parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(obj instanceof Pattern) {
      return (Pattern) obj;
    }
    if(obj instanceof String) {
      try {
        return Pattern.compile((String) obj, Pattern.CASE_INSENSITIVE);
      }
      catch(PatternSyntaxException e) {
        throw new WrongParameterValueException("Given pattern \"" + obj + "\" for parameter \"" + getOptionID().getName() + "\" is no valid regular expression!");
      }
    }
    throw new WrongParameterValueException("Given pattern \"" + obj + "\" for parameter \"" + getOptionID().getName() + "\" is of unknown type!");
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;pattern&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<pattern>";
  }
}
