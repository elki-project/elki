package de.lmu.ifi.dbs.elki.data;

import java.util.BitSet;
import java.util.Comparator;

/**
 * Represents a subspace of the original data space.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector this subspace contains
 */
public class Subspace<V extends FeatureVector<V, ?>> {
  /**
   * The dimensions building this subspace.
   */
  private final BitSet dimensions = new BitSet();

  /**
   * The dimensionality of this subspace.
   */
  private final int dimensionality;

  /**
   * Creates a new one-dimensional subspace of the original data space.
   * 
   * @param dimension the dimension building this subspace
   */
  public Subspace(int dimension) {
    dimensions.set(dimension);
    dimensionality = 1;
  }

  /**
   * Creates a new k-dimensional subspace of the original data space.
   * 
   * @param dimensions the dimensions building this subspace
   */
  public Subspace(BitSet dimensions) {
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      this.dimensions.set(d);
    }
    dimensionality = dimensions.cardinality();
  }

  /**
   * Returns the BitSet representing the dimensions of this subspace.
   * 
   * @return the dimensions of this subspace
   */
  public final BitSet getDimensions() {
    return dimensions;
  }

  /**
   * Returns the dimensionality of this subspace.
   * 
   * @return the number of dimensions this subspace contains
   */
  public final int dimensionality() {
    return dimensionality;
  }

  /**
   * Joins this subspace with the specified subspace. The join is only
   * successful if both subspaces have the first k-1 dimensions in common (where
   * k is the number of dimensions) and the last dimension of this subspace is
   * less than the last dimension of the specified subspace.
   * 
   * @param other the subspace to join
   * @return the join of this subspace with the specified subspace if the join
   *         condition is fulfilled, null otherwise.
   * @see Subspace#joinLastDimensions(Subspace)
   */
  public Subspace<V> join(Subspace<V> other) {
    BitSet newDimensions = joinLastDimensions(other);
    if(newDimensions == null) {
      return null;
    }

    return new Subspace<V>(newDimensions);
  }

  /**
   * Returns a string representation of this subspace by calling {@link
   * #toString("")}.
   * 
   * @return a string representation of this subspace
   */
  @Override
  public String toString() {
    return toString("");
  }

  /**
   * Returns a string representation of this subspace that contains the given
   * string prefix and the dimensions of this subspace.
   * 
   * @param pre a string prefix for each row of this string representation
   * @return a string representation of this subspace
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(pre).append("Dimensions: [");
    int start = dimensions.nextSetBit(0);
    for(int d = start; d >= 0; d = dimensions.nextSetBit(d + 1)) {
      if(d != start) {
        result.append(", ");
      }
      result.append(d + 1);
    }
    result.append("]");
    return result.toString();
  }

  /**
   * Returns true if this subspace is a subspace of the specified subspace, i.e.
   * if the set of dimensions building this subspace are contained in the set of
   * dimensions building the specified subspace.
   * 
   * @param subspace the subspace to test
   * @return true if this subspace is a subspace of the specified subspace,
   *         false otherwise
   */
  public boolean isSubspace(Subspace<V> subspace) {
    if(this.dimensionality > subspace.dimensionality) {
      return false;
    }
    for(int d = dimensions.nextSetBit(0); d >= 0; d = dimensions.nextSetBit(d + 1)) {
      if(!subspace.dimensions.get(d)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Joins the dimensions of this subspace with the dimensions of the specified
   * subspace. The join is only successful if both subspaces have the first k-1
   * dimensions in common (where k is the number of dimensions) and the last
   * dimension of this subspace is less than the last dimension of the specified
   * subspace.
   * 
   * @param other the subspace to join
   * @return the joined dimensions of this subspace with the dimensions of the
   *         specified subspace if the join condition is fulfilled, null
   *         otherwise.
   */
  protected BitSet joinLastDimensions(Subspace<V> other) {
    if(this.dimensionality != other.dimensionality) {
      return null;
    }

    BitSet resultDimensions = new BitSet();
    int last1 = -1, last2 = -1;

    for(int d1 = this.dimensions.nextSetBit(0), d2 = other.dimensions.nextSetBit(0); d1 >= 0 && d2 >= 0; d1 = this.dimensions.nextSetBit(d1 + 1), d2 = other.dimensions.nextSetBit(d2 + 1)) {

      if(d1 == d2) {
        resultDimensions.set(d1);
      }
      last1 = d1;
      last2 = d2;
    }

    if(last1 < last2) {
      resultDimensions.set(last1);
      resultDimensions.set(last2);
      return resultDimensions;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the hash code value of the {@link dimensions} of this subspace.
   * 
   * @return a hash code value for this subspace
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return dimensions.hashCode();
  }

  /**
   * Indicates if the specified object is equal to this subspace, i.e. if the
   * specified object is a Subspace and is built of the same dimensions than
   * this subspace.
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    Subspace<V> other = (Subspace<V>) obj;
    return new DimensionComparator().compare(this, other) == 0;
  }

  /**
   * A comparator for subspaces based on their involved dimensions. The
   * subspaces are ordered according to the ordering of their dimensions.
   * 
   * @author Elke Achtert
   */
  public static class DimensionComparator implements Comparator<Subspace<?>> {
    /**
     * Compares the two specified subspaces for order. If the two subspaces have
     * different dimensionalities a negative integer or a positive integer will
     * be returned if the dimensionality of the first subspace is less than or
     * greater than the dimensionality of the second subspace. Otherwise the
     * comparison works as follows: Let {@code d1} and {@code d2} be the first
     * occurrences of pairwise unequal dimensions in the specified subspaces.
     * Then a negative integer or a positive integer will be returned if {@code
     * d1} is less than or greater than {@code d2}. Otherwise the two subspaces
     * have equal dimensions and zero will be returned.
     * 
     */
    @Override
    public int compare(Subspace<?> s1, Subspace<?> s2) {
      if(s1 == s2 || s1.getDimensions() == null && s2.getDimensions() == null) {
        return 0;
      }

      if(s1.getDimensions() == null && s2.getDimensions() != null) {
        return -1;
      }

      if(s1.getDimensions() != null && s2.getDimensions() == null) {
        return 1;
      }

      int compare = s1.dimensionality() - s2.dimensionality();
      if(compare != 0) {
        return compare;
      }

      for(int d1 = s1.getDimensions().nextSetBit(0), d2 = s2.getDimensions().nextSetBit(0); d1 >= 0 && d2 >= 0; d1 = s1.getDimensions().nextSetBit(d1 + 1), d2 = s2.getDimensions().nextSetBit(d2 + 1)) {
        if(d1 != d2) {
          return d1 - d2;
        }
      }
      return 0;
    }
  }
}
