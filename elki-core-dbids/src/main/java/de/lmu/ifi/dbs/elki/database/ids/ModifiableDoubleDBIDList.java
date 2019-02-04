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
 * Modifiable API for Distance-DBID results
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @composed - - - DoubleDBIDPair
 */
public interface ModifiableDoubleDBIDList extends DoubleDBIDList {
  /**
   * Add an object to this result.
   *
   * @param distance Distance to add
   * @param id DBID to add
   */
  void add(double distance, DBIDRef id);

  /**
   * Add an element.
   *
   * @param pair Pair to add
   */
  void add(DoubleDBIDPair pair);

  /**
   * Clear the list contents.
   */
  void clear();

  /**
   * Sort the result in ascending order
   */
  void sort();

  /**
   * Swap to entries in the list.
   *
   * @param i First entry
   * @param j Second entry
   */
  void swap(int i, int j);

  /**
   * Remove the entry at position p by shifting the remainder forward.
   *
   * @param p Entry offset to remove
   */
  void remove(int p);

  /**
   * Remove the entry at position p by swapping with the last (not preserving
   * the order).
   *
   * @param p Entry offset to remove
   */
  void removeSwap(int p);

  @Override
  DoubleDBIDListMIter iter();
}