package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Index with support for kNN queries.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has KNNQuery oneway - - «provides»
 * 
 * @param <O> Object type
 */
public interface KNNIndex<O> extends Index {
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
  <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints);
}