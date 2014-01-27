package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;

/**
 * Utility class for RStar trees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.uses AbstractRStarTree
 * @apiviz.uses DoubleDistanceRStarTreeKNNQuery
 * @apiviz.uses DoubleDistanceRStarTreeRangeQuery
 * @apiviz.has RangeQuery
 * @apiviz.has KNNQuery
 */
public final class RStarTreeUtil {
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
    SpatialPrimitiveDistanceFunction<? super O> df = distanceQuery.getDistanceFunction();
    DistanceQuery<O> dqc = (DistanceQuery<O>) DistanceQuery.class.cast(distanceQuery);
    RangeQuery<O> q = new RStarTreeRangeQuery<>(tree, dqc, df);
    return (RangeQuery<O>) q;
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
    SpatialPrimitiveDistanceFunction<? super O> df = distanceQuery.getDistanceFunction();
    DistanceQuery<O> dqc = (DistanceQuery<O>) DistanceQuery.class.cast(distanceQuery);
    KNNQuery<O> q = new RStarTreeKNNQuery<>(tree, dqc, df);
    return (KNNQuery<O>) q;
  }
}