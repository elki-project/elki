/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.spatial.rstarvariants.query;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.database.query.PrioritySearcher;
import elki.database.query.distance.SpatialDistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.distance.SpatialPrimitiveDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTree;

/**
 * Utility class for RStar trees.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @assoc - - - AbstractRStarTree
 * @assoc - - - EuclideanRStarTreeKNNQuery
 * @assoc - - - EuclideanRStarTreeRangeQuery
 * @assoc - - - RStarTreeKNNSearcher
 * @assoc - - - RStarTreeRangeSearcher
 * @has - - - RangeSearcher
 * @has - - - KNNSearcher
 */
public final class RStarTreeUtil {
  /**
   * Private constructor. Static methods only.
   */
  private RStarTreeUtil() {
    // Do not use.
  }

  /**
   * Get an RTree range query, using an optimized version for Euclidean
   * distances.
   * 
   * @param <O> Object type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings("unchecked")
  public static <O extends SpatialComparable> RangeSearcher<O> getRangeQuery(AbstractRStarTree<?, ?, ?> tree, SpatialDistanceQuery<O> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistance<? super O> df = distanceQuery.getDistance();
    if(EuclideanDistance.STATIC.equals(df)) {
      return (RangeSearcher<O>) new EuclideanRStarTreeRangeQuery<>(tree, (Relation<NumberVector>) distanceQuery.getRelation());
    }
    return new RStarTreeRangeSearcher<>(tree, distanceQuery.getRelation(), df);
  }

  /**
   * Get an RTree knn query, using an optimized version for Euclidean distances.
   * 
   * @param <O> Object type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings("unchecked")
  public static <O extends SpatialComparable> KNNSearcher<O> getKNNQuery(AbstractRStarTree<?, ?, ?> tree, SpatialDistanceQuery<O> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistance<? super O> df = distanceQuery.getDistance();
    if(EuclideanDistance.STATIC.equals(df)) {
      return (KNNSearcher<O>) new EuclideanRStarTreeKNNQuery<>(tree, (Relation<NumberVector>) distanceQuery.getRelation());
    }
    return new RStarTreeKNNSearcher<>(tree, distanceQuery.getRelation(), df);
  }

  /**
   * Get an RTree priority searcher.
   * 
   * @param <O> Object type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings("unchecked")
  public static <O extends SpatialComparable> PrioritySearcher<O> getDistancePrioritySearcher(AbstractRStarTree<?, ?, ?> tree, SpatialDistanceQuery<O> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistance<? super O> df = distanceQuery.getDistance();
    if(EuclideanDistance.STATIC.equals(df)) {
      return (PrioritySearcher<O>) new EuclideanRStarTreeDistancePrioritySearcher<>(tree, (Relation<NumberVector>) distanceQuery.getRelation());
    }
    return new RStarTreeDistancePrioritySearcher<>(tree, distanceQuery.getRelation(), df);
  }
}
