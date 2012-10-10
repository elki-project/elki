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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a long value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class LongParameter extends NumberParameter<Long> {
  /**
   * Constructs a long parameter with the given optionID, parameter constraint
   * and default value.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param constraints the parameter constraints for this long parameter
   * @param defaultValue the default value
   */
  public LongParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints, long defaultValue) {
    super(optionID, constraints, Long.valueOf(defaultValue));
  }

  /**
   * Constructs a long parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param constraints the parameter constraints for this long parameter
   * @param optional optional flag
   */
  public LongParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a long parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param constraints the parameter constraints for this long parameter
   */
  public LongParameter(OptionID optionID, List<ParameterConstraint<Number>> constraints) {
    super(optionID, constraints);
  }

  /**
   * Constructs a long parameter with the given optionID, parameter constraint
   * and default value.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param constraint the parameter constraint for this long parameter
   * @param defaultValue the default value
   */
  public LongParameter(OptionID optionID, ParameterConstraint<Number> constraint, long defaultValue) {
    super(optionID, constraint, Long.valueOf(defaultValue));
  }

  /**
   * Constructs a long parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param constraint the parameter constraint for this long parameter
   * @param optional optional flag
   */
  public LongParameter(OptionID optionID, ParameterConstraint<Number> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a long parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param constraint the parameter constraint for this long parameter
   */
  public LongParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a long parameter with the given optionID and default value.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param defaultValue the default value
   */
  public LongParameter(OptionID optionID, long defaultValue) {
    super(optionID, Long.valueOf(defaultValue));
  }

  /**
   * Constructs a long parameter with the given optionID.
   * 
   * @param optionID the unique OptionID for this parameter
   * @param optional optional flag
   */
  public LongParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a long parameter with the given optionID.
   * 
   * @param optionID the unique OptionID for this parameter
   */
  public LongParameter(OptionID optionID) {
    super(optionID);
  }
  
  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Long parseValue(Object obj) throws ParameterException {
    if(obj instanceof Long) {
      return (Long) obj;
    }
    if(obj instanceof Number) {
      return Long.valueOf(((Number) obj).longValue());
    }
    try {
      return Long.valueOf(obj.toString());
    }
    catch(NullPointerException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long value, read: " + obj + "!\n");
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;long&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<long>";
  }
}
