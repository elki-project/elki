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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Abstract super class for all exceptions thrown during parameterization.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class ParameterException extends Exception {
  /**
   * Serialization version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   *
   * @param p Parameter
   * @param message Error message
   */
  public ParameterException(Parameter<?> p, String message) {
    super(prefixParameterToMessage(p, message));
  }

  /**
   * Constructor.
   *
   * @param p Parameter
   * @param message Error message
   * @param cause root cause
   */
  public ParameterException(Parameter<?> p, String message, Throwable cause) {
    super(prefixParameterToMessage(p, message), cause);
  }

  /**
   * Constructor.
   *
   * @param message Error message
   */
  public ParameterException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message Error message
   * @param cause root cause
   */
  public ParameterException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Prefix parameter information to error message.
   *
   * @param p Parameter
   * @param message Error message
   * @return Combined error message
   */
  public static String prefixParameterToMessage(Parameter<?> p, String message) {
    return new StringBuilder(100 + message.length()) //
        .append(p instanceof Flag ? "Flag '" : "Parameter '") //
        .append(p.getOptionID().getName()) //
        .append("' ").append(message).toString();
  }

  /**
   * Prefix parameters to error message.
   *
   * @param p Parameter
   * @param p2 Parameter
   * @param message Error message
   * @return Combined error message
   */
  public static String prefixParametersToMessage(Parameter<?> p, String mid, Parameter<?> p2, String message) {
    return new StringBuilder(200 + mid.length() + message.length())//
        .append(p instanceof Flag ? "Flag '" : "Parameter '") //
        .append(p.getOptionID().getName()) //
        .append("' ").append(mid) //
        .append(p instanceof Flag ? " Flag '" : " Parameter '") //
        .append(p.getOptionID().getName()) //
        .append(message.length() > 0 ? "' " : "'.").append(message).toString();
  }
}
