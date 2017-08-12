/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * Represents a subspace of the original data space.
 *
 * @author Elke Achtert
 * @since 0.2
 *
 * @apiviz.owns de.lmu.ifi.dbs.elki.data.Subspace.DimensionComparator
 */
public class Subspace {
  /**
   * The dimensions building this subspace.
   */
  private final long[] dimensions;

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
    dimensions = BitsUtil.zero(dimension + 1);
    BitsUtil.setI(dimensions, dimension);
    dimensionality = 1;
  }

  /**
   * Creates a new k-dimensional subspace of the original data space.
   *
   * @param dimensions the dimensions building this subspace
   */
  public Subspace(long[] dimensions) {
    this.dimensions = dimensions;
    dimensionality = BitsUtil.cardinality(dimensions);
  }

  /**
   * Returns the BitSet representing the dimensions of this subspace.
   *
   * @return the dimensions of this subspace
   */
  public final long[] getDimensions() {
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
  public Subspace join(Subspace other) {
    long[] newDimensions = joinLastDimensions(other);
    if(newDimensions == null) {
      return null;
    }

    return new Subspace(newDimensions);
  }

  /**
   * Returns a string representation of this subspace by calling
   * {@link #toString} with an empty prefix.
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
    StringBuilder result = new StringBuilder();
    result.append(pre).append("Dimensions: [");
    int start = BitsUtil.nextSetBit(dimensions, 0);
    for(int d = start; d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      if(d != start) {
        result.append(", ");
      }
      result.append(d + 1);
    }
    result.append("]");
    return result.toString();
  }

  /**
   * Returns a string representation of the dimensions of this subspace
   * separated by comma.
   *
   * @return a string representation of the dimensions of this subspace
   */
  public String dimensonsToString() {
    return dimensonsToString(", ");
  }

  /**
   * Returns a string representation of the dimensions of this subspace.
   *
   * @param sep the separator between the dimensions
   * @return a string representation of the dimensions of this subspace
   */
  public String dimensonsToString(String sep) {
    StringBuilder result = new StringBuilder();
    result.append("[");
    for(int dim = BitsUtil.nextSetBit(dimensions, 0); dim >= 0; dim = BitsUtil.nextSetBit(dimensions, dim + 1)) {
      if(result.length() == 1) {
        result.append(dim + 1);
      }
      else {
        result.append(sep).append(dim + 1);
      }
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
  public boolean isSubspace(Subspace subspace) {
    if(this.dimensionality > subspace.dimensionality) {
      return false;
    }
    // FIXME: use bit operations.
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      if(!BitsUtil.get(subspace.dimensions, d)) {
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
  protected long[] joinLastDimensions(Subspace other) {
    if(this.dimensionality != other.dimensionality) {
      return null;
    }

    int alloc = MathUtil.max(dimensions.length, other.dimensions.length);
    long[] resultDimensions = new long[alloc];
    int last1 = -1, last2 = -1;

    for(int d1 = BitsUtil.nextSetBit(this.dimensions, 0), d2 = BitsUtil.nextSetBit(other.dimensions, 0); //
    d1 >= 0 && d2 >= 0; //
    d1 = BitsUtil.nextSetBit(this.dimensions, d1 + 1), d2 = BitsUtil.nextSetBit(other.dimensions, d2 + 1)) {
      if(d1 == d2) {
        BitsUtil.setI(resultDimensions, d1);
      }
      last1 = d1;
      last2 = d2;
    }

    if(last1 >= 0 && last2 >= 0 && last1 < last2) {
      BitsUtil.setI(resultDimensions, last1);
      BitsUtil.setI(resultDimensions, last2);
      return resultDimensions;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the hash code value of the {@link #dimensions} of this subspace.
   *
   * @return a hash code value for this subspace
   */
  @Override
  public int hashCode() {
    return BitsUtil.hashCode(dimensions);
  }

  /**
   * Indicates if the specified object is equal to this subspace, i.e. if the
   * specified object is a Subspace and is built of the same dimensions than
   * this subspace.
   *
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof Subspace)) {
      return false;
    }
    Subspace other = (Subspace) obj;
    return new DimensionComparator().compare(this, other) == 0;
  }

  /**
   * A comparator for subspaces based on their involved dimensions. The
   * subspaces are ordered according to the ordering of their dimensions.
   *
   * @author Elke Achtert
   */
  public static class DimensionComparator implements Comparator<Subspace> {
    /**
     * Compares the two specified subspaces for order. If the two subspaces have
     * different dimensionalities a negative integer or a positive integer will
     * be returned if the dimensionality of the first subspace is less than or
     * greater than the dimensionality of the second subspace. Otherwise the
     * comparison works as follows: Let {@code d1} and {@code d2} be the first
     * occurrences of pairwise unequal dimensions in the specified subspaces.
     * Then a negative integer or a positive integer will be returned if
     * {@code d1} is less than or greater than {@code d2}. Otherwise the two
     * subspaces have equal dimensions and zero will be returned.
     *
     * {@inheritDoc}
     */
    @Override
    public int compare(Subspace s1, Subspace s2) {
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

      for(int d1 = BitsUtil.nextSetBit(s1.getDimensions(), 0), d2 = BitsUtil.nextSetBit(s2.getDimensions(), 0); d1 >= 0 && d2 >= 0; d1 = BitsUtil.nextSetBit(s1.getDimensions(), d1 + 1), d2 = BitsUtil.nextSetBit(s2.getDimensions(), d2 + 1)) {
        if(d1 != d2) {
          return d1 - d2;
        }
      }
      return 0;
    }
  }
}
