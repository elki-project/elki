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
 * Class storing a number of very common constraints.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @composed - - - ParameterConstraint
 */
public final class CommonConstraints {
  /**
   * Integer constraint: &gt;= -1
   */
  public static final AbstractNumberConstraint GREATER_EQUAL_MINUSONE_INT = new GreaterEqualConstraint(-1);

  /**
   * Not negative.
   */
  public static final AbstractNumberConstraint GREATER_EQUAL_ZERO_INT = new GreaterEqualConstraint(0);

  /**
   * Larger than zero.
   */
  public static final AbstractNumberConstraint GREATER_EQUAL_ONE_INT = new GreaterEqualConstraint(1);

  /**
   * Larger than one.
   */
  public static final AbstractNumberConstraint GREATER_THAN_ONE_INT = new GreaterConstraint(1);

  /**
   * Not negative.
   */
  public static final AbstractNumberConstraint GREATER_EQUAL_ZERO_DOUBLE = new GreaterEqualConstraint(0.);

  /**
   * Larger than zero.
   */
  public static final AbstractNumberConstraint GREATER_THAN_ZERO_DOUBLE = new GreaterConstraint(0.);

  /**
   * Constraint: less than .5
   */
  public static final AbstractNumberConstraint LESS_THAN_HALF_DOUBLE = new LessConstraint(.5);

  /**
   * At least 1.
   */
  public static final AbstractNumberConstraint GREATER_EQUAL_ONE_DOUBLE = new GreaterEqualConstraint(1.);

  /**
   * Larger than one.
   */
  public static final AbstractNumberConstraint GREATER_THAN_ONE_DOUBLE = new GreaterConstraint(1.);

  /**
   * Less than one.
   */
  public static final AbstractNumberConstraint LESS_THAN_ONE_DOUBLE = new LessConstraint(1.);

  /**
   * Less or equal than one.
   */
  public static final AbstractNumberConstraint LESS_EQUAL_ONE_DOUBLE = new LessEqualConstraint(1.);

  /**
   * Constraint for the whole list.
   */
  public static final ParameterConstraint<int[]> GREATER_EQUAL_ZERO_INT_LIST = new ListEachNumberConstraint<int[]>(GREATER_EQUAL_ZERO_INT);

  /**
   * List constraint: &gt;= 1
   */
  public static final ParameterConstraint<int[]> GREATER_EQUAL_ONE_INT_LIST = new ListEachNumberConstraint<int[]>(GREATER_EQUAL_ONE_INT);

  /**
   * List constraint: &gt; 1
   */
  public static final ParameterConstraint<int[]> GREATER_THAN_ONE_INT_LIST = new ListEachNumberConstraint<int[]>(GREATER_THAN_ONE_INT);

  /**
   * Constraint for the whole list.
   */
  public static final ParameterConstraint<double[]> GREATER_EQUAL_ZERO_DOUBLE_LIST = new ListEachNumberConstraint<double[]>(GREATER_EQUAL_ZERO_DOUBLE);

  /**
   * List constraint: &gt;= 1
   */
  public static final ParameterConstraint<double[]> GREATER_EQUAL_ONE_DOUBLE_LIST = new ListEachNumberConstraint<double[]>(GREATER_EQUAL_ONE_DOUBLE);

  /**
   * List constraint: &gt; 1
   */
  public static final ParameterConstraint<double[]> GREATER_THAN_ONE_DOUBLE_LIST = new ListEachNumberConstraint<double[]>(GREATER_THAN_ONE_DOUBLE);
}
