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

import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Generic FeatureVector class that can contain any type of data (i.e. numerical
 * or categorical attributes). See {@link NumberVector} for vectors that
 * actually store numerical features.
 * 
 * @author Erich Schubert
 * @since 0.1
 */
public interface FeatureVector<D> {
  /**
   * Input type: Any feature vector type.
   */
  VectorTypeInformation<FeatureVector<?>> TYPE = VectorTypeInformation.typeRequest(FeatureVector.class);

  /**
   * The dimensionality of the vector space where of this FeatureVector of V is
   * an element.
   * 
   * @return the number of dimensions of this FeatureVector of V
   */
  int getDimensionality();

  /**
   * Returns the value in the specified dimension.
   * 
   * @param dimension the desired dimension, where 0 &le; dimension &le;
   *        <code>this.getDimensionality()-1</code>
   * @return the value in the specified dimension
   */
  D getValue(int dimension);

  /**
   * Returns a String representation of the FeatureVector of V as a line that is
   * suitable to be printed in a sequential file.
   * 
   * @return a String representation of the FeatureVector of V
   */
  @Override
  String toString();

  /**
   * Factory API for this feature vector.
   * 
   * @author Erich Schubert
   *
   * @param <V> Vector type
   */
  interface Factory<V extends FeatureVector<? extends D>, D> {
    /**
     * Returns a new FeatureVector of V for the given values.
     * 
     * @param array the values of the featureVector
     * @param adapter adapter class
     * @param <A> Array type
     * @return a new FeatureVector of V for the given values
     */
    <A> V newFeatureVector(A array, ArrayAdapter<? extends D, A> adapter);

    /**
     * Get the default serializer for this type.
     * 
     * Note, this may be {@code null} when no serializer is available.
     * 
     * @return Serializer
     */
    ByteBufferSerializer<V> getDefaultSerializer();
    
    /**
     * Get the objects type restriction.
     * 
     * @return Restriction class
     */
    Class<? super V> getRestrictionClass();
  }
}
