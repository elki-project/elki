package de.lmu.ifi.dbs.elki.database;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanPrimitiveDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanRawDoubleDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanPrimitiveDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanRawDoubleDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;

/**
 * Static class with utilities related to querying a database.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.uses Database
 * @apiviz.uses Relation
 * @apiviz.has DistanceQuery
 * @apiviz.has SimilarityQuery
 * @apiviz.has KNNQuery
 * @apiviz.has RangeQuery
 * @apiviz.has RKNNQuery
 */
public final class QueryUtil {
  /**
   * Get a distance query for a given distance function, automatically choosing
   * a relation.
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
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @return KNN Query object
   */
  public static <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final Database database = relation.getDatabase();
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distanceFunction, hints);
    return database.getKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a range query object for the given distance function.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Range: maximum range requested</li>
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
  public static <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Database database, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final Relation<O> relation = database.getRelation(distanceFunction.getInputTypeRestriction(), hints);
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distanceFunction, hints);
    return database.getRangeQuery(distanceQuery, hints);
  }

  /**
   * Get a range query object for the given distance function.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Range: maximum range requested</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @return KNN Query object
   */
  public static <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final Database database = relation.getDatabase();
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distanceFunction, hints);
    return database.getRangeQuery(distanceQuery, hints);
  }

  /**
   * Get a rKNN query object for the given distance function.
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
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @return RKNN Query object
   */
  public static <O, D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    final Database database = relation.getDatabase();
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distanceFunction, hints);
    return database.getRKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a linear scan query for the given distance query.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param distanceQuery distance query
   * @return KNN query
   */
  public static <O, D extends Distance<D>> KNNQuery<O, D> getLinearScanKNNQuery(DistanceQuery<O, D> distanceQuery) {
    // Slight optimizations of linear scans
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      if(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
        final PrimitiveDistanceQuery<O, ?> pdq = (PrimitiveDistanceQuery<O, ?>) distanceQuery;
        @SuppressWarnings("unchecked")
        final KNNQuery<O, ?> knnQuery = new LinearScanRawDoubleDistanceKNNQuery<O>((PrimitiveDistanceQuery<O, DoubleDistance>) pdq);
        @SuppressWarnings("unchecked")
        final KNNQuery<O, D> castQuery = (KNNQuery<O, D>) knnQuery;
        return castQuery;
      }
      else {
        final PrimitiveDistanceQuery<O, D> pdq = (PrimitiveDistanceQuery<O, D>) distanceQuery;
        return new LinearScanPrimitiveDistanceKNNQuery<O, D>(pdq);
      }
    }
    return new LinearScanKNNQuery<O, D>(distanceQuery);
  }

  /**
   * Get a linear scan query for the given distance query.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param distanceQuery distance query
   * @return Range query
   */
  public static <O, D extends Distance<D>> RangeQuery<O, D> getLinearScanRangeQuery(DistanceQuery<O, D> distanceQuery) {
    // Slight optimizations of linear scans
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      if(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction) {
        final PrimitiveDistanceQuery<O, ?> pdq = (PrimitiveDistanceQuery<O, ?>) distanceQuery;
        @SuppressWarnings("unchecked")
        final RangeQuery<O, ?> knnQuery = new LinearScanRawDoubleDistanceRangeQuery<O>((PrimitiveDistanceQuery<O, DoubleDistance>) pdq);
        @SuppressWarnings("unchecked")
        final RangeQuery<O, D> castQuery = (RangeQuery<O, D>) knnQuery;
        return castQuery;
      }
      else {
        final PrimitiveDistanceQuery<O, D> pdq = (PrimitiveDistanceQuery<O, D>) distanceQuery;
        return new LinearScanPrimitiveDistanceRangeQuery<O, D>(pdq);
      }
    }
    return new LinearScanRangeQuery<O, D>(distanceQuery);
  }
}