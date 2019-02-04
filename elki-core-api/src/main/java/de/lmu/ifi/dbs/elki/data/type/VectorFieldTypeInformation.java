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
 * Type information to specify that a type has a fixed dimensionality.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <V> Vector type
 */
public class VectorFieldTypeInformation<V extends FeatureVector<?>> extends VectorTypeInformation<V> implements FieldTypeInformation {
  /**
   * Constructor for a type request without dimensionality constraints.
   *
   * @param cls Class constraint
   * @param <V> vector type
   */
  public static <V extends FeatureVector<?>> VectorFieldTypeInformation<V> typeRequest(Class<? super V> cls) {
    return new VectorFieldTypeInformation<>(cls, -1, Integer.MAX_VALUE);
  }

  /**
   * Constructor for a type request with dimensionality constraints.
   *
   * @param cls Class constraint
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param <V> vector type
   */
  public static <V extends FeatureVector<?>> VectorFieldTypeInformation<V> typeRequest(Class<? super V> cls, int mindim, int maxdim) {
    return new VectorFieldTypeInformation<>(cls, mindim, maxdim);
  }

  /**
   * Labels.
   */
  private String[] labels = null;

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   *
   * @param factory Factory class
   * @param dim Dimensionality
   * @param labels Labels
   * @param serializer Serializer
   */
  public VectorFieldTypeInformation(FeatureVector.Factory<V, ?> factory, int dim, String[] labels, ByteBufferSerializer<? super V> serializer) {
    super(factory, serializer, dim, dim);
    this.labels = labels;
    assert(labels == null || labels.length == dim) : "Created vector field with incomplete labels.";
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   *
   * @param factory Factory class
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param serializer Serializer
   */
  public VectorFieldTypeInformation(FeatureVector.Factory<V, ?> factory, int mindim, int maxdim, ByteBufferSerializer<? super V> serializer) {
    super(factory, serializer, mindim, maxdim);
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   *
   * @param factory Factory class
   * @param dim Dimensionality
   * @param serializer Serializer
   */
  public VectorFieldTypeInformation(FeatureVector.Factory<V, ?> factory, int dim, ByteBufferSerializer<? super V> serializer) {
    super(factory, serializer, dim, dim);
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   *
   * @param factory Factory class
   * @param dim Dimensionality
   * @param labels Labels
   */
  public VectorFieldTypeInformation(FeatureVector.Factory<V, ?> factory, int dim, String[] labels) {
    super(factory, factory.getDefaultSerializer(), dim, dim);
    this.labels = labels;
    assert(labels == null || labels.length == dim) : "Created vector field with incomplete labels.";
  }

  /**
   * Constructor for a request with minimum and maximum dimensionality.
   *
   * @param cls Vector restriction class.
   * @param mindim Minimum dimensionality request
   * @param maxdim Maximum dimensionality request
   */
  private VectorFieldTypeInformation(Class<? super V> cls, int mindim, int maxdim) {
    super(cls, mindim, maxdim);
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   *
   * @param factory Factory class
   * @param dim Dimensionality
   */
  public VectorFieldTypeInformation(FeatureVector.Factory<V, ?> factory, int dim) {
    super(factory, factory.getDefaultSerializer(), dim, dim);
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    if(this == type) {
      return true;
    }
    // Do all checks from superclass
    if(!super.isAssignableFromType(type)) {
      return false;
    }
    // Additionally check that mindim == maxdim.
    VectorTypeInformation<?> other = (VectorTypeInformation<?>) type;
    return other.mindim == other.maxdim;
  }

  @Override
  public int getDimensionality() {
    if(mindim != maxdim) {
      throw new UnsupportedOperationException("Requesting dimensionality for a type request without defined dimensionality!");
    }
    return mindim;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(1000).append(getRestrictionClass().getSimpleName());
    if(mindim == maxdim) {
      buf.append(",dim=").append(mindim);
    }
    else {
      buf.append(",field");
      if(mindim >= 0) {
        buf.append(",mindim=").append(mindim);
      }
      if(maxdim < Integer.MAX_VALUE) {
        buf.append(",maxdim=").append(maxdim);
      }
    }
    return buf.toString();
  }

  /**
   * Get the column label.
   *
   * @param col Column number
   * @return Label
   */
  public String getLabel(int col) {
    return labels == null ? null : labels[col];
  }

  /**
   * Get the column labels.
   *
   * @return labels
   */
  protected String[] getLabels() {
    return labels;
  }
}
