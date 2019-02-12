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
package de.lmu.ifi.dbs.elki.evaluation.clustering.pairsegments;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * A segment represents a set of pairs that share the same clustering
 * properties.
 * 
 * As such, for each ring (= clustering), a cluster number (or the constant
 * {@link #UNCLUSTERED}) is stored.
 *
 * @author Sascha Goldhofer
 * @author Erich Schubert
 * @since 0.5.0
 */
public class Segment implements Comparable<Segment> {
  /**
   * Object is not clustered
   */
  public static final int UNCLUSTERED = -1;

  /**
   * IDs in segment, for object segments.
   */
  protected DBIDs objIDs = null;

  /**
   * Size of cluster, in pairs.
   */
  protected long pairsize = 0;

  /**
   * The cluster numbers in each ring
   */
  protected int[] clusterIds;

  /**
   * Constructor.
   *
   * @param clusterings Number of clusterings
   */
  public Segment(int clusterings) {
    clusterIds = new int[clusterings];
  }

  /**
   * Get the number of pairs in the segment.
   *
   * @return Number of pairs
   */
  public long getPairCount() {
    return pairsize;
  }

  /**
   * Constructor.
   * 
   * @param clone Clone of cluster ids
   */
  public Segment(int[] clone) {
    clusterIds = clone;
  }

  /**
   * Get cluster number for index idx.
   * 
   * @param idx Index
   * @return Cluster number
   */
  public int get(int idx) {
    return clusterIds[idx];
  }

  /**
   * Checks if the segment has a cluster with unpaired objects. Unpaired
   * clusters are represented by "0" (0 = all).
   * 
   * @return true when unclustered in at least one dimension.
   */
  public boolean isUnpaired() {
    for(int id : clusterIds) {
      if(id == UNCLUSTERED) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if this segment contains the pairs that are never clustered by any of
   * the clusterings (all 0).
   * 
   * @return true when unclustered everywhere
   */
  public boolean isNone() {
    for(int id : clusterIds) {
      if(id != UNCLUSTERED) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the index of the first clustering having an unpaired cluster, or -1
   * no unpaired cluster exists.
   * 
   * @return clustering id or -1
   */
  public int getUnpairedClusteringIndex() {
    for(int index = 0; index < clusterIds.length; index++) {
      if(clusterIds[index] == UNCLUSTERED) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Get the DBIDs of objects contained in this segment.
   * 
   * @return the segment IDs
   */
  public DBIDs getDBIDs() {
    return objIDs;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(Segment.class.isInstance(obj))) {
      return false;
    }
    Segment other = (Segment) obj;
    return Arrays.equals(clusterIds, other.clusterIds);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public int compareTo(Segment sid) {
    for(int i = 0; i < clusterIds.length; i++) {
      final int a = this.clusterIds[i];
      final int b = sid.clusterIds[i];
      if(a != b) {
        if(a * b > 0) {
          // Regular comparison
          return (a < b) ? -1 : +1;
          // return (a < b) ? +1 : -1;
        }
        else {
          // Inverse, to sort negative last
          return (a < b) ? +1 : -1;
        }
      }
    }
    return 0;
  }
}
