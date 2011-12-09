package de.lmu.ifi.dbs.elki.data.type;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * Type information to specify that a type has a fixed dimensionality.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class VectorFieldTypeInformation<V extends FeatureVector<?, ?>> extends VectorTypeInformation<V> {
  /**
   * Object factory for producing new instances
   */
  private final V factory;

  /**
   * Labels
   */
  private String[] labels = null;

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   * 
   * @param cls Restriction java class
   * @param serializer Serializer
   * @param dim Dimensionality
   * @param labels Labels
   * @param factory Factory class
   */
  public VectorFieldTypeInformation(Class<? super V> cls, ByteBufferSerializer<? super V> serializer, int dim, String[] labels, V factory) {
    super(cls, serializer, dim, dim);
    this.labels = labels;
    this.factory = factory;
    assert (labels == null || labels.length == dim) : "Created vector field with incomplete labels.";
  }

  /**
   * Constructor for a request with minimum and maximum dimensionality.
   * 
   * @param cls Vector restriction class.
   * @param serializer Serializer
   * @param mindim Minimum dimensionality request
   * @param maxdim Maximum dimensionality request
   */
  public VectorFieldTypeInformation(Class<? super V> cls, ByteBufferSerializer<? super V> serializer, int mindim, int maxdim) {
    super(cls, serializer, mindim, maxdim);
    this.factory = null;
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   * 
   * @param cls Restriction java class
   * @param serializer Serializer
   * @param dim Dimensionality
   * @param factory Factory class
   */
  public VectorFieldTypeInformation(Class<? super V> cls, ByteBufferSerializer<? super V> serializer, int dim, V factory) {
    super(cls, serializer, dim, dim);
    this.factory = factory;
  }

  /**
   * Constructor for a request with fixed dimensionality.
   * 
   * @param cls Vector restriction class.
   * @param serializer Serializer
   * @param dim Dimensionality request
   */
  public VectorFieldTypeInformation(Class<? super V> cls, ByteBufferSerializer<? super V> serializer, int dim) {
    super(cls, serializer, dim, dim);
    this.factory = null;
  }

  /**
   * Constructor for a request without fixed dimensionality.
   * 
   * @param cls Vector restriction class.
   * @param serializer Serializer
   */
  public VectorFieldTypeInformation(Class<? super V> cls, ByteBufferSerializer<? super V> serializer) {
    super(cls, serializer);
    this.factory = null;
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   * 
   * @param cls Restriction java class
   * @param dim Dimensionality
   * @param labels Labels
   * @param factory Factory class
   */
  public VectorFieldTypeInformation(Class<? super V> cls, int dim, String[] labels, V factory) {
    super(cls, dim, dim);
    this.labels = labels;
    this.factory = factory;
    assert (labels == null || labels.length == dim) : "Created vector field with incomplete labels.";
  }

  /**
   * Constructor for a request with minimum and maximum dimensionality.
   * 
   * @param cls Vector restriction class.
   * @param mindim Minimum dimensionality request
   * @param maxdim Maximum dimensionality request
   */
  public VectorFieldTypeInformation(Class<? super V> cls, int mindim, int maxdim) {
    super(cls, mindim, maxdim);
    this.factory = null;
  }

  /**
   * Constructor with given dimensionality and factory, so usually an instance.
   * 
   * @param cls Restriction java class
   * @param dim Dimensionality
   * @param factory Factory class
   */
  public VectorFieldTypeInformation(Class<? super V> cls, int dim, V factory) {
    super(cls, dim, dim);
    this.factory = factory;
  }

  /**
   * Constructor for a request with fixed dimensionality.
   * 
   * @param cls Vector restriction class.
   * @param dim Dimensionality request
   */
  public VectorFieldTypeInformation(Class<? super V> cls, int dim) {
    super(cls, dim, dim);
    this.factory = null;
  }

  /**
   * Constructor for a request without fixed dimensionality.
   * 
   * @param cls Vector restriction class.
   */
  public VectorFieldTypeInformation(Class<? super V> cls) {
    super(cls);
    this.factory = null;
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    // Do all checks from superclass
    if(!super.isAssignableFromType(type)) {
      return false;
    }
    // Additionally check that mindim == maxdim.
    VectorTypeInformation<?> other = (VectorTypeInformation<?>) type;
    if(other.mindim != other.maxdim) {
      return false;
    }
    return true;
  }

  /**
   * Get the dimensionality of the type.
   * 
   * @return dimensionality
   */
  public int dimensionality() {
    if(mindim != maxdim) {
      throw new UnsupportedOperationException("Requesting dimensionality for a type request without defined dimensionality!");
    }
    return mindim;
  }

  /**
   * Get the object type factory.
   * 
   * @return the factory
   */
  public V getFactory() {
    if(factory == null) {
      throw new UnsupportedOperationException("Requesting factory for a type request!");
    }
    return factory;
  }

  /**
   * Pseudo constructor that is often convenient to use when T is not completely
   * known.
   * 
   * @param <T> Type
   * @param cls Class restriction
   * @return Type
   */
  public static <T extends FeatureVector<?, ?>> VectorFieldTypeInformation<T> get(Class<T> cls) {
    return new VectorFieldTypeInformation<T>(cls);
  }

  /**
   * Pseudo constructor that is often convenient to use when T is not completely
   * known, but the dimensionality is fixed.
   * 
   * @param <T> Type
   * @param cls Class restriction
   * @param dim Dimensionality (exact)
   * @return Type
   */
  public static <T extends FeatureVector<?, ?>> VectorFieldTypeInformation<T> get(Class<T> cls, int dim) {
    return new VectorFieldTypeInformation<T>(cls, dim);
  }

  @Override
  public String toString() {
    if(mindim == maxdim) {
      return getRestrictionClass().getSimpleName() + ",dim=" + mindim;
    }
    else {
      return super.toString();
    }
  }

  /**
   * Get the column label
   * 
   * @param col Column number
   * @return Label
   */
  public String getLabel(int col) {
    if(labels == null) {
      return null;
    }
    return labels[col - 1];
  }
}