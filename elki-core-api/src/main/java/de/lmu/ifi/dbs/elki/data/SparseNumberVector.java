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

import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Combines the SparseFeatureVector and NumberVector.
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface SparseNumberVector extends NumberVector, SparseFeatureVector<Number> {
  /**
   * Input data type: Sparse vectors with variable length.
   */
  VectorTypeInformation<SparseNumberVector> VARIABLE_LENGTH = VectorTypeInformation.typeRequest(SparseNumberVector.class);

  /**
   * Input data type: Sparse vector field.
   */
  VectorFieldTypeInformation<SparseNumberVector> FIELD = VectorFieldTypeInformation.typeRequest(SparseNumberVector.class);

  /**
   * Iterator over non-zero features only, <em>ascendingly</em>.
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
   *   final double val = v.iterDoubleValue(iter);
   *   // Do something.
   * }
   * }
   * </pre>
   *
   * @return Identifier for the first non-zero dimension, <b>not necessarily the
   *         dimension!</b>
   */
  @Override
  default int iter() {
    return 0;
  }
  
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
  default float iterFloatValue(int iter) {
    return (float) iterDoubleValue(iter);
  }

  /**
   * Get the value of the iterators' current dimension.
   *
   * @param iter Iterator
   * @return Value at the current position
   */
  default int iterIntValue(int iter) {
    return (int) iterLongValue(iter);
  }

  /**
   * Get the value of the iterators' current dimension.
   *
   * @param iter Iterator
   * @return Value at the current position
   */
  default short iterShortValue(int iter) {
    return (short) iterLongValue(iter);
  }

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
  default byte iterByteValue(int iter) {
    return (byte) iterLongValue(iter);
  }

  /**
   * @deprecated As the vectors are sparse, try to iterate over the sparse
   *             dimensions only, see {@link #iterDoubleValue}.
   */
  @Override
  @Deprecated
  double doubleValue(int dimension);

  /**
   * @deprecated As the vectors are sparse, try to iterate over the sparse
   *             dimensions only, see {@link #iterFloatValue}.
   */
  @Override
  @Deprecated
  default float floatValue(int dimension) {
    return (float) doubleValue(dimension);
  }

  /**
   * @deprecated As the vectors are sparse, try to iterate over the sparse
   *             dimensions only, see {@link #iterIntValue}.
   */
  @Override
  @Deprecated
  default int intValue(int dimension) {
    return (int) longValue(dimension);
  }

  /**
   * @deprecated As the vectors are sparse, try to iterate over the sparse
   *             dimensions only, see {@link #iterLongValue}.
   */
  @Override
  @Deprecated
  long longValue(int dimension);

  /**
   * @deprecated As the vectors are sparse, try to iterate over the sparse
   *             dimensions only, see {@link #iterShortValue}.
   */
  @Override
  @Deprecated
  default short shortValue(int dimension) {
    return (short) longValue(dimension);
  }

  /**
   * @deprecated As the vectors are sparse, try to iterate over the sparse
   *             dimensions only, see {@link #iterByteValue}.
   */
  @Override
  @Deprecated
  default byte byteValue(int dimension) {
    return (byte) longValue(dimension);
  }

  /**
   * Factory for sparse number vectors: make from a dim-value map.
   *
   * @author Erich Schubert
   *
   * @has - - - SparseNumberVector
   *
   * @param <V> Vector type number type
   */
  interface Factory<V extends SparseNumberVector> extends NumberVector.Factory<V> {
    /**
     * Returns a new NumberVector of N for the given values.
     *
     * @param values the values of the NumberVector
     * @param maxdim Maximum dimensionality.
     * @return a new NumberVector of N for the given values
     */
    V newNumberVector(Int2DoubleOpenHashMap values, int maxdim);
  }
}
