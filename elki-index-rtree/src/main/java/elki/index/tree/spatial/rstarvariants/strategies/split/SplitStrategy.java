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
package elki.index.tree.spatial.rstarvariants.strategies.split;

import elki.data.spatial.SpatialComparable;
import elki.utilities.datastructures.arraylike.ArrayAdapter;

/**
 * Generic interface for split strategies.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface SplitStrategy {
  /**
   * Split a page
   * 
   * @param entries Entries to split
   * @param getter Adapter for the entries array
   * @param minEntries Minimum number of entries in each part
   * @param <A> Array type
   * @param <E> Entry type
   * @return BitSet containing the assignment.
   */
  <E extends SpatialComparable, A> long[] split(A entries, ArrayAdapter<E, A> getter, int minEntries);
}