/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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
package elki.database.query;

import elki.database.query.distance.DistancePrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.query.rknn.RKNNQuery;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.similarity.Similarity;

/**
 * Interface to automatically add indexes to a database when no suitable indexes
 * have been found. The dummy implementation {@link DisableQueryOptimizer} can
 * be used to disable this functionality, for example for benchmarking purposes.
 * 
 * @author Erich Schubert
 */
public interface QueryOptimizer {
  /**
   * Optimize a similarity query for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param similarityFunction Similarity function
   * @param flags Optimizer flags
   * @return optimized query, if possible
   */
  default <O> SimilarityQuery<O> getSimilarityQuery(Relation<? extends O> relation, Similarity<? super O> similarityFunction, int flags) {
    return null;
  }

  /**
   * Optimize a distance query for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param distanceFunction Distance function
   * @param flags Optimizer flags
   * @return optimized query, if possible
   */
  default <O> DistanceQuery<O> getDistanceQuery(Relation<? extends O> relation, Distance<? super O> distanceFunction, int flags) {
    return null;
  }

  /**
   * Optimize a kNN query for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param distanceQuery distance query
   * @param maxk Maximum k
   * @param flags Optimizer flags
   * @return optimized query, if possible
   */
  default <O> KNNQuery<O> getKNNQuery(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return null;
  };

  /**
   * Optimize a range query for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param distanceQuery distance query
   * @param maxrange Maximum range
   * @param flags Optimizer flags
   * @return optimized query, if possible
   */
  default <O> RangeQuery<O> getRangeQuery(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return null;
  };

  /**
   * Optimize a range query for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param simQuery similarity query
   * @param maxrange Maximum range
   * @param flags Optimizer flags
   * @return optimized query, if possible
   */
  default <O> RangeQuery<O> getSimilarityRangeQuery(Relation<? extends O> relation, SimilarityQuery<O> simQuery, double maxrange, int flags) {
    return null;
  };

  /**
   * Optimize a reverse nearest neighbors query for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param distanceQuery distance query
   * @param flags Optimizer flags
   * @param maxk Maximum k to query
   * @return optimized query, if possible
   */
  default <O> RKNNQuery<O> getRKNNQuery(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return null;
  };

  /**
   * Optimize a distance priority search for this relation.
   *
   * @param <O> Object type
   * @param relation Data relation
   * @param distanceQuery distance query
   * @param maxrange Maximum range
   * @param flags Optimizer flags
   * @return optimized query, if possible
   */
  default <O> DistancePrioritySearcher<O> getPrioritySearcher(Relation<? extends O> relation, DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return null;
  };
}
