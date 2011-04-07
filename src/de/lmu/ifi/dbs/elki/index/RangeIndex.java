package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Index with support for kNN queries.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database Object type
 */
public interface RangeIndex<O extends DatabaseObject> extends Index<O> {

  /**
   * Get a range query object for the given distance function and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  // TODO: Do we really need the database?
  <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Database<O> database, DistanceFunction<? super O, D> distanceFunction, Object... hints);

  /**
   * Get a range query object for the given distance query and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param <D> Distance type
   * @param database Database
   * @param distanceQuery Distance query
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  // TODO: Do we really need the database?
  <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Database<O> database, DistanceQuery<O, D> distanceQuery, Object... hints);
}