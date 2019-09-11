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
package elki.database.query.distance;

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.KNNHeap;
import elki.database.ids.KNNList;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.DatabaseQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;

/**
 * Distance priority-based searcher. When used with an index, this will return
 * relevant objects in - approximately - increasing order. But unless you give
 * the hint {@link DatabaseQuery#HINT_OPTIMIZED_ONLY}, the system may fall back
 * to a slow linear scan that returns objects in arbitrary order, if no suitable
 * index is available.
 *
 * @author Erich Schubert
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @param <O> Object type
 */
public interface DistancePrioritySearcher<O> extends KNNQuery<O>, RangeQuery<O>, DBIDIter {
  /**
   * Priority search function.
   *
   * @param query Query object
   * @param threshold Initial distance threshold
   * @return {@code this}, for chaining
   */
  default DistancePrioritySearcher<O> search(O query, double threshold) {
    return search(query).decreaseCutoff(threshold);
  }

  /**
   * Start search with a new object and threshold.
   *
   * @param query Query object
   * @param threshold Distance threshold
   * @return {@code this}, for chaining
   */
  default DistancePrioritySearcher<O> search(DBIDRef query, double threshold) {
    return search(query).decreaseCutoff(threshold);
  }

  /**
   * Start search with a new object.
   *
   * @param query Query object
   * @return {@code this}, for chaining
   */
  DistancePrioritySearcher<O> search(DBIDRef query);

  /**
   * Start search with a new object.
   *
   * @param query Query object
   * @return {@code this}, for chaining
   */
  DistancePrioritySearcher<O> search(O query);

  @Override
  default KNNList getKNNForDBID(DBIDRef id, int k) {
    KNNHeap heap = DBIDUtil.newHeap(k);
    double threshold = Double.POSITIVE_INFINITY;
    for(DistancePrioritySearcher<O> iter = search(id); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= threshold) {
        iter.decreaseCutoff(threshold = heap.insert(dist, iter));
      }
    }
    return heap.toKNNList();
  }

  @Override
  default KNNList getKNNForObject(O obj, int k) {
    KNNHeap heap = DBIDUtil.newHeap(k);
    double threshold = Double.POSITIVE_INFINITY;
    for(DistancePrioritySearcher<O> iter = search(obj); iter.valid(); iter.advance()) {
      double dist = iter.computeExactDistance();
      if(dist <= threshold) {
        iter.decreaseCutoff(threshold = heap.insert(dist, iter));
      }
    }
    return heap.toKNNList();
  }

  @Override
  default ModifiableDoubleDBIDList getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result) {
    for(DistancePrioritySearcher<O> iter = search(id, range); iter.valid(); iter.advance()) {
      final double dist = iter.computeExactDistance();
      if(dist <= range) {
        result.add(dist, iter);
      }
    }
    return result;
  }

  @Override
  default ModifiableDoubleDBIDList getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result) {
    for(DistancePrioritySearcher<O> iter = search(obj, range); iter.valid(); iter.advance()) {
      final double dist = iter.computeExactDistance();
      if(dist <= range) {
        result.add(dist, iter);
      }
    }
    return result;
  }

  /**
   * Decrease the cutoff threshold.
   * <p>
   * The cutoff must not be increased, as the search may have pruned some
   * results automatically.
   *
   * @param threshold Threshold parameter
   * @return this, for chaining
   */
  DistancePrioritySearcher<O> decreaseCutoff(double threshold);

  /**
   * Get the current object.
   *
   * @return Object
   */
  O getCandidate();

  /**
   * Compute the <em>exact</em> distance to the current candidate.
   * <p>
   * The searcher may or may not have this value already.
   *
   * @return Distance
   */
  double computeExactDistance();

  /**
   * Get approximate distance (if available).
   * <p>
   * Quality guarantees may vary a lot!
   *
   * @return {@code Double.NaN} if not valid
   */
  default double getApproximateDistance() {
    return Double.NaN;
  }

  /**
   * Get approximate distance accuracy (if available).
   * <p>
   * Quality guarantees may vary a lot!
   *
   * @return {@code Double.NaN} if not valid
   */
  default double getApproximateAccuracy() {
    return Double.NaN;
  }

  /**
   * Get the lower bound (if available).
   *
   * @return {@code Double.NaN} if not valid
   */
  default double getLowerBound() {
    return Double.NaN;
  }

  /**
   * Get the upper bound (if available).
   *
   * @return {@code Double.NaN} if not valid
   */
  default double getUpperBound() {
    return Double.NaN;
  }
}
