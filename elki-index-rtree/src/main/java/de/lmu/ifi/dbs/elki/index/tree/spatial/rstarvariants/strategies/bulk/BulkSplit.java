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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Interface for a bulk split strategy.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface BulkSplit {
  /**
   * Partitions the specified feature vectors
   * 
   * @param <T> actual type we split
   * @param spatialObjects the spatial objects to be partitioned
   * @param minEntries the minimum number of entries in a partition
   * @param maxEntries the maximum number of entries in a partition
   * @return the partition of the specified spatial objects
   */
  <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries);
}