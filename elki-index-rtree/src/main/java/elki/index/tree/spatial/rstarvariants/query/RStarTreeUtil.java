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
package elki.index.tree.spatial.rstarvariants.query;

import elki.data.NumberVector;
import elki.data.spatial.SpatialComparable;
import elki.database.query.distance.SpatialDistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.SpatialPrimitiveDistance;
import elki.distance.distancefunction.minkowski.EuclideanDistance;
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
 * @assoc - - - RStarTreeKNNQuery
 * @assoc - - - RStarTreeRangeQuery
 * @has - - - RangeQuery
 * @has - - - KNNQuery
 */
public final class RStarTreeUtil {
  /**
   * Private constructor. Static methods only.
   */
  private RStarTreeUtil() {
    // Do not use.
  }

  /**
   * Get an RTree range query, using an optimized double implementation when
   * possible.
   * 
   * @param <O> Object type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings({ "cast", "unchecked" })
  public static <O extends SpatialComparable> RangeQuery<O> getRangeQuery(AbstractRStarTree<?, ?, ?> tree, SpatialDistanceQuery<O> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistance<? super O> df = distanceQuery.getDistance();
    if(EuclideanDistance.STATIC.equals(df)) {
      return (RangeQuery<O>) new EuclideanRStarTreeRangeQuery<>(tree, (Relation<NumberVector>) distanceQuery.getRelation());
    }
    return new RStarTreeRangeQuery<>(tree, distanceQuery.getRelation(), df);
  }

  /**
   * Get an RTree knn query, using an optimized double implementation when
   * possible.
   * 
   * @param <O> Object type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings({ "cast", "unchecked" })
  public static <O extends SpatialComparable> KNNQuery<O> getKNNQuery(AbstractRStarTree<?, ?, ?> tree, SpatialDistanceQuery<O> distanceQuery, Object... hints) {
    // Can we support this distance function - spatial distances only!
    SpatialPrimitiveDistance<? super O> df = distanceQuery.getDistance();
    if(EuclideanDistance.STATIC.equals(df)) {
      return (KNNQuery<O>) new EuclideanRStarTreeKNNQuery<>(tree, (Relation<NumberVector>) distanceQuery.getRelation());
    }
    return new RStarTreeKNNQuery<>(tree, distanceQuery.getRelation(), df);
  }
}