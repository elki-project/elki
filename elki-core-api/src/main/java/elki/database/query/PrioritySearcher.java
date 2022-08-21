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
package elki.database.query;

import elki.database.ids.*;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;

/**
 * Distance priority-based searcher. When used with an index, this will return
 * relevant objects in - approximately - increasing order. But unless you give
 * the hint {@link elki.database.query.QueryBuilder#optimizedOnly}, the system
 * may fall back to a slow linear scan that returns objects in arbitrary order,
 * if no suitable index is available.
 * <p>
 * This searcher has fairly loose guarantees because <b>it may need to fall back
 * to a linear scan</b>. In such cases, you may be given every point in
 * arbitrary order. But if you set some thresholds, it may even then be able to
 * avoid some computations, such as computing the square root in Euclidean
 * distance.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @param <O> Object type
 */
public interface PrioritySearcher<O> extends KNNSearcher<O>, RangeSearcher<O>, DBIDIter {
  /**
   * Priority search function.
   *
   * @param query Query object
   * @param threshold Initial distance threshold
   * @return {@code this}, for chaining
   */
  default PrioritySearcher<O> search(O query, double threshold) {
    return search(query).decreaseCutoff(threshold);
  }

  /**
   * Start search with a new object.
   *
   * @param query Query object
   * @return {@code this}, for chaining
   */
  PrioritySearcher<O> search(O query);

  @Override
  default KNNList getKNN(O obj, int k) {
    final KNNHeap heap = DBIDUtil.newHeap(k);
    double threshold = Double.POSITIVE_INFINITY;
    for(PrioritySearcher<O> iter = search(obj); iter.valid(); iter.advance()) {
      if(iter.getLowerBound() > threshold) {
        continue;
      }
      double dist = iter.computeExactDistance();
      if(dist <= threshold) {
        iter.decreaseCutoff(threshold = heap.insert(dist, iter));
      }
    }
    return heap.toKNNList();
  }

  @Override
  default ModifiableDoubleDBIDList getRange(O obj, double range, ModifiableDoubleDBIDList result) {
    for(PrioritySearcher<O> iter = search(obj, range); iter.valid(); iter.advance()) {
      if(iter.getLowerBound() > range) {
        continue;
      }
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
  PrioritySearcher<O> decreaseCutoff(double threshold);

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
   * <p>
   * Note: the lower bound is already checked by the cutoff of the priority
   * search, so this is primarily useful for analyzing the search behavior.
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

  /**
   * Lower bound for all subsequent instances (that have been completely
   * explored). The searcher guarantees that no further results will be returned
   * with a distance less than this.
   *
   * @return lower bound; {@code 0} if no guarantees (e.g., linear scan)
   */
  double allLowerBound();

  @Override
  PrioritySearcher<O> advance();
}
