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
package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Interface for a result providing an object ordering.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public interface OrderingResult extends Result {
  /**
   * Get the full set of DBIDs this ordering is defined for.
   * 
   * @return DBIDs
   */
  DBIDs getDBIDs();
  
  /**
   * Sort the given ids according to this ordering and return an iterator.
   * 
   * @param ids Collection of ids.
   * @return iterator for sorted array of ids
   */
  ArrayModifiableDBIDs order(DBIDs ids);
}
