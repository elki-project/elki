package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Interface for a bulk split strategy.
 * 
 * @author Erich Schubert
 *
 * @param <N> Base type that can be split by this strategy.
 */
public interface BulkSplit {
  /**
   * Partitions the specified feature vectors
   * 
   * @param spatialObjects the spatial objects to be partitioned
   * @param minEntries the minimum number of entries in a partition
   * @param maxEntries the maximum number of entries in a partition
   * @param <T> actual subtype
   * @return the partition of the specified spatial objects
   */
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries);
}