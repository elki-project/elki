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
package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Index that supports dynamic insertions and removals.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 *
 */
public interface DynamicIndex extends Index {
  /**
   * Deletes the specified object from this index.
   * 
   * @param id Object to remove
   * @return true if this index did contain the object, false otherwise
   */
  boolean delete(DBIDRef id);

  /**
   * Inserts the specified object into this index.
   * 
   * @param id the object to be inserted
   */
  void insert(DBIDRef id);

  /**
   * Deletes the specified objects from this index.
   * 
   * @param ids Objects to remove
   */
  void deleteAll(DBIDs ids);

  /**
   * Inserts the specified objects into this index. If a bulk load mode is
   * implemented, the objects are inserted in one bulk.
   * 
   * @param ids the objects to be inserted
   */
  void insertAll(DBIDs ids);
}
