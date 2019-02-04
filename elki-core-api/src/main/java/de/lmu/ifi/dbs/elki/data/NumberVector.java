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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberVectorAdapter;

/**
 * Interface NumberVector defines the methods that should be implemented by any
 * Object that is element of a real vector space of type N.
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @opt nodefillcolor LemonChiffon
 * @opt hide de.lmu.ifi.dbs.elki.(datasource.filter|data.projection).*
 */
public interface NumberVector extends FeatureVector<Number>, SpatialComparable {
  /**
   * The String to separate attribute values in a String that represents the
   * values.
   */
  String ATTRIBUTE_SEPARATOR = " ";

  /**
   * Number vectors of <em>variable</em> length.
   */
  VectorTypeInformation<NumberVector> VARIABLE_LENGTH = VectorTypeInformation.typeRequest(NumberVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   */
  VectorFieldTypeInformation<NumberVector> FIELD = VectorFieldTypeInformation.typeRequest(NumberVector.class);

  /**
   * Type request for two-dimensional number vectors
   */
  VectorFieldTypeInformation<NumberVector> FIELD_1D = VectorFieldTypeInformation.typeRequest(NumberVector.class, 1, 1);

  /**
   * Type request for two-dimensional number vectors
   */
  VectorFieldTypeInformation<NumberVector> FIELD_2D = VectorFieldTypeInformation.typeRequest(NumberVector.class, 2, 2);

  @Deprecated
  @Override
  default Number getValue(int dimension) {
    return doubleValue(dimension);
  }

  @Override
  default double getMin(int dimension) {
    return doubleValue(dimension);
  }

  @Override
  default double getMax(int dimension) {
    return doubleValue(dimension);
  }

  /**
   * Returns the value in the specified dimension as double.
   * 
   * Note: this might seem redundant with respect to
   * {@code getValue(dim).doubleValue()}, but usually this is much more
   * efficient due to boxing/unboxing cost.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &lt;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  double doubleValue(int dimension);

  /**
   * Returns the value in the specified dimension as float.
   * 
   * Note: this might seem redundant with respect to
   * {@code getValue(dim).floatValue()}, but usually this is much more efficient
   * due to boxing/unboxing cost.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &lt;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  default float floatValue(int dimension) {
    return (float) doubleValue(dimension);
  }

  /**
   * Returns the value in the specified dimension as int.
   * 
   * Note: this might seem redundant with respect to
   * {@code getValue(dim).intValue()}, but usually this is much more efficient
   * due to boxing/unboxing cost.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &lt;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  default int intValue(int dimension) {
    return (int) longValue(dimension);
  }

  /**
   * Returns the value in the specified dimension as long.
   * 
   * Note: this might seem redundant with respect to
   * {@code getValue(dim).longValue()}, but usually this is much more efficient
   * due to boxing/unboxing cost.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &lt;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  long longValue(int dimension);

  /**
   * Returns the value in the specified dimension as short.
   * 
   * Note: this might seem redundant with respect to
   * {@code getValue(dim).shortValue()}, but usually this is much more efficient
   * due to boxing/unboxing cost.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &lt;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  default short shortValue(int dimension) {
    return (short) longValue(dimension);
  }

  /**
   * Returns the value in the specified dimension as byte.
   * 
   * Note: this might seem redundant with respect to
   * {@code getValue(dim).byteValue()}, but usually this is much more efficient
   * due to boxing/unboxing cost.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &lt;
   *        <code>this.getDimensionality()</code>
   * @return the value in the specified dimension
   */
  default byte byteValue(int dimension) {
    return (byte) longValue(dimension);
  }

  /**
   * Returns a double array <i>copy</i> of this vector.
   * 
   * @return Copy as {@code double[]}
   */
  double[] toArray();

  /**
   * Factory API for this feature vector.
   * 
   * @author Erich Schubert
   * 
   * @has - - - NumberVector
   * 
   * @param <V> Vector type
   */
  interface Factory<V extends NumberVector> extends FeatureVector.Factory<V, Number> {
    /**
     * Returns a new NumberVector of N for the given values.
     * 
     * @param values the values of the NumberVector
     * @return a new NumberVector of N for the given values
     */
    default V newNumberVector(double[] values) {
      return newNumberVector(values, DoubleArrayAdapter.STATIC);
    }

    /**
     * Returns a new NumberVector of N for the given values.
     * 
     * @param values Existing number vector
     * @return a new NumberVector of N for the given values
     */
    default V newNumberVector(NumberVector values) {
      return newNumberVector(values, NumberVectorAdapter.STATIC);
    }

    /**
     * Instantiate from any number-array like object.
     * 
     * @param <A> Array type
     * @param array Array
     * @param adapter Adapter
     * @return a new NumberVector of N for the given values.
     */
    <A> V newNumberVector(A array, NumberArrayAdapter<?, ? super A> adapter);
  }
}
