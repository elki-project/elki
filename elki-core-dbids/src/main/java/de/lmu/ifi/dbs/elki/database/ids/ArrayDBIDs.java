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
 * Interface for array based DBIDs.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DBIDArrayIter
 */
public interface ArrayDBIDs extends DBIDs {
  /**
   * Get the i'th entry (starting at 0)
   * <p>
   * If possible, use an {@link DBIDArrayIter} via {@link #iter()} instead, or
   * an {@link DBIDVar} via {@link #assignVar}
   *
   * @param i Index
   * @return DBID of i'th entry.
   * @deprecated Preferably use a {@link DBIDArrayIter} or a {@link DBIDVar}
   *             instead of materializing expensive {@link DBID} objects.
   */
  @Deprecated
  DBID get(int i);

  /**
   * Assign a DBID variable the value of position {@code index}.
   *
   * @param index Position
   * @param var Variable to assign the value to.
   */
  DBIDVar assignVar(int index, DBIDVar var);

  /**
   * Iterable
   *
   * @return Iterator
   */
  @Override
  DBIDArrayIter iter();

  /**
   * Size of the DBID "collection".
   *
   * @return size
   */
  @Override
  int size();

  /**
   * Search for the position of the given key, assuming that the data set is
   * sorted. For unsorted arrays, the result is undefined.
   * <p>
   * For keys not found, <code>-(1+insertion position)</code> is returned, as
   * for Java {@link java.util.Collections#binarySearch}
   *
   * @param key Key to search for
   * @return Offset of key
   */
  int binarySearch(DBIDRef key);

  /**
   * Slice a subarray (as view, not copy!)
   *
   * @param begin Begin (inclusive)
   * @param end End (exclusive)
   * @return Array slice.
   */
  ArrayDBIDs slice(int begin, int end);
}
