package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query;

import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

/**
 * Query utility classes for MTrees.
 * 
 * @author Erich Schubert
 */
public final class MTreeQueryUtil {
  /**
   * Get an RTree knn query, using an optimized double implementation when
   * possible.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings({ "cast", "unchecked" })
  public static <O, D extends NumberDistance<D, ?>> KNNQuery<O, D> getKNNQuery(AbstractMTree<O, D, ?, ?, ?> tree, DistanceQuery<O, D> distanceQuery, Object... hints) {
    DistanceFunction<? super O, D> df = distanceQuery.getDistanceFunction();
    // Can we use an optimized query?
    if (df instanceof PrimitiveDoubleDistanceFunction) {
      PrimitiveDoubleDistanceFunction<? super O> dfc = (PrimitiveDoubleDistanceFunction<? super O>) df;
      AbstractMTree<O, DoubleDistance, ?, ?, ?> treec = (AbstractMTree<O, DoubleDistance, ?, ?, ?>) tree;
      DistanceQuery<O, DoubleDistance> dqc = (DistanceQuery<O, DoubleDistance>) distanceQuery;
      KNNQuery<O, ?> q = new DoubleDistanceMetricalIndexKNNQuery<>(treec, dqc, dfc);
      return (KNNQuery<O, D>) q;
    }
    return new MetricalIndexKNNQuery<>(tree, distanceQuery);
  }

  /**
   * Get an RTree knn query, using an optimized double implementation when
   * possible.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param tree Tree to query
   * @param distanceQuery distance query
   * @param hints Optimizer hints
   * @return Query object
   */
  @SuppressWarnings({ "cast", "unchecked" })
  public static <O, D extends NumberDistance<D, ?>> RangeQuery<O, D> getRangeQuery(AbstractMTree<O, D, ?, ?, ?> tree, DistanceQuery<O, D> distanceQuery, Object... hints) {
    DistanceFunction<? super O, D> df = distanceQuery.getDistanceFunction();
    // Can we use an optimized query?
    if (df instanceof PrimitiveDoubleDistanceFunction) {
      PrimitiveDoubleDistanceFunction<? super O> dfc = (PrimitiveDoubleDistanceFunction<? super O>) df;
      AbstractMTree<O, DoubleDistance, ?, ?, ?> treec = (AbstractMTree<O, DoubleDistance, ?, ?, ?>) tree;
      DistanceQuery<O, DoubleDistance> dqc = (DistanceQuery<O, DoubleDistance>) distanceQuery;
      RangeQuery<O, ?> q = new DoubleDistanceMetricalIndexRangeQuery<>(treec, dqc, dfc);
      return (RangeQuery<O, D>) q;
    }
    return new MetricalIndexRangeQuery<>(tree, distanceQuery);
  }
}
