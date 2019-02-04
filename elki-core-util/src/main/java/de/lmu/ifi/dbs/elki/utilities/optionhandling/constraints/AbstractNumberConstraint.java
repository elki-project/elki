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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

/**
 * Abstract super class for constraints dealing with a certain number value.
 *
 * @author Elke Achtert
 * @since 0.1
 */
public abstract class AbstractNumberConstraint implements ParameterConstraint<Number> {
  /**
   * The constraint value.
   */
  final Number constraintValue;

  /**
   * Creates an abstract number constraint.
   *
   * @param constraintValue the constraint value
   */
  public AbstractNumberConstraint(Number constraintValue) {
    this.constraintValue = constraintValue;
  }
}
