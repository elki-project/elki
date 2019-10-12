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

import elki.database.query.distance.DistanceQuery;
import elki.database.query.rknn.RKNNQuery;

/**
 * Index with support for kNN queries.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @navhas - provides - RKNNQuery
 * 
 * @param <O> Database Object type
 */
public interface RKNNIndex<O> extends Index {
  /**
   * Get a RKNN query object for the given distance query and k.
   * <p>
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param distanceQuery Distance query
   * @param maxk Maximum k for RkNN query
   * @param flags Hints for the optimizer
   * @return RKNN Query object or {@code null}
   */
  RKNNQuery<O> getRKNNQuery(DistanceQuery<O> distanceQuery, int maxk, int flags);
}
