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

import java.util.Comparator;

/**
 * Comparator for sorting spatial objects by the minimum value in a single
 * dimension.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - SpatialComparable
 */
public class SpatialSingleMinComparator implements Comparator<SpatialComparable> {
  /**
   * Current dimension.
   */
  int dim;

  /**
   * Constructor.
   * 
   * @param dim Dimension to sort by.
   */
  public SpatialSingleMinComparator(int dim) {
    super();
    this.dim = dim;
  }

  /**
   * Set the dimension to sort by.
   * 
   * @param dim Dimension
   */
  public void setDimension(int dim) {
    this.dim = dim;
  }

  @Override
  public int compare(SpatialComparable o1, SpatialComparable o2) {
    return Double.compare(o1.getMin(dim), o2.getMin(dim));
  }
}
