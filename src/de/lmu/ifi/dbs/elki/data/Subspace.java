package de.lmu.ifi.dbs.elki.data;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;

/**
 * Represents a subspace of the original data space.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector this subspace contains
 */
public class Subspace<V extends FeatureVector<V, ?>> implements TextWriteable, Model {
  /**
   * The dimensions building this subspace.
   */
  private TreeSet<Integer> dimensions;

  /**
   * Creates a new one-dimensional subspace of the original data space.
   * 
   * @param dimension the dimension building this subspace
   */
  public Subspace(int dimension) {
    dimensions = new TreeSet<Integer>();
    dimensions.add(dimension);
  }

  /**
   * Creates a new k-dimensional subspace of the original data space.
   * 
   * @param dimensions the dimensions building this subspace
   */
  public Subspace(SortedSet<Integer> dimensions) {
    this.dimensions = new TreeSet<Integer>();
    this.dimensions.addAll(dimensions);
  }

  /**
   * Returns the set of dimensions of this subspace.
   * 
   * @return the dimensions of this subspace
   */
  public final SortedSet<Integer> getDimensions() {
    return dimensions;
  }

  /**
   * Returns the dimensionality of this subspace.
   * 
   * @return the number of dimensions this subspace contains
   */
  public final int dimensionality() {
    return dimensions.size();
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
    SortedSet<Integer> newDimensions = joinLastDimensions(other);
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
    for(Iterator<Integer> it = dimensions.iterator(); it.hasNext();) {
      int d = it.next();
      result.append(d + 1);
      if(it.hasNext()) {
        result.append(", ");
      }
    }
    result.append("]");
    return result.toString();
  }

  /**
   * Serialize using {@link #toString()} method.
   */
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn(this.toString());
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
    return subspace.dimensions.containsAll(this.dimensions);
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
   * @throws IllegalArgumentException if the dimensionality of the subspaces to
   *         join differs
   */
  protected SortedSet<Integer> joinLastDimensions(Subspace<V> other) {
//    BitSet dims = new BitSet();
//    
//    for (int i = dims.nextSetBit(0); i >= 0; i = dims.nextSetBit(i + 1)) {
//    }
//    
//    dims.
    
    
    SortedSet<Integer> otherDimensions = other.dimensions;

    if(this.dimensionality() != other.dimensionality()) {
      throw new IllegalArgumentException("different number of dimensions: " + this.dimensionality() + " != " + other.dimensionality());
    }

    if(this.dimensions.last().compareTo(otherDimensions.last()) >= 0) {
      return null;
    }

    SortedSet<Integer> result = new TreeSet<Integer>();
    Iterator<Integer> it1 = this.dimensions.iterator();
    Iterator<Integer> it2 = otherDimensions.iterator();
    for(int i = 0; i < this.dimensions.size() - 1; i++) {
      Integer dim1 = it1.next();
      Integer dim2 = it2.next();
      if(!dim1.equals(dim2)) {
        return null;
      }
      result.add(dim1);
    }

    result.add(this.dimensions.last());
    result.add(otherDimensions.last());
    return result;
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dimensions == null) ? 0 : dimensions.hashCode());
    return result;
  }

  /*
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
      
      int compare = s1.dimensionality()-s2.dimensionality();
      if (compare != 0) {
        return compare;
      }

      Iterator<Integer> it1 = s1.getDimensions().iterator();
      Iterator<Integer> it2 = s2.getDimensions().iterator();
      while(it1.hasNext()) {
        Integer d1 = it1.next();
        Integer d2 = it2.next();
        if(d1 == d2) {
          continue;
        }
        else {
          return d1 - d2;
        }
      }

      return 0;
    }
  }
}
