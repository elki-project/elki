package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk;

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