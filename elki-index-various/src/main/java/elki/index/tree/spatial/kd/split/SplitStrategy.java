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
package elki.index.tree.spatial.kd.split;

import elki.data.NumberVector;
import elki.data.VectorUtil.SortDBIDsBySingleDimension;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDArrayMIter;
import elki.database.relation.Relation;

/**
 * Split strategy for full k-d-tree construction. This interface is engineered
 * to work on a modifiable array of DBIDs (more precisely, on a range), and
 * the split partitions are encoded in this array.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public interface SplitStrategy {
  /**
   * Build the k-d-tree using midpoint splitting.
   *
   * @param relation data relation
   * @param dims data dimensionality
   * @param sorted modifiable array to encode the partitions
   * @param iter reusable iterator to the array
   * @param left left subinterval
   * @param right right subinterval
   * @param comp reusable comparator (bound to the relation above)
   * @return split info, null if no good split
   */
  Info findSplit(Relation<? extends NumberVector> relation, int dims, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, int left, int right, SortDBIDsBySingleDimension comp);

  /**
   * Split information.
   * 
   * @author Erich Schubert
   */
  public static class Info {
    /**
     * Splitting value
     */
    public double val;

    /**
     * Split dimension
     */
    public int dim;

    /**
     * Split position in array
     */
    public int pos;

    /**
     * Constructor.
     *
     * @param dim Dimension
     * @param pos Split position in array
     * @param val Spplitting threshold
     */
    public Info(int dim, int pos, double val) {
      this.dim = dim;
      this.pos = pos;
      this.val = val;
    }
  }

  /**
   * Utility functions.
   *
   * @author Erich Schubert
   */
  public static final class Util {
    /**
     * Private constructor. Do not instatiate.
     */
    private Util() {
      // Do not instatiate, use static methods direclty.
    }

    /**
     * Find the minimum and maximum in each dimension of a range of values.
     *
     * @param dims Number of dimensions
     * @param relation Data relation
     * @param iter Iterator
     * @param left Beginning of range
     * @param right End of range
     * @return Array containing minima, then maxima
     */
    static double[] minmaxRange(int dims, Relation<? extends NumberVector> relation, DBIDArrayIter iter, int left, int right) {
      double[] minmax = new double[dims << 1];
      final NumberVector vec1 = relation.get(iter.seek(left));
      for(int d = 0, d2 = dims; d < dims; d++, d2++) {
        minmax[d] = minmax[d2] = vec1.doubleValue(d);
      }
      for(int i = left + 1; i < right; i++) {
        final NumberVector vec = relation.get(iter.seek(i));
        for(int d = 0, d2 = dims; d < dims; d++, d2++) {
          final double v = vec.doubleValue(d);
          minmax[d] = v < minmax[d] ? v : minmax[d];
          minmax[d2] = v > minmax[d2] ? v : minmax[d2];
        }
      }
      return minmax;
    }

    /**
     * Compute the sum and sum-of-squares (for variance).
     * 
     * @param relation Data relation
     * @param dims Dimensionality
     * @param iter Iterator
     * @param left Interval start
     * @param right Interval end
     * @return Array containing sums and variances
     */
    static double[] sumvar(Relation<? extends NumberVector> relation, int dims, DBIDArrayMIter iter, int left, int right) {
      double[] sumvar = new double[dims << 1];
      NumberVector vec1 = relation.get(iter.seek(left));
      for(int d = 0; d < dims; d++) { // Simply copy the first
        sumvar[d] = vec1.doubleValue(d);
      }
      for(int i = left + 1, j = 1; i < right; i++, j++) {
        NumberVector vec = relation.get(iter.seek(i));
        double f = 1. / (j * (j + 1));
        for(int d = 0, d2 = dims; d < dims; d++, d2++) {
          // using Youngs and Cramer incremental variance
          final double v = vec.doubleValue(d);
          final double tmp = j * v - sumvar[d];
          sumvar[d] += v;
          sumvar[d2] += tmp * tmp * f;
        }
      }
      return sumvar;
    }

    /**
     * Find the largest difference
     *
     * @param minmax Minima, then maxima
     * @return Index of largest difference
     */
    static int argmaxdiff(double[] minmax) {
      final int dims = minmax.length >> 1;
      int dim = 0;
      double v = minmax[dims] - minmax[0];
      for(int d = 1, d2 = dims + 1; d < dims; d++, d2++) {
        double v2 = minmax[d2] - minmax[d];
        if(v2 > v) {
          v = v2;
          dim = d;
        }
      }
      return dim;
    }

    /**
     * Pivot an interval.
     * 
     * @param relation Data relation
     * @param sorted Sorted list of entries
     * @param iter List iterator
     * @param dim Dimension
     * @param left Left range of the list
     * @param right Right end of the list range
     * @param mid Value to pivot to
     * @return Pivot position
     */
    static int pivot(Relation<? extends NumberVector> relation, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, final int dim, int left, int right, double mid) {
      int l = left, r = right - 1;
      while(true) {
        while(l <= r && relation.get(iter.seek(l)).doubleValue(dim) <= mid) {
          ++l;
        }
        while(l <= r && relation.get(iter.seek(r)).doubleValue(dim) >= mid) {
          --r;
        }
        if(l >= r) {
          break;
        }
        sorted.swap(l++, r--);
      }
      assert relation.get(iter.seek(r)).doubleValue(dim) <= mid : relation.get(iter.seek(r)).doubleValue(dim) + " not less than " + mid;
      ++r; // exclusive
      assert r == right || relation.get(iter.seek(r)).doubleValue(dim) >= mid : relation.get(iter.seek(r)).doubleValue(dim) + " not at least " + mid;
      return r;
    }
  }
}
