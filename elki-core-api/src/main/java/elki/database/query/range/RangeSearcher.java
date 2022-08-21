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
package elki.database.query.range;

import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.ModifiableDoubleDBIDList;

/**
 * The interface for range queries, that can return all objects within the
 * specified radius.
 * <p>
 * Do not confuse this with rectangular window queries, which are also sometimes
 * called "range queries".
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @navassoc - create - DoubleDBIDList
 *
 * @param <O> Object type
 */
public interface RangeSearcher<O> {
  /**
   * Get the neighbors for a particular object in a given query range.
   *
   * @param query query object
   * @param range Query range
   * @return neighbors
   */
  default DoubleDBIDList getRange(O query, double range) {
    return getRange(query, range, DBIDUtil.newDistanceDBIDList()).sort();
  }

  /**
   * Get the neighbors for a particular id in a given query range.
   *
   * @param query query object ID
   * @param range Query range
   * @param result Output data structure
   * @return neighbors
   */
  ModifiableDoubleDBIDList getRange(O query, double range, ModifiableDoubleDBIDList result);
}
