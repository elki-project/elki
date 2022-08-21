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
package elki.database.query.knn;

import elki.data.NumberVector;
import elki.database.ids.*;
import elki.database.query.distance.PrimitiveDistanceQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;

/**
 * Instance of this query for a particular database.
 * <p>
 * This is a subtle optimization: for primitive queries, it is clearly faster to
 * retrieve the query object from the relation only once, and to first find the
 * nearest neighbors with squared Euclidean distances, then only compute the
 * square root for the results.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - PrimitiveDistanceQuery
 * @assoc - - - EuclideanDistance
 * @assoc - - - SquaredEuclideanDistance
 * 
 * @param <O> relation object type
 */
public class LinearScanEuclideanKNNByObject<O extends NumberVector> extends LinearScanPrimitiveKNNByObject<O> {
  /**
   * Constructor.
   *
   * @param distanceQuery Distance function to use
   */
  public LinearScanEuclideanKNNByObject(PrimitiveDistanceQuery<O> distanceQuery) {
    super(distanceQuery);
    assert (EuclideanDistance.STATIC.equals(distanceQuery.getDistance()));
  }

  @Override
  public KNNList getKNN(O obj, int k) {
    final SquaredEuclideanDistance squared = SquaredEuclideanDistance.STATIC;
    final Relation<? extends O> relation = this.relation;
    final KNNHeap heap = DBIDUtil.newHeap(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      final double dist = squared.distance(obj, relation.get(iter));
      max = dist <= max ? heap.insert(dist, iter) : max;
    }
    return heap.toKNNListSqrt();
  }
}
