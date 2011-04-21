package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Index with support for kNN queries.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database Object type
 */
public interface RKNNIndex<O> extends Index {
  /**
   * Get a KNN query object for the given distance function and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param distanceFunction Distance function
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceFunction<? super O, D> distanceFunction, Object... hints);

  /**
   * Get a KNN query object for the given distance query and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param distanceQuery Distance query
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints);
}