package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;

/**
 * Interface for initializing K-Medoids. In contrast to k-means initializers,
 * this initialization will only return members of the original data set.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Object type
 */
public interface KMedoidsInitialization<V> {
  /**
   * Choose initial means
   * 
   * @param k Parameter k
   * @param distanceFunction Distance function
   * @return List of chosen means for k-means
   */
  public abstract DBIDs chooseInitialMedoids(int k, DistanceQuery<? super V, ?> distanceFunction);
}