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
 * Construct a type information for vector spaces with variable dimensionality.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - FeatureVector
 * 
 * @param <V> Vector type
 */
public class VectorTypeInformation<V extends FeatureVector<?>> extends SimpleTypeInformation<V> {
  /**
   * Object factory for producing new instances.
   */
  private final FeatureVector.Factory<V, ?> factory;

  /**
   * Constructor for a type request without dimensionality constraints.
   * 
   * @param cls Class constraint
   * @param <V> vector type
   */
  public static <V extends FeatureVector<?>> VectorTypeInformation<V> typeRequest(Class<? super V> cls) {
    return new VectorTypeInformation<>(cls, -1, Integer.MAX_VALUE);
  }

  /**
   * Constructor for a type request with dimensionality constraints.
   * 
   * @param cls Class constraint
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param <V> vector type
   */
  public static <V extends FeatureVector<?>> VectorTypeInformation<V> typeRequest(Class<? super V> cls, int mindim, int maxdim) {
    return new VectorTypeInformation<>(cls, mindim, maxdim);
  }

  /**
   * Minimum dimensionality.
   */
  protected final int mindim;

  /**
   * Maximum dimensionality.
   */
  protected final int maxdim;

  /**
   * Constructor for a type request.
   * 
   * @param cls base class
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   */
  public VectorTypeInformation(Class<? super V> cls, int mindim, int maxdim) {
    super(cls);
    this.factory = null;
    assert (mindim <= maxdim);
    this.mindim = mindim;
    this.maxdim = maxdim;
  }

  /**
   * Constructor for an actual type.
   * 
   * @param factory Vector factory
   * @param serializer Serializer
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   */
  public VectorTypeInformation(FeatureVector.Factory<V, ?> factory, ByteBufferSerializer<? super V> serializer, int mindim, int maxdim) {
    super(factory.getRestrictionClass(), serializer);
    this.factory = factory;
    assert (mindim <= maxdim);
    this.mindim = mindim;
    this.maxdim = maxdim;
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    // This validates the base type V
    // Other type must also be a vector type
    if(!super.isAssignableFromType(type) || !(type instanceof VectorTypeInformation)) {
      return false;
    }
    VectorTypeInformation<?> othertype = (VectorTypeInformation<?>) type;
    assert (othertype.mindim <= othertype.maxdim);
    // the other must not have a lower minimum dimensionality
    if(this.mindim > othertype.mindim) {
      return false;
    }
    // ... or a higher maximum dimensionality.
    return othertype.maxdim <= this.maxdim;
  }

  @Override
  public boolean isAssignableFrom(Object other) {
    // Validate that we can assign
    if(!super.isAssignableFrom(other)) {
      return false;
    }
    // Get the object dimensionality
    final int odim = cast(other).getDimensionality();
    return odim >= mindim && odim <= maxdim;
  }

  /**
   * Get the object type factory.
   * 
   * @return the factory
   */
  public FeatureVector.Factory<V, ?> getFactory() {
    if(factory == null) {
      throw new UnsupportedOperationException("Requesting factory for a type request!");
    }
    return factory;
  }

  /**
   * Get the minimum dimensionality of the occurring vectors.
   * 
   * @return dimensionality
   */
  public int mindim() {
    if(mindim < 0) {
      throw new UnsupportedOperationException("Requesting dimensionality for a request without defined dimensionality!");
    }
    return mindim;
  }

  /**
   * Get the maximum dimensionality of the occurring vectors.
   * 
   * @return dimensionality
   */
  public int maxdim() {
    if(maxdim == Integer.MAX_VALUE) {
      throw new UnsupportedOperationException("Requesting dimensionality for a request without defined dimensionality!");
    }
    return maxdim;
  }

  /**
   * Get the multiplicity of the vector.
   * 
   * @return Multiplicity {@code 1} (except for subclasses)
   */
  public int getMultiplicity() {
    return 1;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(1000).append(super.toString()).append(",variable");
    if(mindim >= 0) {
      buf.append(",mindim=").append(mindim);
    }
    if(maxdim < Integer.MAX_VALUE) {
      buf.append(",maxdim=").append(maxdim);
    }
    return buf.toString();
  }
}
