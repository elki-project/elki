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
package elki.database.relation;

import java.util.function.BiConsumer;

import elki.data.type.SimpleTypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDs;

/**
 * An object representation from a database.
 * <p>
 * To <b>search</b> the relation, use {@link elki.database.query.QueryBuilder}.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - DBIDRef
 * @opt hide TooltipStringVisualization
 *
 * @param <O> Object type
 */
public interface Relation<O> {
  /**
   * Get the representation of an object.
   *
   * @param id Object ID
   * @return object instance
   */
  O get(DBIDRef id);

  /**
   * Get the data type of this representation
   *
   * @return Data type
   */
  SimpleTypeInformation<O> getDataTypeInformation();

  /**
   * Get the IDs the query is defined for.
   * <p>
   * If possible, prefer {@link #iterDBIDs()}.
   *
   * @return IDs this is defined for
   */
  DBIDs getDBIDs();

  /**
   * Get an iterator access to the DBIDs.
   * <p>
   * To iterate over all IDs, use the following code fragment:
   *
   * <pre>
   * {@code
   * for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
   *    relation.get(iter); // Get the current element
   * }
   * }
   * </pre>
   *
   * @return iterator for the DBIDs.
   */
  DBIDIter iterDBIDs();

  /**
   * Get the number of DBIDs.
   *
   * @return Size
   */
  int size();

  /**
   * Get a long (human readable) name for the relation.
   *
   * @return Relation name
   */
  // @Override // Used to be in "Result"
  String getLongName();

  /**
   * Execute a function for each ID.
   *
   * @param action Action to execute
   */
  default void forEach(BiConsumer<? super DBIDRef, ? super O> action) {
    for(DBIDIter it = iterDBIDs(); it.valid(); it.advance()) {
      action.accept(it, get(it));
    }
  }
}
