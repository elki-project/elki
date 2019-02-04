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
package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

/**
 * Interface to plug in the cache storage into the parser.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface DistanceCacheWriter {
  /**
   * Puts the specified distance value for the given ids to the distance cache.
   * 
   * @param id1 the first id offset
   * @param id2 the second id offset
   * @param distance the distance value
   */
  void put(int id1, int id2, double distance);
}
