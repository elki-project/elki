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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.database.datastore.IntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Utility functionality for writing clustering algorithms.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public final class ClusteringAlgorithmUtil {
  /**
   * Private constructor. Static methods only.
   */
  private ClusteringAlgorithmUtil() {
    // Do not use.
  }

  /**
   * Collect clusters from their [0;k-1] integer labels.
   * 
   * @param ids Objects
   * @param assignment Cluster assignment
   * @param k Number of labels (must be labeled 0 to k-1)
   * @return Partitions
   */
  public static ArrayModifiableDBIDs[] partitionsFromIntegerLabels(DBIDs ids, IntegerDataStore assignment, int k) {
    int[] sizes = new int[k];
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      sizes[assignment.intValue(iter)] += 1;
    }
    ArrayModifiableDBIDs[] clusters = new ArrayModifiableDBIDs[k];
    for(int i = 0; i < k; i++) {
      clusters[i] = DBIDUtil.newArray(sizes[i]);
    }
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      clusters[assignment.intValue(iter)].add(iter);
    }
    return clusters;
  }
}
