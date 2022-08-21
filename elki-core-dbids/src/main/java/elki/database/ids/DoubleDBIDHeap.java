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
package elki.database.ids;

/**
 * Max heap for DBIDs.
 * <p>
 * To instantiate, use:
 * {@link elki.database.ids.DBIDUtil#newMaxHeap}!
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @opt nodefillcolor LemonChiffon
 */
public interface DoubleDBIDHeap extends DBIDRef {
  /**
   * Get the topmost value (the heap is a {@link DBIDRef}, referencing the top
   * element).
   *
   * @return value at top
   */
  double peekKey();

  /**
   * Remove the topmost element.
   */
  void poll();

  /**
   * Add a double-id pair to the heap
   *
   * @param key Key value
   * @param id ID number
   * @return Value of to the element at the top of the heap
   */
  double insert(double key, DBIDRef id);

  /**
   * Add a double-id pair to the heap unless the heap grows beyond the given
   * maximum size. Beware of the following subtle details:
   * <ul>
   * <li>If the heap has fewer than "max" elements, the value is added.
   * <li>else, if the new value is "worse" than the top, it will replace the top
   * (and the heap will be repaired)
   * <li>for a min heap, this adds only <em>larger</em> value, for a max heap
   * only <em>smaller</em> values. This may seem odd, but it is equivalent to
   * adding the new element and then removing the top ("best") element.
   * <li>But: the heap will not be reduced to the given size
   * </ul>
   *
   * @param distance Distance value
   * @param id ID number
   * @param max Maximum number of values
   * @return Distance to the element at the top of the heap
   */
  double insert(double distance, DBIDRef id, int max);

  /**
   * Current size of heap.
   *
   * @return Heap size
   */
  int size();

  /**
   * Test if the heap is empty.
   *
   * @return true when empty.
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Clear the heap.
   */
  void clear();

  /**
   * Unordered iterator over the heap.
   *
   * @return Iterator
   */
  DoubleDBIDIter unorderedIterator();

  /**
   * Check if an object is already in the heap (slow scan).
   * 
   * @param other Other object
   * @return {@code true} if contained
   */
  boolean contains(DBIDRef other);
}
