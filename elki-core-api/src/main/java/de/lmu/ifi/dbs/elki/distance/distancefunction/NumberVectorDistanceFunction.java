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

import de.lmu.ifi.dbs.elki.data.NumberVector;

/**
 * Base interface for the common case of distance functions defined on numerical
 * vectors.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @param <O> vector type, usually NumberVector or a parent type
 */
public interface NumberVectorDistanceFunction<O> extends PrimitiveDistanceFunction<O> {
  /**
   * Computes the distance between two given vectors according to this distance
   * function.
   *
   * @param o1 first vector
   * @param o2 second vector
   * @return the distance between two given vectors according to this distance
   *         function
   */
  double distance(NumberVector o1, NumberVector o2);
}
