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
package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Exception when a required parameter was not given.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class UnspecifiedParameterException extends WrongParameterValueException {
  /**
   * Serial UID
   */
  private static final long serialVersionUID = -7142809547201980898L;

  /**
   * Parameter that was missing.
   */
  private String parameter;

  /**
   * Constructor with missing Parameter
   * 
   * @param parameter Missing parameter
   */
  public UnspecifiedParameterException(Parameter<?> parameter) {
    super(parameter, null, "requires a value.");
    this.parameter = parameter.getOptionID().getName();
  }

  /**
   * Get the parameter name that was missing.
   * 
   * @return Parameter name
   */
  public String getParameterName() {
    return parameter;
  }
}
