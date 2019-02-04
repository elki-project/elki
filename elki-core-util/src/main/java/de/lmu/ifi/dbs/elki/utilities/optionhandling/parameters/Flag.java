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

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Option class specifying a flag object.
 * <p>
 * A flag object is optional parameter which can be set (value &quot;true&quot;)
 * or not (value &quot;false&quot;).
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 */
public class Flag extends AbstractParameter<Flag, Boolean> {
  /**
   * Constant indicating that the flag is set.
   */
  public static final String SET = "true";

  /**
   * Constant indicating that the flag is not set.
   */
  public static final String NOT_SET = "false";

  /**
   * Constructs a flag object with the given optionID.
   * <p>
   * If the flag is not set its value is &quot;false&quot;.
   * 
   * @param optionID the unique id of the option
   */
  public Flag(OptionID optionID) {
    super(optionID);
    setOptional(true);
    setDefaultValue(Boolean.FALSE);
  }

  @Override
  protected Boolean parseValue(Object obj) throws ParameterException {
    if(SET.equals(obj)) {
      return Boolean.TRUE;
    }
    if(NOT_SET.equals(obj)) {
      return Boolean.FALSE;
    }
    if(obj instanceof Boolean) {
      return (Boolean) obj;
    }
    if(obj != null && SET.equals(obj.toString())) {
      return Boolean.TRUE;
    }
    if(obj != null && NOT_SET.equals(obj.toString())) {
      return Boolean.FALSE;
    }
    throw new WrongParameterValueException("Wrong value for flag \"" + getOptionID().getName() + "\". Allowed values:\n" + SET + " or " + NOT_SET);
  }

  /**
   * A flag has no syntax, since it doesn't take extra options
   */
  @Override
  public String getSyntax() {
    return "<|" + SET + "|" + NOT_SET + ">";
  }

  @Override
  public String getValueAsString() {
    return getValue().booleanValue() ? SET : NOT_SET;
  }

  @Override
  protected boolean validate(Boolean obj) throws ParameterException {
    if(obj == null) {
      throw new WrongParameterValueException("Boolean option '" + getOptionID().getName() + "' got 'null' value.");
    }
    return true;
  }

  /**
   * Convenience function using a native boolean, that doesn't require error
   * handling.
   * 
   * @param val boolean value
   */
  public void setValue(boolean val) {
    try {
      super.setValue(Boolean.valueOf(val));
    }
    catch(ParameterException e) {
      // We're pretty sure that any Boolean is okay, so this should never be
      // reached.
      throw new AbortException("Flag did not accept boolean value!", e);
    }
  }

  /**
   * Shorthand for {@code isDefined() && getValue() == true}
   * 
   * @return true when defined and true.
   */
  public boolean isTrue() {
    return isDefined() && getValue().booleanValue();
  }

  /**
   * Shorthand for {@code isDefined() && getValue() == false}
   * 
   * @return true when defined and true.
   */
  public boolean isFalse() {
    return isDefined() && !getValue().booleanValue();
  }
}