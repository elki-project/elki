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
package de.lmu.ifi.dbs.elki.index.lsh.hashfunctions;

/**
 * Hash functions as used by locality sensitive hashing.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <V> Data type to hash.
 */
public interface LocalitySensitiveHashFunction<V> {
  /**
   * Compute the hash value of an object.
   *
   * @param obj Object to hash
   * @return Hash value
   */
  int hashObject(V obj);

  /**
   * Compute the hash value of an object (faster version).
   *
   * @param obj Object to hash
   * @param buf Buffer, sized according to the number of projections.
   * @return Hash value
   */
  int hashObject(V obj, double[] buf);

  /**
   * Get the number of projections performed.
   *
   * @return Number of projections.
   */
  int getNumberOfProjections();
}
