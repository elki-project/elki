/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.database;

import elki.data.NumberVector;
import elki.database.query.distance.*;
import elki.database.query.knn.KNNQuery;
import elki.database.query.knn.LinearScanDistanceKNNQuery;
import elki.database.query.knn.LinearScanEuclideanDistanceKNNQuery;
import elki.database.query.knn.LinearScanPrimitiveDistanceKNNQuery;
import elki.database.query.range.*;
import elki.database.query.similarity.PrimitiveSimilarityQuery;
import elki.database.query.similarity.SimilarityQuery;
import elki.distance.minkowski.EuclideanDistance;

/**
 * Static class with utilities related to querying a database.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @assoc - - - Database
 * @assoc - - - Relation
 * @has - - - KNNQuery
 * @has - - - RangeQuery
 * @has - - - DistancePrioritySearcher
 */
public final class QueryUtil {
  /**
   * Private constructor. Static methods only.
   */
  private QueryUtil() {
    // Do not use.
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
      if(EuclideanDistance.STATIC.equals(pdq.getDistance())) {
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
      if(EuclideanDistance.STATIC.equals(pdq.getDistance())) {
        final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) pdq;
        return (RangeQuery<O>) new LinearScanEuclideanDistanceRangeQuery<>(ndq);
      }
      return new LinearScanPrimitiveDistanceRangeQuery<>(pdq);
    }
    return new LinearScanDistanceRangeQuery<>(distanceQuery);
  }

  /**
   * Get a linear scan query for the given similarity query.
   *
   * @param <O> Object type
   * @param simQuery similarity query
   * @return Range query
   */
  public static <O> RangeQuery<O> getLinearScanSimilarityRangeQuery(SimilarityQuery<O> simQuery) {
    // Slight optimizations of linear scans
    if(simQuery instanceof PrimitiveSimilarityQuery) {
      final PrimitiveSimilarityQuery<O> pdq = (PrimitiveSimilarityQuery<O>) simQuery;
      return new LinearScanPrimitiveSimilarityRangeQuery<>(pdq);
    }
    return new LinearScanSimilarityRangeQuery<>(simQuery);
  }

  /**
   * Get a linear scan query for the given similarity query.
   *
   * @param <O> Object type
   * @param distanceQuery distance query
   * @return Priority searcher
   */
  @SuppressWarnings("unchecked")
  public static <O> DistancePrioritySearcher<O> getLinearScanPrioritySearcher(DistanceQuery<O> distanceQuery) {
    if(EuclideanDistance.STATIC.equals(distanceQuery.getDistance())) {
      final PrimitiveDistanceQuery<NumberVector> ndq = (PrimitiveDistanceQuery<NumberVector>) distanceQuery;
      return (DistancePrioritySearcher<O>) new LinearScanEuclideanDistancePrioritySearcher<>(ndq);
    }
    return new LinearScanDistancePrioritySearcher<>(distanceQuery);
  }
}
