package de.lmu.ifi.dbs.elki.data.type;

import de.lmu.ifi.dbs.elki.data.FeatureVector;

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
   * Constructor for a request without fixed dimensionality.
   * 
   * @param cls Vector restriction class.
   */
  public VectorFieldTypeInformation(Class<? super V> cls) {
    super(cls);
    this.factory = null;
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
}