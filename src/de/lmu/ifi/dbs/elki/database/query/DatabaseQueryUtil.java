package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Utility classes for Database Query handling.
 * 
 * @author Erich Schubert
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
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByID(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, DBID id) {
    return database.getKNNQuery(distanceFunction, k, KNNQuery.HINT_SINGLE).getKNNForDBID(id, k);
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
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByID(Database<O> database, DistanceQuery<O, D> distanceQuery, int k, DBID id) {
    return database.getKNNQuery(distanceQuery, k, KNNQuery.HINT_SINGLE).getKNNForDBID(id, k);
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
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByObject(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k, O obj) {
    return database.getKNNQuery(distanceFunction, k, KNNQuery.HINT_SINGLE).getKNNForObject(obj, k);
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
   * @return
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleKNNQueryByObject(Database<O> database, DistanceQuery<O, D> distanceQuery, int k, O obj) {
    return database.getKNNQuery(distanceQuery, k, KNNQuery.HINT_SINGLE).getKNNForObject(obj, k);
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
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByID(Database<O> database, DistanceFunction<? super O, D> distanceFunction, D range, DBID id) {
    return database.getRangeQuery(distanceFunction, range, RangeQuery.HINT_SINGLE).getRangeForDBID(id, range);
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
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByID(Database<O> database, DistanceQuery<O, D> distanceQuery, D range, DBID id) {
    return database.getRangeQuery(distanceQuery, range, RangeQuery.HINT_SINGLE).getRangeForDBID(id, range);
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
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByObject(Database<O> database, DistanceFunction<? super O, D> distanceFunction, D range, O obj) {
    return database.getRangeQuery(distanceFunction, range, RangeQuery.HINT_SINGLE).getRangeForObject(obj, range);
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
   * @return
   */
  public static <O extends DatabaseObject, D extends Distance<D>> List<DistanceResultPair<D>> singleRangeQueryByObject(Database<O> database, DistanceQuery<O, D> distanceQuery, D range, O obj) {
    return database.getRangeQuery(distanceQuery, range, RangeQuery.HINT_SINGLE).getRangeForObject(obj, range);
  }
}