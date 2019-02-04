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
 * Thrown by a Parameterizable object in case of wrong parameter format.
 * 
 * @author Steffi Wanka
 * @since 0.1
 */
public class WrongParameterValueException extends ParameterException {
  /**
   * Generated serial version UID.
   */
  private static final long serialVersionUID = 2155964376772417402L;

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read
   * @param reason detailed error description
   * @param cause the cause
   */
  public WrongParameterValueException(Parameter<?> parameter, String read, String reason, Throwable cause) {
    super(parameter, reason + formatRead(read) + "\nExpected: " + parameter.getOptionID().getDescription() + formatCause(cause), cause);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param read the value of the parameter read
   * @param reason detailed error description
   */
  public WrongParameterValueException(Parameter<?> parameter, String read, String reason) {
    this(parameter, read, reason, null);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param parameter the parameter that has a wrong value
   * @param parameter2 the second parameter that has a wrong value
   * @param reason detailed error description
   */
  public WrongParameterValueException(Parameter<?> parameter, String mid, Parameter<?> parameter2, String reason) {
    super(prefixParametersToMessage(parameter, mid, parameter2, reason));
  }

  /**
   * Format the value read for the parameter.
   *
   * @param read Read value
   * @return String
   */
  private static String formatRead(String read) {
    return read == null ? "" : ("\nRead: " + read);
  }

  /**
   * Thrown by a Parameterizable object in case of wrong parameter format.
   * 
   * @param message detail message
   */
  public WrongParameterValueException(String message) {
    super(message);
  }

  /**
   * Format the error cause.
   * 
   * @param cause Error cause.
   * @return String representation
   */
  private static String formatCause(Throwable cause) {
    if(cause == null) {
      return "";
    }
    String message = cause.getMessage();
    return "\n" + (message != null ? message : cause.toString());
  }
}
