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
package de.lmu.ifi.dbs.elki.data;

/**
 * Extended interface for sparse feature vector types.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface SparseFeatureVector<D> extends FeatureVector<D> {
  /**
   * Iterator over non-zero features only, <em>ascending</em>.
   * 
   * Note: depending on the underlying implementation, this may or may not be
   * the dimension. Use {@link #iterDim} to get the actual dimension. In fact,
   * usually this will be the ith non-zero value, assuming an array
   * representation.
   * 
   * Think of this number as an iterator. For efficiency, it has a primitive
   * type!
   * 
   * Intended usage:
   * 
   * <pre>
   * {@code
   * for (int iter = v.iter(); v.iterValid(iter); iter = v.iterAdvance(iter)) {
   *   final int dim = v.iterDim(iter);
   *   // Do something.
   * }
   * }
   * </pre>
   * 
   * @return Identifier for the first non-zero dimension, <b>not necessarily the
   *         dimension!</b>
   */
  default int iter() {
    return 0;
  }

  /**
   * Get the dimension an iterator points to.
   * 
   * @param iter Iterator position
   * @return Dimension the iterator refers to
   */
  int iterDim(int iter);

  /**
   * Advance the iterator to the next position.
   * 
   * @param iter Previous iterator position
   * @return Next iterator position
   */
  default int iterAdvance(int iter) {
    return iter + 1;
  }

  /**
   * Retract the iterator to the next position.
   * 
   * @param iter Next iterator position
   * @return Previous iterator position
   */
  default int iterRetract(int iter) {
    return iter - 1;
  }

  /**
   * Test the iterator position for validity.
   * 
   * @param iter Iterator position
   * @return {@code true} when it refers to a valid position.
   */
  boolean iterValid(int iter);
}
