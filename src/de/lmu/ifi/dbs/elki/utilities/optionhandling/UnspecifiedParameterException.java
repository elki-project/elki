package de.lmu.ifi.dbs.elki.utilities.optionhandling;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Exception when a required parameter was not given.
 * @author Erich Schubert
 */
public class UnspecifiedParameterException extends WrongParameterValueException {
  /**
   * Serial UID
   */
  private static final long serialVersionUID = -7142809547201980898L;

  /**
   * Constructor with missing Parameter
   * @param parameter Missing parameter
   */
  public UnspecifiedParameterException(Parameter<?, ?> parameter) {
    super("No value given for parameter \"" + parameter.getName() + "\":\n" + "Expected: " + parameter.getFullDescription());
  }

  /**
   * Constructor with missing Parameter and cause
   * @param parameter Missing parameter
   * @param cause Cause
   */
  public UnspecifiedParameterException(Parameter<?, ?> parameter, Throwable cause) {
    super("No value given for parameter \"" + parameter.getName() + "\":\n" + "Expected: " + parameter.getFullDescription(), cause);
  }

  /**
   * Constructor with error message.
   * @param message Message
   */
  public UnspecifiedParameterException(String message) {
    super(message);
  }
}