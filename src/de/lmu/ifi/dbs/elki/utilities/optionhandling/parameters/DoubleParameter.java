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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a double value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class DoubleParameter extends NumberParameter<Double> {
  /**
   * Constructs a double parameter with the given optionID, parameter
   * constraint, and default value.
   * 
   * @param optionID the unique id of this parameter
   * @param defaultValue the default value for this parameter
   * @param constraint the constraint of this parameter
   * @deprecated Use {@link #addConstraint} instead.
   */
  @Deprecated
  public DoubleParameter(OptionID optionID, double defaultValue, ParameterConstraint<Number> constraint) {
    super(optionID, defaultValue);
    addConstraint(constraint);
  }

  /**
   * Constructs a double parameter with the given optionID, and parameter
   * constraint.
   * 
   * @param optionID the unique id of this parameter
   * @param constraint the constraint of this parameter
   * @deprecated Use {@link #addConstraint} instead.
   */
  @Deprecated
  public DoubleParameter(OptionID optionID, ParameterConstraint<Number> constraint) {
    super(optionID);
    addConstraint(constraint);
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param defaultValue the default value for this double parameter
   */
  public DoubleParameter(OptionID optionID, double defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param optional Flag to indicate that the parameter is optional
   * 
   * @deprecated Use {@link #setOptional} instead.
   */
  @Deprecated
  public DoubleParameter(OptionID optionID, boolean optional) {
    super(optionID);
    setOptional(optional);
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public DoubleParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Double parseValue(Object obj) throws WrongParameterValueException {
    if (obj instanceof Double) {
      return (Double) obj;
    }
    try {
      return Double.valueOf(obj.toString());
    } catch (NullPointerException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a double value, read: " + obj + "!\n");
    } catch (NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a double value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;double&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<double>";
  }

  /**
   * Get the parameter value as double.
   * 
   * @return double value
   */
  public double doubleValue() {
    return getValue().doubleValue();
  }
}
