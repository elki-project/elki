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

import java.util.function.Consumer;

/**
 * Interface for a collection of database references (IDs).
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - DBID
 * @has - - - DBIDIter
 */
public interface DBIDs {
  /**
   * Get a DBID iterator (a more efficient API).
   * <p>
   * Example:
   *
   * <pre>
   * {@code
   * for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
   *   DBID id = iter.getDBID();
   * }
   * }
   * </pre>
   *
   * @return iterator
   */
  DBIDIter iter();

  /**
   * Retrieve the collection / data size.
   *
   * @return collection size
   */
  int size();

  /**
   * Test whether an ID is contained.
   *
   * @param o object to test
   * @return true when contained
   */
  boolean contains(DBIDRef o);

  /**
   * Test for an empty DBID collection.
   *
   * @return true when empty.
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Execute a function for each ID.
   *
   * @param action Action to execute
   */
  default void forEach(Consumer<? super DBIDRef> action) {
    for(DBIDIter it = iter(); it.valid(); it.advance()) {
      action.accept(it);
    }
  }
}
