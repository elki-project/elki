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
package de.lmu.ifi.dbs.elki.database.ids;

/**
 * Interface for a generic modifiable DBID collection.
 *
 * Note: we had this use the Java Collections API for a long time, however this
 * prevented certain optimizations. So it now only mimics the Collections API
 * <em>deliberately</em>.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DBIDMIter
 */
public interface ModifiableDBIDs extends DBIDs {
  /**
   * Add DBIDs to collection.
   *
   * @param ids IDs to add.
   * @return {@code true} if the collection changed.
   */
  boolean addDBIDs(DBIDs ids);

  /**
   * Remove DBIDs from collection.
   *
   * @param ids IDs to remove.
   * @return {@code true} if the collection changed.
   */
  boolean removeDBIDs(DBIDs ids);

  /**
   * Add a single DBID to the collection.
   *
   * @param id ID to add
   * @return {@code true} if the collection changed.
   */
  boolean add(DBIDRef id);

  /**
   * Remove a single DBID from the collection.
   *
   * @param id ID to remove
   * @return {@code true} if the collection changed.
   */
  boolean remove(DBIDRef id);

  /**
   * Clear this collection.
   */
  void clear();

  /**
   * Get a <em>modifiable</em> DBID iterator (a more efficient API).
   *
   * usage example:
   *
   * <pre>
   * {@code
   * for(DBIDMIter iter = ids.iter(); iter.valid(); iter.advance()) {
   *   DBID id = iter.getDBID();
   *   iter.remove();
   * }
   * }
   * </pre>
   *
   * @return modifiable iterator
   */
  @Override
  DBIDMIter iter();

  /**
   * Pop (get and remove) one DBID from the set, into a variable.
   *
   * @param outvar Output variable
   * @return Always {@code outvar}, for inlining
   * @throws ArrayIndexOutOfBoundsException when empty.
   */
  DBIDVar pop(DBIDVar outvar);
}