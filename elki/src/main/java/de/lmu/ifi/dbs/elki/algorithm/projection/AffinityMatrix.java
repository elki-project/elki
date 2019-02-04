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
package de.lmu.ifi.dbs.elki.algorithm.projection;

import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;

/**
 * Abstraction interface for an affinity matrix.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public interface AffinityMatrix {
  /**
   * Scale the whole matrix with a constant factor d.
   * 
   * This is used to add/remove early exaggeration of tSNE.
   * 
   * @param d Scaling factor.
   */
  void scale(double d);

  /**
   * Number of rows.
   * 
   * @return Size
   */
  int size();

  /**
   * Get an entry by absolute position.
   * 
   * Note: this may be expensive on sparse matrixes.
   *
   * @param i Row
   * @param j Column
   * @return Entry
   */
  double get(int i, int j);

  /**
   * Iterator over non-zero features only.
   *
   * Note: depending on the underlying implementation, this may or may not be
   * the dimension. Use {@link #iterDim} to get the actual dimension. In fact,
   * usually this will be the ith non-zero value, assuming an array
   * representation.
   *
   * Think of this number as an <em>iterator</em>. But for efficiency, it has a
   * primitive type, so it does not require garbage collection. With Java 10
   * value types, we will likely be able to make this <em>both</em> type-safe
   * and highly efficient again.
   * 
   * Intended usage:
   * 
   * <pre>
   * {@code
   * for (int iter = v.iter(x); v.iterValid(x, iter); iter = v.iterAdvance(x, iter)) {
   *   final int dim = v.iterDim(x, iter);
   *   final int val = v.iterValue(x, iter);
   * }
   * }
   * </pre>
   * 
   * Important: you need to use the return value of <tt>iterAdvance</tt> for the
   * next iteration, or you will have an endless loop.
   *
   * @param x Point to get the neighbors for
   * @return Identifier for the first non-zero dimension, <b>not necessarily the
   *         dimension!</b>
   */
  int iter(int x);

  /**
   * Get the dimension an iterator points to.
   * 
   * @param iter Iterator position
   * @return Dimension the iterator refers to
   */
  int iterDim(int x, int iter);

  /**
   * Get the value an iterator points to.
   * 
   * @param iter Iterator position
   * @return Dimension the iterator refers to
   */
  double iterValue(int x, int iter);

  /**
   * Advance the iterator to the next position.
   * 
   * @param iter Previous iterator position
   * @return Next iterator position
   */
  int iterAdvance(int x, int iter);

  /**
   * Test the iterator position for validity.
   * 
   * @param iter Iterator position
   * @return {@code true} when it refers to a valid position.
   */
  boolean iterValid(int x, int iter);

  /**
   * Array iterator over the stored objects.
   * 
   * @return DBID iterator
   */
  DBIDArrayIter iterDBIDs();
}
