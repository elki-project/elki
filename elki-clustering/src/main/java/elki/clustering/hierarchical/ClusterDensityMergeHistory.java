/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.hierarchical;

import elki.database.datastore.DoubleDataStore;
import elki.database.ids.ArrayDBIDs;

/**
 * Hierarchical clustering merge list, with additional coredists information.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class ClusterDensityMergeHistory extends ClusterMergeHistory {
  /**
   * Core distance information.
   */
  protected DoubleDataStore coredists;

  /**
   * Constructor.
   *
   * @param ids Initial object ids
   * @param merges Merge history 2*(N-1) values
   * @param distances Distances
   * @param sizes Cluster sizes
   * @param isSquared If distances are squared distances
   * @param coredists Density information
   */
  public ClusterDensityMergeHistory(ArrayDBIDs ids, int[] merges, double[] distances, int[] sizes, boolean isSquared, DoubleDataStore coredists) {
    super(ids, merges, distances, sizes, isSquared);
    this.coredists = coredists;
  }

  /**
   * Get the core distances
   * 
   * @return Core distances
   */
  public DoubleDataStore getCoreDistanceStore() {
    return coredists;
  }
}
