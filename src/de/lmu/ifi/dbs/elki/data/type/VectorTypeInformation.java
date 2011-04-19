package de.lmu.ifi.dbs.elki.data.type;

import de.lmu.ifi.dbs.elki.data.FeatureVector;

/**
 * Construct a type information for vector spaces with fixed dimensionality.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class VectorTypeInformation<V extends FeatureVector<?, ?>> extends SimpleTypeInformation<V> {
  /**
   * Minimum dimensionality
   */
  protected final int mindim;

  /**
   * Maximum dimensionality
   */
  protected final int maxdim;

  /**
   * Constructor.
   * 
   * @param cls base class
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   */
  public VectorTypeInformation(Class<? super V> cls, int mindim, int maxdim) {
    super(cls);
    assert (this.mindim <= this.maxdim);
    this.mindim = mindim;
    this.maxdim = maxdim;
  }

  /**
   * Constructor without size constraints.
   * 
   * @param cls
   */
  public VectorTypeInformation(Class<? super V> cls) {
    this(cls, -1, Integer.MAX_VALUE);
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    // This validates the base type V
    if(!super.isAssignableFromType(type)) {
      return false;
    }
    // Other type must also be a vector type
    if(!(type instanceof VectorTypeInformation)) {
      return false;
    }
    VectorTypeInformation<?> othertype = (VectorTypeInformation<?>) type;
    assert (othertype.mindim <= othertype.maxdim);
    // the other must not have a lower minimum dimensionality
    if(this.mindim > othertype.mindim) {
      return false;
    }
    // ... or a higher maximum dimensionality.
    if(othertype.maxdim > this.maxdim) {
      return false;
    }
    return true;
  }

  @Override
  public boolean isAssignableFrom(Object other) {
    // Validate that we can assign
    if(!super.isAssignableFrom(other)) {
      return false;
    }
    // Get the object dimensionality
    final int odim = cast(other).getDimensionality();
    if(odim < mindim) {
      return false;
    }
    if(odim > maxdim) {
      return false;
    }
    return true;
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
   * Pseudo constructor that is often convenient to use when T is not completely known.
   * 
   * @param <T> Type
   * @param cls Class restriction
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @return Type information
   */
  public static <T extends FeatureVector<?, ?>> VectorTypeInformation<T> get(Class<T> cls, int mindim, int maxdim) {
    return new VectorTypeInformation<T>(cls, mindim, maxdim);
  }

  /**
   * Pseudo constructor that is often convenient to use when T is not completely known.
   * 
   * @param <T> Type
   * @param cls Class restriction
   * @return Type information
   */
  public static <T extends FeatureVector<?, ?>> VectorTypeInformation<T> get(Class<T> cls) {
    return new VectorTypeInformation<T>(cls);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer(super.toString());
    if (mindim >= 0) {
      buf.append(",mindim="+mindim);
    }
    if (maxdim < Integer.MAX_VALUE) {
      buf.append(",maxdim="+maxdim);
    }
    return buf.toString();
  }
}