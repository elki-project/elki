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
package de.lmu.ifi.dbs.elki.data.spatial;

/**
 * Defines the required methods needed for comparison of spatial objects.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 */
public interface SpatialComparable {
  /**
   * Returns the dimensionality of the object.
   * 
   * @return the dimensionality
   */
  int getDimensionality();

  /**
   * Returns the minimum coordinate at the specified dimension.
   * 
   * @param dimension the dimension for which the coordinate should be returned,
   *        where 0 &le; dimension &lt; <code>getDimensionality()</code>
   * @return the minimum coordinate at the specified dimension
   */
  double getMin(int dimension);

  /**
   * Returns the maximum coordinate at the specified dimension.
   * 
   * @param dimension the dimension for which the coordinate should be returned,
   *        where 0 &le; dimension &lt; <code>getDimensionality()</code>
   * @return the maximum coordinate at the specified dimension
   */
  double getMax(int dimension);
}
