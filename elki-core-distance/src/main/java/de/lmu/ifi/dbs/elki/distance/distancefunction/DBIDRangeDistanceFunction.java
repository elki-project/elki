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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;

/**
 * Distance functions valid in a <em>static</em> database context only
 * (i.e. for DBIDRanges)
 *
 * For any "distance" that cannot be computed for arbitrary objects, only those
 * that exist in the database and referenced by their ID. Furthermore, the IDs
 * must be contiguous.
 *
 * Example: external precomputed distances
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - "defined on" - de.lmu.ifi.dbs.elki.database.ids.DBIDRange
 */
public interface DBIDRangeDistanceFunction extends DBIDDistanceFunction {
  /**
   * Compute the distance for two integer offsets.
   *
   * @param i1 First offset
   * @param i2 Second offset
   * @return Distance
   */
  double distance(int i1, int i2);

  /**
   * Validate the range of DBIDs to use. This will log a warning if an obvious
   * mismatch was found.
   *
   * @param range DBID range
   */
  void checkRange(DBIDRange range);
}
