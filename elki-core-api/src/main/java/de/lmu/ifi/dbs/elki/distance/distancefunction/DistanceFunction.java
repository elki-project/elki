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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Base interface for any kind of distances.
 * 
 * @author Erich Schubert
 * @since 0.1
 * 
 * @param <O> Object type
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @has - - - TypeInformation
 */
public interface DistanceFunction<O> {
  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  default boolean isSymmetric() {
    return true;
  }

  /**
   * Is this distance function metric (satisfy the triangle inequality)
   * 
   * @return {@code true} when metric.
   */
  default boolean isMetric() {
    return false;
  }

  /**
   * Squared distances, that would become metric after square root.
   * 
   * E.g. squared Euclidean.
   * 
   * @return {@code true} when squared.
   */
  default boolean isSquared() {
    return false;
  }

  /**
   * Get the input data type of the function.
   * 
   * @return Type restriction
   */
  TypeInformation getInputTypeRestriction();

  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param relation The representation to use
   * @return Actual distance query.
   */
  <T extends O> DistanceQuery<T> instantiate(Relation<T> relation);
}
