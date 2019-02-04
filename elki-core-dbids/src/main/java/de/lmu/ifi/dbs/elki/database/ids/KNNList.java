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
 * Interface for kNN results.
 * <p>
 * To iterate over the results, use the following code:
 * 
 * <pre>
 * {@code
 * for (DistanceDBIDResultIter<D> iter = result.iter(); iter.valid(); iter.advance()) {
 *   // You can get the distance via: iter.getDistance();
 *   // Or use iter just like any other DBIDRef
 * }
 * }
 * </pre>
 *
 * If you are only interested in the IDs of the objects, the following is also
 * sufficient:
 *
 * <pre>
 * {@code
 * for (DBIDIter<D> iter = result.iter(); iter.valid(); iter.advance()) {
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
 */
public interface KNNList extends DoubleDBIDList {
  /**
   * Get the K parameter (note: this may be less than the size of the list!)
   *
   * @return K
   */
  int getK();

  /**
   * Get the distance to the k nearest neighbor, or infinity otherwise.
   *
   * @return Maximum distance
   */
  double getKNNDistance();

  /**
   * Select a subset for a smaller k.
   *
   * @param k New k
   * @return KNN result for the smaller k.
   */
  KNNList subList(int k);
}
