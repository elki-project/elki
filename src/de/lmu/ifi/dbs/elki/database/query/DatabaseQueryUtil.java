package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Utility classes for Database Query handling.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery oneway - - invokes
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.query.range.RangeQuery oneway - - invokes
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery oneway - - invokes
 */
public final class DatabaseQueryUtil {
  /**
   * Execute a single kNN query by Object DBID
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceFunction Distance function to use
   * @param k Value of k
   * @param id DBID to query
   * @return kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByDBID(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, DBID id) {
    return database.getKNNQuery(distanceFunction, k, DatabaseQuery.HINT_SINGLE).getKNNForDBID(id, k);
  }

  /**
   * Execute a single kNN query by Object DBID
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceQuery Distance query to use
   * @param k Value of k
   * @param id DBID to query
   * @return kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByDBID(Database<O> database, DistanceQuery<O, D> distanceQuery, int k, DBID id) {
    return database.getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_SINGLE).getKNNForDBID(id, k);
  }

  /**
   * Execute a single kNN query by Object
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceFunction Distance function to use
   * @param k Value of k
   * @param obj Query object
   * @return kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByObject(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, O obj) {
    return database.getKNNQuery(distanceFunction, k, DatabaseQuery.HINT_SINGLE).getKNNForObject(obj, k);
  }

  /**
   * Execute a single kNN query by Object
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceQuery Distance query to use
   * @param k Value of k
   * @param obj Query object
   * @return kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByObject(Database<O> database, DistanceQuery<O, D> distanceQuery, int k, O obj) {
    return database.getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_SINGLE).getKNNForObject(obj, k);
  }

  /**
   * Execute a single Range query by Object DBID
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceFunction Distance function to use
   * @param range Query range
   * @param id DBID to query
   * @return neighbors of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByDBID(Database<O> database, DistanceFunction<? super O, D> distanceFunction, D range, DBID id) {
    return database.getRangeQuery(distanceFunction, range, DatabaseQuery.HINT_SINGLE).getRangeForDBID(id, range);
  }

  /**
   * Execute a single Range query by Object DBID
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceQuery Distance query to use
   * @param range Query range
   * @param id DBID to query
   * @return neighbors of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByDBID(Database<O> database, DistanceQuery<O, D> distanceQuery, D range, DBID id) {
    return database.getRangeQuery(distanceQuery, range, DatabaseQuery.HINT_SINGLE).getRangeForDBID(id, range);
  }

  /**
   * Execute a single Range query by Object
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceFunction Distance function to use
   * @param range Query range
   * @param obj Query object
   * @return neighbors of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByObject(Database<O> database, DistanceFunction<? super O, D> distanceFunction, D range, O obj) {
    return database.getRangeQuery(distanceFunction, range, DatabaseQuery.HINT_SINGLE).getRangeForObject(obj, range);
  }

  /**
   * Execute a single Range query by Object
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceQuery Distance query to use
   * @param range Query range
   * @param obj Query object
   * @return neighbors of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByObject(Database<O> database, DistanceQuery<O, D> distanceQuery, D range, O obj) {
    return database.getRangeQuery(distanceQuery, range, DatabaseQuery.HINT_SINGLE).getRangeForObject(obj, range);
  }

  /**
   * Execute a single reverse kNN query by Object DBID
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceFunction Distance function to use
   * @param k Value of k
   * @param id DBID to query
   * @return reverse kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRKNNQueryByDBID(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, DBID id) {
    return database.getRKNNQuery(distanceFunction, k, DatabaseQuery.HINT_SINGLE).getRKNNForDBID(id, k);
  }

  /**
   * Execute a single reverse kNN query by Object DBID
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceQuery Distance query to use
   * @param k Value of k
   * @param id DBID to query
   * @return reverse kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRKNNQueryByDBID(Database<O> database, DistanceQuery<O, D> distanceQuery, int k, DBID id) {
    return database.getRKNNQuery(distanceQuery, k, DatabaseQuery.HINT_SINGLE).getRKNNForDBID(id, k);
  }

  /**
   * Execute a single reverse kNN query by Object
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceFunction Distance function to use
   * @param k Value of k
   * @param obj Query object
   * @return reverse kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRKNNQueryByObject(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, O obj) {
    return database.getRKNNQuery(distanceFunction, k, DatabaseQuery.HINT_SINGLE).getRKNNForObject(obj, k);
  }

  /**
   * Execute a single reverse kNN query by Object
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database to query
   * @param distanceQuery Distance query to use
   * @param k Value of k
   * @param obj Query object
   * @return reverse kNN of object
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRKNNQueryByObject(Database<O> database, DistanceQuery<O, D> distanceQuery, int k, O obj) {
    return database.getRKNNQuery(distanceQuery, k, DatabaseQuery.HINT_SINGLE).getRKNNForObject(obj, k);
  }
}