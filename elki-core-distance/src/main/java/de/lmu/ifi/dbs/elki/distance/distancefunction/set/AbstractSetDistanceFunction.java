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
package de.lmu.ifi.dbs.elki.distance.distancefunction.set;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/**
 * Abstract base class for set distance functions.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Vector type
 */
public abstract class AbstractSetDistanceFunction<O> implements PrimitiveDistanceFunction<O> {
  /**
   * Constants for checking null.
   */
  public static final Integer INTEGER_NULL = Integer.valueOf(0);

  /**
   * Constants for checking null.
   */
  public static final Double DOUBLE_NULL = Double.valueOf(0.);

  /**
   * Empty string.
   */
  public static final String STRING_NULL = "";

  /**
   * Test a value for null.
   * 
   * TODO: delegate to {@link FeatureVector} instead?
   * 
   * @param val Value
   * @return true when null
   */
  protected static boolean isNull(Object val) {
    return (val == null) || STRING_NULL.equals(val) || DOUBLE_NULL.equals(val) || INTEGER_NULL.equals(val);
  }
}
