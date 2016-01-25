package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
 * Class storing a number of very common constraints.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.landmark
 * @apiviz.composedOf ParameterConstraint
 */
public final class CommonConstraints {
  /**
   * Integer constraint: >= -1
   */
  public static final ParameterConstraint<? super Integer> GREATER_EQUAL_MINUSONE_INT = new GreaterEqualConstraint(-1);

  /**
   * Not negative.
   */
  public static final ParameterConstraint<? super Integer> GREATER_EQUAL_ZERO_INT = new GreaterEqualConstraint(0);

  /**
   * Larger than zero.
   */
  public static final ParameterConstraint<? super Integer> GREATER_EQUAL_ONE_INT = new GreaterEqualConstraint(1);

  /**
   * Larger than one.
   */
  public static final ParameterConstraint<? super Integer> GREATER_THAN_ONE_INT = new GreaterConstraint(1);

  /**
   * Not negative.
   */
  public static final ParameterConstraint<? super Double> GREATER_EQUAL_ZERO_DOUBLE = new GreaterEqualConstraint(0.);

  /**
   * Larger than zero.
   */
  public static final ParameterConstraint<? super Double> GREATER_THAN_ZERO_DOUBLE = new GreaterConstraint(0.);

  /**
   * Constraint: less than .5
   */
  public static final ParameterConstraint<? super Double> LESS_THAN_HALF_DOUBLE = new LessConstraint(.5);

  /**
   * At least 1.
   */
  public static final ParameterConstraint<? super Double> GREATER_EQUAL_ONE_DOUBLE = new GreaterEqualConstraint(1.);

  /**
   * Larger than one.
   */
  public static final ParameterConstraint<? super Double> GREATER_THAN_ONE_DOUBLE = new GreaterConstraint(1.);

  /**
   * Less than one.
   */
  public static final ParameterConstraint<? super Double> LESS_THAN_ONE_DOUBLE = new LessConstraint(1.);

  /**
   * Less or equal than one.
   */
  public static final ParameterConstraint<? super Double> LESS_EQUAL_ONE_DOUBLE = new LessEqualConstraint(1.);

  /**
   * Constraint for the whole list.
   */
  public static final ParameterConstraint<int[]> GREATER_EQUAL_ZERO_INT_LIST = new ListEachConstraint(GREATER_EQUAL_ZERO_INT);

  /**
   * List constraint: >= 1
   */
  public static final ParameterConstraint<int[]> GREATER_EQUAL_ONE_INT_LIST = new ListEachConstraint(GREATER_EQUAL_ONE_INT);
}
