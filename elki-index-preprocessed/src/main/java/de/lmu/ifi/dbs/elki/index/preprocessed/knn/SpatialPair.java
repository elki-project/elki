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
package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Defines the requirements for objects that can be indexed by a Spatial Index,
 * which are spatial nodes or data objects.
 * 
 * @author Elke Achtert
 * @since 0.4.0
 */
public class SpatialPair<K, V extends SpatialComparable> extends Pair<K, V> implements SpatialComparable {
  /**
   * Constructor: bundle a key and a spatial comparable
   * 
   * @param key key
   * @param spatial spatial value
   */
  public SpatialPair(K key, V spatial) {
    super(key, spatial);
  }

  @Override
  public int getDimensionality() {
    return second.getDimensionality();
  }

  @Override
  public double getMin(int dimension) {
    return second.getMin(dimension);
  }

  @Override
  public double getMax(int dimension) {
    return second.getMax(dimension);
  }
}