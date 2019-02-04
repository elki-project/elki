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
package de.lmu.ifi.dbs.elki.database.query.range;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;

/**
 * The interface for range queries, that can return all objects within the
 * specified radius.
 *
 * Do not confuse this with rectangular window queries, which are also commonly
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
public interface RangeQuery<O> extends DatabaseQuery {
  /**
   * Get the neighbors for a particular id in a given query range
   *
   * @param id query object ID
   * @param range Query range
   * @return neighbors
   */
  DoubleDBIDList getRangeForDBID(DBIDRef id, double range);

  /**
   * Get the neighbors for a particular object in a given query range
   *
   * @param obj Query object
   * @param range Query range
   * @return neighbors
   */
  DoubleDBIDList getRangeForObject(O obj, double range);

  /**
   * Get the neighbors for a particular object in a given query range
   *
   * @param id query object ID
   * @param range Query range
   * @param result Neighbors output set
   */
  void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList result);

  /**
   * Get the neighbors for a particular object in a given query range
   *
   * @param obj Query object
   * @param range Query range
   * @param result Neighbors output set
   */
  void getRangeForObject(O obj, double range, ModifiableDoubleDBIDList result);
}
