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
package de.lmu.ifi.dbs.elki.data;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;

/**
 * Represents a subspace of the original data space.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @composed - - - de.lmu.ifi.dbs.elki.data.Subspace.DimensionComparator
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
    return newDimensions != null ? new Subspace(newDimensions) : null;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(100 + 5 * dimensionality).append("Dimensions: [");
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      result.append(d + 1).append(", ");
    }
    if(result.length() >= 2) { // Un-append ", "
      result.setLength(result.length() - 2);
    }
    return result.append(']').toString();
  }

  /**
   * Returns a string representation of the dimensions of this subspace
   * separated by comma.
   *
   * @return a string representation of the dimensions of this subspace
   */
  public String dimensionsToString() {
    return dimensonsToString(", ");
  }

  /**
   * Returns a string representation of the dimensions of this subspace.
   *
   * @param sep the separator between the dimensions
   * @return a string representation of the dimensions of this subspace
   */
  public String dimensonsToString(String sep) {
    StringBuilder result = new StringBuilder(100).append('[');
    for(int dim = BitsUtil.nextSetBit(dimensions, 0); dim >= 0; dim = BitsUtil.nextSetBit(dimensions, dim + 1)) {
      result.append(dim + 1).append(sep);
    }
    if(result.length() > sep.length()) { // Un-append last separator
      result.setLength(result.length() - sep.length());
    }
    return result.append(']').toString();
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
    return this.dimensionality <= subspace.dimensionality && //
        BitsUtil.intersectionSize(dimensions, subspace.dimensions) == dimensionality;
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
    if(this.dimensionality == 1) {
      return BitsUtil.orI(BitsUtil.copy(this.dimensions), other.dimensions);
    }
    int last1 = BitsUtil.capacity(this.dimensions) - BitsUtil.numberOfLeadingZeros(this.dimensions) - 1;
    int last2 = BitsUtil.capacity(other.dimensions) - BitsUtil.numberOfLeadingZeros(other.dimensions) - 1;
    if(last2 < 0 || last1 >= last2) {
      return null;
    }
    long[] result = BitsUtil.orI(BitsUtil.copy(other.dimensions), this.dimensions);
    return BitsUtil.cardinality(result) == dimensionality + 1 ? result : null;
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

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj != null && getClass() == obj.getClass() && //
        BitsUtil.equal(this.dimensions, ((Subspace) obj).dimensions);
  }

  /**
   * A comparator for subspaces based on their involved dimensions. The
   * subspaces are ordered according to the ordering of their dimensions.
   * <p>
   * If the two subspaces have different dimensionalities a negative integer or
   * a positive integer will be returned if the dimensionality of the first
   * subspace is less than or greater than the dimensionality of the second
   * subspace. Otherwise the comparison works as follows: Let {@code d1} and
   * {@code d2} be the first occurrences of pairwise unequal dimensions in the
   * specified subspaces. Then a negative integer or a positive integer will be
   * returned if {@code d1} is less than or greater than {@code d2}. Otherwise
   * the two subspaces have equal dimensions and zero will be returned.
   */
  public static Comparator<Subspace> DIMENSION_COMPARATOR = new Comparator<Subspace>() {
    @Override
    public int compare(Subspace s1, Subspace s2) {
      if(s1 == s2 || s1.getDimensions() == s2.getDimensions()) {
        return 0;
      }

      if(s1.getDimensions() == null) {
        return -1;
      }

      if(s2.getDimensions() == null) {
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
  };
}
