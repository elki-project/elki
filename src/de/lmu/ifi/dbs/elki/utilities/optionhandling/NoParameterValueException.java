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

/**
 * Thrown by OptionHandler in case of incorrect parameter-array.
 */
public class NoParameterValueException extends ParameterException {
  /**
   * Serial version UID
   */
  private static final long serialVersionUID = 8991076624591950629L;

  /**
   * Thrown by OptionHandler in case of incorrect parameter-array.
   *
   * @param message the detail message
   */
  public NoParameterValueException(String message) {
    super(message);
  }

  /**
   * Thrown by OptionHandler in case of incorrect parameter-array.
   *
   * @param message the detail message
   * @param cause   the cause
   */
  public NoParameterValueException(String message, Throwable cause) {
    super(message, cause);
  }
}
