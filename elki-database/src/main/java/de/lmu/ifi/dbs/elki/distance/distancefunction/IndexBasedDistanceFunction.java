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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.database.query.distance.DatabaseDistanceQuery;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Distance function relying on an index (such as preprocessed neighborhoods).
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @opt nodefillcolor LemonChiffon
 * @stereotype factory
 * @navassoc - creates - Instance
 *
 * @param <O> Object type
 */
public interface IndexBasedDistanceFunction<O> extends DistanceFunction<O> {
  /**
   * OptionID for the index parameter
   */
  OptionID INDEX_ID = new OptionID("distancefunction.index", "Distance index to use.");

  /**
   * Instance interface for Index based distance functions.
   *
   * @author Erich Schubert
   *
   * @param <T> Object type
   */
  interface Instance<T, I extends Index> extends DatabaseDistanceQuery<T> {
    /**
     * Get the index used.
     *
     * @return the index used
     */
    I getIndex();
  }
}
