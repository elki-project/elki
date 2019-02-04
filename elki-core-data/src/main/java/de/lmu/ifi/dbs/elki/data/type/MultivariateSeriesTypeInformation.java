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
package de.lmu.ifi.dbs.elki.data.type;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.utilities.io.ByteBufferSerializer;

/**
 * Type information for multi-variate time series.
 *
 * @author Sebastian Hollizeck
 * @since 0.7.0
 *
 * @param <V> Vector type
 */
public class MultivariateSeriesTypeInformation<V extends FeatureVector<?>> extends VectorTypeInformation<V> {
  /**
   * Number of variates per dimension.
   */
  protected final int multiplicity;

  /**
   * Constructor for a type request without dimensionality constraints.
   * 
   * @param cls Class constraint
   * @param <V> vector type
   */
  public static <V extends FeatureVector<?>> MultivariateSeriesTypeInformation<V> typeRequest(Class<? super V> cls) {
    return new MultivariateSeriesTypeInformation<>(cls, -1, Integer.MAX_VALUE, -1);
  }

  /**
   * Constructor for an actual type.
   *
   * @param cls base class
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param multiplicity Number of variates
   */
  public MultivariateSeriesTypeInformation(Class<? super V> cls, int mindim, int maxdim, int multiplicity) {
    super(cls, mindim, maxdim);
    this.multiplicity = multiplicity;
  }

  /**
   * Constructor for an actual type.
   *
   * @param factory Vector factory
   * @param serializer Serializer
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param multiplicity Number of variates
   */
  public MultivariateSeriesTypeInformation(FeatureVector.Factory<V, ?> factory, ByteBufferSerializer<? super V> serializer, int mindim, int maxdim, int multiplicity) {
    super(factory, serializer, mindim, maxdim);
    this.multiplicity = multiplicity;
  }

  /**
   * Get the multiplicity of the vector.
   * 
   * @return Multiplicity
   */
  @Override
  public int getMultiplicity() {
    return multiplicity;
  }

  @Override
  public String toString() {
    return super.toString() + ",multiplicity=" + multiplicity;
  }
}
