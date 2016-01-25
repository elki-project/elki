package de.lmu.ifi.dbs.elki.database;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.integer.DoubleIntegerDBIDKNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanEuclideanDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanPrimitiveDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanEuclideanDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanPrimitiveDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;

/**
 * Static class with utilities related to querying a database.
 *
 * @author Erich Schubert
 * @since 0.4.0
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
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * @return Distance query
   */
  public static <O> DistanceQuery<O> getDistanceQuery(Database database, DistanceFunction<? super O> distanceFunction, Object... hints) {
    final Relation<O> objectQuery = database.getRelation(distanceFunction.getInputTypeRestriction(), hints);
    return database.getDistanceQuery(objectQuery, distanceFunction, hints);
  }

  /**
   * Get a similarity query, automatically choosing a relation.
   *
   * @param <O> Object type
   * @param database Database
   * @param similarityFunction Similarity function
   * @param hints Optimizer hints
   * @return Similarity Query
   */
  public static <O> SimilarityQuery<O> getSimilarityQuery(Database database, SimilarityFunction<? super O> similarityFunction, Object... hints) {
    final Relation<O> objectQuery = database.getRelation(similarityFunction.getInputTypeRestriction(), hints);
    return database.getSimilarityQuery(objectQuery, similarityFunction, hints);
  }

  /**
   * Get a KNN query object for the given distance function.
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   *
   * @param <O> Object type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  public static <O> KNNQuery<O> getKNNQuery(Database database, DistanceFunction<? super O> distanceFunction, Object... hints) {
    final Relation<O> relation = database.getRelation(distanceFunction.getInputTypeRestriction(), hints);
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction, hints);
    return relation.getKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a KNN query object for the given distance function.
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   *
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   *
   * @param <O> Object type
   * @return KNN Query object
   */
  public static <O> KNNQuery<O> getKNNQuery(Relation<O> relation, DistanceFunction<? super O> distanceFunction, Object... hints) {
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction, hints);
    return relation.getKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a range query object for the given distance function for radius-based
   * neighbor search. (Range queries in ELKI refers to radius-based ranges, not
   * rectangular query windows.)
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Range: maximum range requested</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   *
   * @param <O> Object type
   * @param database Database
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  public static <O> RangeQuery<O> getRangeQuery(Database database, DistanceFunction<? super O> distanceFunction, Object... hints) {
    final Relation<O> relation = database.getRelation(distanceFunction.getInputTypeRestriction(), hints);
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction, hints);
    return relation.getRangeQuery(distanceQuery, hints);
  }

  /**
   * Get a range query object for the given distance function for radius-based
   * neighbor search. (Range queries in ELKI refers to radius-based ranges, not
   * rectangular query windows.)
   *
   * An index is used when possible, but it may fall back to a linear scan.
   *
   * Hints include:
   * <ul>
   * <li>Range: maximum range requested</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   *
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   *
   * @param <O> Object type
   * @return KNN Query object
   */
  public static <O> RangeQuery<O> getRangeQuery(Relation<O> relation, DistanceFunction<? super O> distanceFunction, Object... hints) {
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction, hints);
    return relation.getRangeQuery(distanceQuery, hints);
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
   *
   * @param relation Relation used
   * @param distanceFunction Distance function
   * @param hints Optimizer hints
   *
   * @param <O> Object type
   * @return RKNN Query object
   */
  public static <O> RKNNQuery<O> getRKNNQuery(Relation<O> relation, DistanceFunction<? super O> distanceFunction, Object... hints) {
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(distanceFunction, hints);
    return relation.getRKNNQuery(distanceQuery, hints);
  }

  /**
   * Get a linear scan query for the given distance query.
   *
   * @param <O> Object type
   * @param distanceQuery distance query
   * @return KNN query
   */
  @SuppressWarnings("unchecked")
  public static <O> KNNQuery<O> getLinearScanKNNQuery(DistanceQuery<O> distanceQuery) {
    // Slight optimizations of linear scans
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistanceFunction.STATIC.equals(pdq.getDistanceFunction())) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return (KNNQuery<O>) new LinearScanEuclideanDistanceKNNQuery<>(ndq);
      }
      return new LinearScanPrimitiveDistanceKNNQuery<>(pdq);
    }
    return new LinearScanDistanceKNNQuery<>(distanceQuery);
  }

  /**
   * Get a linear scan query for the given distance query.
   *
   * @param <O> Object type
   * @param distanceQuery distance query
   * @return Range query
   */
  @SuppressWarnings("unchecked")
  public static <O> RangeQuery<O> getLinearScanRangeQuery(DistanceQuery<O> distanceQuery) {
    // Slight optimizations of linear scans
    if(distanceQuery instanceof PrimitiveDistanceQuery) {
      final PrimitiveDistanceQuery<O> pdq = (PrimitiveDistanceQuery<O>) distanceQuery;
      if(EuclideanDistanceFunction.STATIC.equals(pdq.getDistanceFunction())) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return (RangeQuery<O>) new LinearScanEuclideanDistanceRangeQuery<>(ndq);
      }
      return new LinearScanPrimitiveDistanceRangeQuery<>(pdq);
    }
    return new LinearScanDistanceRangeQuery<>(distanceQuery);
  }

  /**
   * Apply the square root function to each value in the list.
   *
   * @param knnList kNN list
   * @return new list, after taking the square root
   */
  public static KNNList applySqrt(KNNList knnList) {
    DoubleIntegerDBIDKNNList ret = new DoubleIntegerDBIDKNNList(knnList.getK(), knnList.size());
    for(DoubleDBIDListIter iter = knnList.iter(); iter.valid(); iter.advance()) {
      ret.add(Math.sqrt(iter.doubleValue()), iter);
    }
    return ret;
  }
}
