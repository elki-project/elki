package de.lmu.ifi.dbs.elki.utilities;

import de.lmu.ifi.dbs.elki.data.RealVector;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a subspace of the original data space.
 *
 * @author Elke Achtert
 * @param <V> Vector class
 */
public class Subspace<V extends RealVector<V, ?>> {
  /**
   * The dimensions building this subspace.
   */
  private SortedSet<Integer> dimensions;

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
    this.dimensions = dimensions;
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
   * Joins this subspace with the specified subspace.
   * The join is only successful if
   * both subspaces have the first k-1 dimensions in common
   * (where k is the number of dimensions).
   *
   * @param other the subspace to join
   * @param all   the overall number of feature vectors
   * @param tau   the density threshold for the selectivity of a unit
   * @return the join of this subspace with the specified subspace
   *         if the join condition is fulfilled,
   *         null otherwise.
   */
  public Subspace<V> join(Subspace<V> other, double all, double tau) {
    SortedSet<Integer> dimensions = joinDimensions(other);
    if (dimensions == null) return null;
    else return new Subspace<V>(dimensions);
  }

  /**
   * Joins the dimensions of this subspace with the dimensions
   * of the specified subspace. The join is only successful if
   * both subspaces have the first k-1 dimensions in common
   * (where k is the number of dimensions).
   *
   * @param other the subspace to join
   * @return the joined dimensions of this subspace with the dimensions
   *         of the specified subspace if the join condition is fulfilled,
   *         null otherwise.
   */
  protected SortedSet<Integer> joinDimensions(Subspace<V> other) {
    SortedSet<Integer> otherDimensions = other.dimensions;

    if (this.dimensions.size() != otherDimensions.size())
      throw new IllegalArgumentException("different dimensions sizes!");

    if (this.dimensions.last().compareTo(otherDimensions.last()) >= 0)
      return null;

    SortedSet<Integer> result = new TreeSet<Integer>();
    Iterator<Integer> it1 = this.dimensions.iterator();
    Iterator<Integer> it2 = otherDimensions.iterator();
    for (int i = 0; i < this.dimensions.size() - 1; i++) {
      Integer dim1 = it1.next();
      Integer dim2 = it2.next();
      if (!dim1.equals(dim2)) return null;
      result.add(dim1);
    }

    result.add(this.dimensions.last());
    result.add(otherDimensions.last());
    return result;
  }

  /**
   * Returns a string representation of this subspace
   * that contains the coverage, the dimensions and the
   * dense units of this subspace.
   *
   * @return a string representation of this subspace
   */
  @Override
  public String toString() {
    return toString("");
  }

  /**
   * Returns a string representation of this subspace
   * that contains the coverage, the dimensions and the
   * dense units of this subspace.
   *
   * @param pre a string prefix
   * @return a string representation of this subspace
   */
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(pre).append("Dimensions: ").append(dimensions).append("\n");
    return result.toString();
  }
}
