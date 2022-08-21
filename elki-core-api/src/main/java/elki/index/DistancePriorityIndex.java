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
package elki.index;

import elki.database.ids.DBIDRef;
import elki.database.query.PrioritySearcher;
import elki.database.query.WrappedPrioritySearchDBIDByLookup;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.range.RangeSearcher;

/**
 * Interface for incremental priority-based search using distance functions.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Object type
 */
public interface DistancePriorityIndex<O> extends KNNIndex<O>, RangeIndex<O> {
  @Override
  default KNNSearcher<O> kNNByObject(DistanceQuery<O> distanceQuery, int maxk, int flags) {
    return priorityByObject(distanceQuery, Double.POSITIVE_INFINITY, flags);
  }

  @Override
  default RangeSearcher<O> rangeByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return priorityByObject(distanceQuery, maxrange, flags);
  }

  /**
   * Get a priority search object.
   *
   * @param distanceQuery Distance query
   * @param maxrange Maximum search range (may be
   *        {@code Double.POSITIVE_INFINITY}
   * @param flags Optimizer hints
   * @return Priority searcher
   */
  PrioritySearcher<O> priorityByObject(DistanceQuery<O> distanceQuery, double maxrange, int flags);

  /**
   * Get a priority search object.
   *
   * @param distanceQuery Distance query
   * @param maxrange Maximum search range (may be
   *        {@code Double.POSITIVE_INFINITY}
   * @param flags Optimizer hints
   * @return Priority searcher
   */
  default PrioritySearcher<DBIDRef> priorityByDBID(DistanceQuery<O> distanceQuery, double maxrange, int flags) {
    return WrappedPrioritySearchDBIDByLookup.wrap(distanceQuery.getRelation(), priorityByObject(distanceQuery, maxrange, flags));
  }
}
