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
package elki.database.query.knn;

import elki.database.ids.*;
import elki.database.query.LinearScanQuery;
import elki.database.query.distance.DistanceQuery;

/**
 * Instance of this query for a particular database.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DistanceQuery
 */
public class LinearScanDistanceKNNQuery<O> extends AbstractDistanceKNNQuery<O> implements LinearScanQuery {
  /**
   * Constructor.
   *
   * @param distanceQuery Distance function to use
   */
  public LinearScanDistanceKNNQuery(DistanceQuery<O> distanceQuery) {
    super(distanceQuery);
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    final DistanceQuery<O> dq = distanceQuery;
    KNNHeap heap = DBIDUtil.newHeap(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = dq.getRelation().iterDBIDs(); iter.valid(); iter.advance()) {
      final double dist = dq.distance(id, iter);
      max = dist <= max ? heap.insert(dist, iter) : max;
    }
    return heap.toKNNList();
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    final DistanceQuery<O> dq = distanceQuery;
    KNNHeap heap = DBIDUtil.newHeap(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = dq.getRelation().iterDBIDs(); iter.valid(); iter.advance()) {
      final double dist = dq.distance(obj, iter);
      max = dist <= max ? heap.insert(dist, iter) : max;
    }
    return heap.toKNNList();
  }
}
