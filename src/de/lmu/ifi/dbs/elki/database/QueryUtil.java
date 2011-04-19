package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;

/**
 * Static class with utilities related to querying a database.
 * 
 * @author Erich Schubert
 */
public final class QueryUtil {
  /**
   * Get a distance query for a given distance function, automatically choosing a relation.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * @return Distance query
   */
  public static <O, D extends Distance<D>> DistanceQuery<O, D> getDistanceQuery(Database database, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final Relation<O> objectQuery = database.getRelation(distanceFunction.getInputTypeRestriction(), hints);
    return database.getDistanceQuery(objectQuery, distanceFunction, hints);
  }

  /**
   * Get a similarity query, automatically choosing a relation.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database
   * @param similarityFunction Similarity function
   * @param hints Optimizer hints
   * @return Similarity Query
   */
  public static <O, D extends Distance<D>> SimilarityQuery<O, D> getSimilarityQuery(Database database, SimilarityFunction<? super O, D> similarityFunction, Object... hints) {
    final Relation<O> objectQuery = database.getRelation(similarityFunction.getInputTypeRestriction(), hints);
    return database.getSimilarityQuery(objectQuery, similarityFunction, hints);
  }

  /**
   * Get a KNN query object for the given distance function.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  public static <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Database database, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final Relation<O> relation = database.getRelation(distanceFunction.getInputTypeRestriction(), hints);
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distanceFunction, hints);
    return database.getKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a KNN query object for the given distance function.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param database Database
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  public static <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Database database, Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distanceFunction, hints);
    return database.getKNNQuery(distanceQuery, hints);
  }
}