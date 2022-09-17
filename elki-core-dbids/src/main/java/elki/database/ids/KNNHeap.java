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
 * Interface for kNN heaps.
 * <p>
 * To instantiate, use:
 * {@link elki.database.ids.DBIDUtil#newHeap}!
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - "serializes to" - KNNList
 */
public interface KNNHeap extends DoubleDBIDHeap {
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
   * Add a distance-id pair to the heap
   *
   * @param distance Distance value
   * @param id ID number
   * @return Distance to the element at the top of the heap
   */
  @Override
  double insert(double distance, DBIDRef id);

  /**
   * <b>Disallowed</b>, because {@code max} is fixed.
   *
   * @throws UnsupportedOperationException
   */
  @Deprecated
  @Override
  default double insert(double distance, DBIDRef id, int max) {
    throw new UnsupportedOperationException("You cannot override the k of kNN heaps.");
  }

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
}
