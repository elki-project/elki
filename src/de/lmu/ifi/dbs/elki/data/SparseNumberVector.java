package de.lmu.ifi.dbs.elki.data;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import gnu.trove.map.TIntDoubleMap;

/**
 * Combines the SparseFeatureVector and NumberVector.
 * 
 * @author Erich Schubert
 * 
 * @param <N> Number type
 */
public interface SparseNumberVector<N extends Number> extends NumberVector<N>, SparseFeatureVector<N> {
  /**
   * Update the vector space dimensionality.
   * 
   * @param maxdim New dimensionality
   */
  void setDimensionality(int maxdim);

  /**
   * Get the value of the iterators' current dimension.
   * 
   * @param iter Iterator
   * @return Value at the current position
   */
  double iterDoubleValue(int iter);

  /**
   * Get the value of the iterators' current dimension.
   * 
   * @param iter Iterator
   * @return Value at the current position
   */
  float iterFloatValue(int iter);

  /**
   * Get the value of the iterators' current dimension.
   * 
   * @param iter Iterator
   * @return Value at the current position
   */
  int iterIntegerValue(int iter);

  /**
   * Get the value of the iterators' current dimension.
   * 
   * @param iter Iterator
   * @return Value at the current position
   */
  short iterShortValue(int iter);

  /**
   * Get the value of the iterators' current dimension.
   * 
   * @param iter Iterator
   * @return Value at the current position
   */
  long iterLongValue(int iter);

  /**
   * Get the value of the iterators' current dimension.
   * 
   * @param iter Iterator
   * @return Value at the current position
   */
  byte iterByteValue(int iter);

  /**
   * Factory for sparse number vectors: make from a dim-value map.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has SparseNumberVector
   * 
   * @param <V> Vector type number type
   * @param <N> Number type
   */
  interface Factory<V extends SparseNumberVector<N>, N extends Number> extends NumberVector.Factory<V, N> {
    /**
     * Returns a new NumberVector of N for the given values.
     * 
     * @param values the values of the NumberVector
     * @param maxdim Maximum dimensionality.
     * @return a new NumberVector of N for the given values
     */
    V newNumberVector(TIntDoubleMap values, int maxdim);
  }
}
