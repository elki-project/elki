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
package elki.distance.external;

import java.io.InputStream;

/**
 * Parse distances from an input stream into a distance cache for storing.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @navassoc - call - DistanceParser.DistanceCacheWriter
 */
public interface DistanceParser {
  /**
   * Returns a list of the objects parsed from the specified input stream and a
   * list of the labels associated with the objects.
   * 
   * @param in the stream to parse objects from
   * @param cache Cache writer
   */
  void parse(InputStream in, DistanceCacheWriter cache);

  /**
   * Interface to plug in the cache storage into the parser.
   * 
   * @author Erich Schubert
   * @since 0.7.0
   */
  @FunctionalInterface
  interface DistanceCacheWriter {
    /**
     * Puts the specified distance value for the given ids to the distance
     * cache.
     *
     * @param id1 the first id offset
     * @param id2 the second id offset
     * @param distance the distance value
     */
    void put(int id1, int id2, double distance);
  }
}
