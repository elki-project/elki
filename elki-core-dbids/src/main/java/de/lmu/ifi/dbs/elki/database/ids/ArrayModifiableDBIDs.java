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

import java.util.Comparator;

/**
 * Array-oriented implementation of a modifiable DBID collection.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - DBIDArrayMIter
 */
public interface ArrayModifiableDBIDs extends ModifiableDBIDs, ArrayDBIDs {
  /**
   * Sort the DBID set.
   */
  void sort();

  /**
   * Sort the DBID set.
   *
   * @param comparator Comparator to use
   */
  void sort(Comparator<? super DBIDRef> comparator);

  /**
   * Sort the DBID set.
   *
   * @param start Starting index, for partial sorting
   * @param end End index, for partial sorting (exclusive)
   * @param comparator Comparator to use
   */
  void sort(int start, int end, Comparator<? super DBIDRef> comparator);

  /**
   * Remove the i'th entry (starting at 0)
   *
   * @param i Index
   */
  void remove(int i);

  /**
   * Replace the i'th entry (starting at 0)
   *
   * @param i Index
   * @param newval New value
   */
  void set(int i, DBIDRef newval);

  /**
   * Insert at position i (starting at 0, moving the remainder by one position).
   *
   * Note: this operation has linear time complexity on average: O(n/2)
   *
   * @param i Index
   * @param newval New value
   */
  void insert(int i, DBIDRef newval);

  /**
   * Swap DBIDs add positions a and b.
   *
   * @param a First position
   * @param b Second position
   */
  void swap(int a, int b);

  @Override
  DBIDArrayMIter iter();
}
