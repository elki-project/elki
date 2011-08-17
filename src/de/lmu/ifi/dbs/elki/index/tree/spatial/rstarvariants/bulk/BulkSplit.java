package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;
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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Interface for a bulk split strategy.
 * 
 * @author Erich Schubert
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
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries);
}