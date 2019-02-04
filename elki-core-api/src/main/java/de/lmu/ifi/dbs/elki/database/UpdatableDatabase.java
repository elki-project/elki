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
package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.datasource.bundle.ObjectBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;

/**
 * Database API with updates. Static databases allow for certain optimizations
 * that cannot be done in dynamic databases.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface UpdatableDatabase extends Database {
  /**
   * Inserts the given object(s) and their associations into the database.
   *
   * @param objpackages the objects to be inserted
   * @return the IDs assigned to the inserted objects
   */
  DBIDs insert(ObjectBundle objpackages);

  /**
   * Removes and returns the specified objects with the given ids from the
   * database.
   *
   * @param ids the ids of the object to be removed from the database
   * @return the objects that have been removed
   */
  ObjectBundle delete(DBIDs ids);

  /**
   * Removes and returns the specified objects with the given ids from the
   * database.
   *
   * @param id the id of the object to be removed from the database
   * @return the object that have been removed
   */
  SingleObjectBundle delete(DBIDRef id);
}