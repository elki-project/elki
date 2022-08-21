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
package elki.math.statistics.dependence;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

/**
 * Measure the dependence of two variables.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface Dependence {
  /**
   * Measure the dependence of two variables.
   * <p>
   * This is the more flexible API, which allows using different internal data
   * representations.
   * 
   * @param adapter1 First data adapter
   * @param data1 First data set
   * @param adapter2 Second data adapter
   * @param data2 Second data set
   * @param <A> First array type
   * @param <B> Second array type
   * @return Dependence measure
   */
  <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2);

  /**
   * Measure the dependence of two variables.
   * <p>
   * This is the more flexible API, which allows using different internal data
   * representations.
   * 
   * @param adapter Array type adapter
   * @param data1 First data set
   * @param data2 Second data set
   * @param <A> Array type
   * @return Dependence measure
   */
  default <A> double dependence(NumberArrayAdapter<?, A> adapter, A data1, A data2) {
    return dependence(adapter, data1, adapter, data2);
  }

  /**
   * Measure the dependence of two variables.
   * <p>
   * This is the more flexible API, which allows using different internal data
   * representations.
   * <p>
   * The resulting data is a serialized lower triangular matrix:
   * 
   * <pre>
   *  X  S  S  S  S  S
   *  0  X  S  S  S  S
   *  1  2  X  S  S  S
   *  3  4  5  X  S  S
   *  6  7  8  9  X  S
   * 10 11 12 13 14  X
   * </pre>
   * 
   * @param adapter Data adapter
   * @param data Data sets. Must have fast random access!
   * @param <A> Array type
   * @return Lower triangular serialized matrix
   */
  default <A> double[] dependence(NumberArrayAdapter<?, A> adapter, List<? extends A> data) {
    final int dims = data.size();
    double[] out = new double[(dims * (dims - 1)) >> 1];
    int o = 0;
    for(int y = 1; y < dims; y++) {
      A dy = data.get(y);
      for(int x = 0; x < y; x++) {
        out[o++] = dependence(adapter, data.get(x), adapter, dy);
      }
    }
    return out;
  }

  /**
   * Measure the dependence of two variables.
   * 
   * @param data1 First data set
   * @param data2 Second data set
   * @return Dependence measure
   */
  default double dependence(double[] data1, double[] data2) {
    return dependence(DoubleArrayAdapter.STATIC, data1, DoubleArrayAdapter.STATIC, data2);
  }

  /**
   * Utility functions that were previously in the abstract class.
   *
   * @author Erich Schubert
   */
  public class Utils {
    /**
     * Compute ranks of all objects, normalized to [0;1]
     * (where 0 is the smallest value, 1 is the largest).
     * 
     * @param adapter Data adapter
     * @param data Data array
     * @param len Length of data
     * @return Array of scores
     */
    public static <A> double[] computeNormalizedRanks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
      // Sort the objects:
      int[] s1 = sortedIndex(adapter, data, len);
      final double norm = .5 / (len - 1);
      double[] ret = new double[len];
      for(int i = 0; i < len;) {
        final int start = i++;
        final double val = adapter.getDouble(data, s1[start]);
        while(i < len && adapter.getDouble(data, s1[i]) <= val) {
          i++;
        }
        final double score = (start + i - 1) * norm;
        for(int j = start; j < i; j++) {
          ret[s1[j]] = score;
        }
      }
      return ret;
    }

    /**
     * Compute ranks of all objects, ranging from 1 to len.
     * <p>
     * Ties are given the average rank.
     *
     * @param adapter Data adapter
     * @param data Data array
     * @param len Length of data
     * @return Array of scores
     */
    public static <A> double[] ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
      return ranks(adapter, data, sortedIndex(adapter, data, len));
    }

    /**
     * Compute ranks of all objects, ranging from 1 to len.
     * <p>
     * Ties are given the average rank.
     *
     * @param adapter Data adapter
     * @param data Data array
     * @param idx Data index
     * @return Array of scores
     */
    public static <A> double[] ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx) {
      final int len = idx.length;
      double[] ret = new double[len];
      for(int i = 0; i < len;) {
        final int start = i++;
        final double val = adapter.getDouble(data, idx[start]);
        // Include ties:
        while(i < len && adapter.getDouble(data, idx[i]) <= val) {
          i++;
        }
        final double score = (start + i - 1) * .5 + 1;
        for(int j = start; j < i; j++) {
          ret[idx[j]] = score;
        }
      }
      return ret;
    }

    /**
     * Validate the length of the two data sets (must be the same, and non-zero)
     * 
     * @param adapter1 First data adapter
     * @param data1 First data set
     * @param adapter2 Second data adapter
     * @param data2 Second data set
     * @param <A> First array type
     * @param <B> Second array type
     */
    public static <A, B> int size(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
      final int len = adapter1.size(data1);
      if(len == 0) {
        throw new IllegalArgumentException("Empty array!");
      }
      if(len != adapter2.size(data2)) {
        throw new IllegalArgumentException("Array sizes do not match!");
      }
      return len;
    }

    /**
     * Validate the length of the two data sets (must be the same, and non-zero)
     * 
     * @param adapter Data adapter
     * @param data Data sets
     * @param <A> First array type
     */
    public static <A> int size(NumberArrayAdapter<?, A> adapter, Collection<? extends A> data) {
      if(data.size() < 2) {
        throw new IllegalArgumentException("Need at least two axes to compute dependence measures.");
      }
      Iterator<? extends A> iter = data.iterator();
      final int len = adapter.size(iter.next());
      if(len == 0) {
        throw new IllegalArgumentException("Empty array!");
      }
      while(iter.hasNext()) {
        if(len != adapter.size(iter.next())) {
          throw new IllegalArgumentException("Array sizes do not match!");
        }
      }
      return len;
    }

    /**
     * Build a sorted index of objects.
     *
     * @param adapter Data adapter
     * @param data Data array
     * @param len Length of data
     * @return Sorted index
     */
    public static <A> int[] sortedIndex(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
      int[] s1 = MathUtil.sequence(0, len);
      IntegerArrayQuickSort.sort(s1, (x, y) -> Double.compare(adapter.getDouble(data, x), adapter.getDouble(data, y)));
      return s1;
    }
  }
}
