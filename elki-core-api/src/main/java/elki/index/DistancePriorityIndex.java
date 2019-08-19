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
package elki.index;

import elki.database.query.distance.DistancePrioritySearcher;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.query.range.RangeQuery;

/**
 * Interface for incremental priority-based search using distance functions.
 *
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public interface DistancePriorityIndex<O> extends KNNIndex<O>, RangeIndex<O> {
  @Override
  default KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    return getPriorityQuery(distanceQuery, hints);
  }

  @Override
  default RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    return getPriorityQuery(distanceQuery, hints);
  }

  /**
   * Get a priority search object.
   *
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return Priority searcher
   */
  DistancePrioritySearcher<O> getPriorityQuery(DistanceQuery<O> distanceQuery, Object... hints);
}
