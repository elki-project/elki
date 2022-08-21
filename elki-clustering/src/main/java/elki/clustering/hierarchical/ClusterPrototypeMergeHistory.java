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

import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBID;

/**
 * Cluster merge history with additional cluster prototypes (for HACAM,
 * MedoidLinkage, and MiniMax clustering)
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class ClusterPrototypeMergeHistory extends ClusterMergeHistory {
  /**
   * Cluster prototypes
   */
  protected ArrayDBIDs prototypes;

  /**
   * Constructor.
   *
   * @param ids Initial object ids
   * @param merges Merge history 2*(N-1) values
   * @param distances Distances
   * @param sizes Cluster sizes
   * @param isSquared If distances are squared distances
   * @param prototypes Cluster prototypes
   */
  public ClusterPrototypeMergeHistory(ArrayDBIDs ids, int[] merges, double[] distances, int[] sizes, boolean isSquared, ArrayDBIDs prototypes) {
    super(ids, merges, distances, sizes, isSquared);
    this.prototypes = prototypes;
  }

  /**
   * Get the prototype of cluster i.
   * 
   * @param i Merge number
   * @return Prototype
   */
  @SuppressWarnings("deprecation")
  public DBID prototype(int i) {
    // ignore deprecation, because we need to dereference here.
    return i < ids.size() ? ids.get(i) : prototypes.get(i - ids.size());
  }
}
