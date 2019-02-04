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
package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

/**
 * Index with support for distance queries (e.g. precomputed distance matrixes,
 * caches)
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @navhas - provides - DistanceQuery
 * 
 * @param <O> Object type
 */
public interface DistanceIndex<O> extends Index {
  /**
   * Get a KNN query object for the given distance query and k.
   * 
   * This function MAY return null, when the given distance is not supported!
   * 
   * @param distanceFunction Distance function to use.
   * @param hints Hints for the optimizer
   * @return KNN Query object or {@code null}
   */
  DistanceQuery<O> getDistanceQuery(DistanceFunction<? super O> distanceFunction, Object... hints);
}