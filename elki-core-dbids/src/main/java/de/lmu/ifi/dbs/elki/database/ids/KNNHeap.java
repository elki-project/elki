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
 * Interface for kNN heaps.
 * <p>
 * To instantiate, use:
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBIDUtil#newHeap}!
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - "serializes to" - KNNList
 * @composed - - - DoubleDBIDPair
 */
public interface KNNHeap {
  /**
   * Serialize to a {@link KNNList}. This empties the heap!
   *
   * @return KNNList with the heaps contents.
   */
  KNNList toKNNList();

  /**
   * Serialize to a {@link KNNList}, but applying sqrt to every distance.
   * This empties the heap!
   *
   * @return KNNList with the heaps contents.
   */
  KNNList toKNNListSqrt();

  /**
   * Get the K parameter ("maxsize" internally).
   *
   * @return K
   */
  int getK();

  /**
   * Get the distance to the k nearest neighbor, or maxdist otherwise.
   *
   * @return Maximum distance
   */
  double getKNNDistance();

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * <p>
   * Compared to the super.add() method, this often saves the pair construction.
   *
   * @param distance Distance value
   * @param id ID number
   * @return current k-distance
   */
  double insert(double distance, DBIDRef id);

  /**
   * Add a distance-id pair to the heap unless the distance is too large.
   * <p>
   * Use for existing pairs.
   *
   * @param e Existing distance pair
   */
  void insert(DoubleDBIDPair e);

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
  DoubleDBIDListIter unorderedIterator();

  /**
   * Check if an object is already in the heap.
   * 
   * @param other Other object
   * @return {@code true} if contained
   */
  boolean contains(DBIDRef other);
}
