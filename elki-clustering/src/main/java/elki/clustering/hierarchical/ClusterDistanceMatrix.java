/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.hierarchical;

import elki.math.MathUtil;
import elki.utilities.exceptions.AbortException;

/**
 * Shared code for algorithms that work on a pairwise cluster distance matrix.
 * <p>
 * Note that this requires O(n²) memory (and often O(n³) runtime).
 * <p>
 * This class bridges the gap from the relational (indexed by identifiers) and
 * the matrix view (indexed by integers 0...n-1).
 * <p>
 * While this will usually store (merge-) distances when clustering, it can
 * store arbitrary doubles.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ClusterDistanceMatrix {
  /**
   * Distance matrix (<b>modifiable</b>).
   */
  public final double[] matrix;

  /**
   * Mapping from positions to cluster numbers
   */
  public final int[] clustermap;

  /**
   * Number of rows/columns.
   */
  public final int size;

  /**
   * Constructor.
   *
   * @param size Size
   */
  public ClusterDistanceMatrix(int size) {
    this.size = size;
    if(size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + //
          0x10000 // = 65535
          + " instances (~16 GB RAM), at which point the Java maximum array size is reached.");
    }
    matrix = new double[triangleSize(size)];
    clustermap = MathUtil.sequence(0, size);
  }

  /**
   * Compute the size of a complete x by x triangle (minus diagonal)
   *
   * @param x Offset
   * @return Size of complete triangle
   */
  public static int triangleSize(int x) {
    return (x * (x - 1)) >>> 1;
  }

  /**
   * Get a value from the (upper triangular) distance matrix.
   * <p>
   * Note: in many cases, linear iteration over the matrix will be faster than
   * repeated calls to this method!
   *
   * @param x First object
   * @param y Second object
   * @return Distance
   */
  public double get(int x, int y) {
    return x == y ? 0 : x < y ? matrix[triangleSize(y) + x] : matrix[triangleSize(x) + y];
  }
}
