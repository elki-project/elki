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
 * Collection of double values associated with objects.
 * <p>
 * To iterate over the results, use the following code:
 *
 * <pre>
 * {@code
 * for (DoubleDBIDListIter iter = result.iter(); iter.valid(); iter.advance()) {
 *   // You can get the distance via: iter.doubleValue();
 *   // And use iter just like any other DBIDRef
 * }
 * }
 * </pre>
 *
 * If you are only interested in the IDs of the objects, the following is also
 * sufficient:
 *
 * <pre>
 * {@code
 * for (DBIDIter iter = result.iter(); iter.valid(); iter.advance()) {
 *   // Use iter just like any other DBIDRef
 * }
 * }
 * </pre>
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @composed - - - DoubleDBIDPair
 * @has - - - DoubleDBIDListIter
 */
public interface DoubleDBIDList extends DBIDs {
  @Override
  int size();

  /**
   * Assign a DBID variable the value of position {@code index}.
   *
   * @param index Position
   * @param var Variable to assign the value to.
   */
  DBIDVar assignVar(int index, DBIDVar var);

  /**
   * Get the double value at a given index (to access the DBID, either use
   * {@link #iter} or {@link #assignVar}.
   *
   * @param index Index
   * @return Value
   */
  double doubleValue(int index);

  @Override
  DoubleDBIDListIter iter();

  /**
   * Get a subset list.
   *
   * @param begin Begin
   * @param end End
   * @return Sublist
   */
  DoubleDBIDList slice(int begin, int end);

  /**
   * Execute a function for each ID.
   *
   * @param action Action to execute
   */
  default void forEachDouble(Consumer action) {
    for(DoubleDBIDListIter it = iter(); it.valid(); it.advance()) {
      action.accept(it, it.doubleValue());
    }
  }

  /**
   * Consumer for (DBIDRef, double) pairs.
   *
   * @author Erich Schubert
   * 
   * @assoc - iterates - DoubleDBIDList
   */
  @FunctionalInterface
  interface Consumer {
    /**
     * Act on each value.
     *
     * @param idref DBID reference
     * @param val value
     */
    void accept(DBIDRef idref, double val);
  }
}
