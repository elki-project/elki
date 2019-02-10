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
package de.lmu.ifi.dbs.elki.utilities.datastructures;

import java.util.Comparator;
import java.util.List;

/**
 * QuickSelect computes ("selects") the element at a given rank and can be used
 * to compute Medians and arbitrary quantiles by computing the appropriate rank.
 * <p>
 * This algorithm is essentially an incomplete QuickSort that only descends into
 * that part of the data that we are interested in, and also attributed to
 * Charles Antony Richard Hoare.
 * <p>
 * If you want to use this with a {@code Comparable} type, use the natural
 * comparator {@code Comparator.naturalOrder()}. You can wrap arrays as lists
 * with {@code java.util.Arrays#asList}, to sort them in-place, or implement
 * an {@link Adapter} for your data structure.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - List
 * @assoc - - - Adapter
 */
public class QuickSelect {
  /**
   * For small arrays, use a simpler method:
   */
  private static final int SMALL = 47;

  /**
   * Do not instantiate - static methods only!
   */
  private QuickSelect() {
    // Do not instantiate - static methods only!
  }

  /**
   * Choose the best pivot for the given rank.
   *
   * @param rank Rank
   * @param m1 Pivot candidate
   * @param m2 Pivot candidate
   * @param m3 Pivot candidate
   * @param m4 Pivot candidate
   * @param m5 Pivot candidate
   * @return Best pivot candidate
   */
  private static final int bestPivot(int rank, int m1, int m2, int m3, int m4, int m5) {
    if(rank < m1) {
      return m1;
    }
    if(rank > m5) {
      return m5;
    }
    if(rank < m2) {
      return m2;
    }
    if(rank > m4) {
      return m4;
    }
    return m3;
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * @param data Data to process
   * @param start Interval start
   * @param end Interval end (exclusive)
   * @param rank rank position we are interested in (starting at 0)
   */
  public static <T> void quickSelect(T data, Adapter<T> adapter, int start, int end, int rank) {
    while(true) {
      // Optimization for small arrays
      // This also ensures a minimum size below
      if(start + SMALL > end) {
        insertionSort(data, adapter, start, end);
        return;
      }

      // Best of 5 pivot picking:
      // Choose pivots by looking at five candidates.
      final int len = end - start;
      final int seventh = (len >> 3) + (len >> 6) + 1;
      final int m3 = (start + end) >> 1; // middle
      final int m2 = m3 - seventh;
      final int m1 = m2 - seventh;
      final int m4 = m3 + seventh;
      final int m5 = m4 + seventh;

      // Explicit (and optimal) sorting network for 5 elements
      // See Knuth for details.
      if(adapter.compareGreater(data, m1, m2)) {
        adapter.swap(data, m1, m2);
      }
      if(adapter.compareGreater(data, m1, m3)) {
        adapter.swap(data, m1, m3);
      }
      if(adapter.compareGreater(data, m2, m3)) {
        adapter.swap(data, m2, m3);
      }
      if(adapter.compareGreater(data, m4, m5)) {
        adapter.swap(data, m4, m5);
      }
      if(adapter.compareGreater(data, m1, m4)) {
        adapter.swap(data, m1, m4);
      }
      if(adapter.compareGreater(data, m3, m4)) {
        adapter.swap(data, m3, m4);
      }
      if(adapter.compareGreater(data, m2, m5)) {
        adapter.swap(data, m2, m5);
      }
      if(adapter.compareGreater(data, m2, m3)) {
        adapter.swap(data, m2, m3);
      }
      if(adapter.compareGreater(data, m4, m5)) {
        adapter.swap(data, m4, m5);
      }

      int best = bestPivot(rank, m1, m2, m3, m4, m5);
      // final double pivot = data[best];
      // Move middle element out of the way.
      adapter.swap(data, best, end - 1);

      // Begin partitioning
      int i = start, j = end - 2;
      // This is classic quicksort stuff
      while(true) {
        while(i <= j && adapter.compareGreater(data, end - 1, i)) {
          i++;
        }
        while(j >= i && !adapter.compareGreater(data, end - 1, j)) {
          j--;
        }
        if(i >= j) {
          break;
        }
        adapter.swap(data, i, j);
      }

      // Move pivot (former middle element) back into the appropriate place
      adapter.swap(data, i, end - 1);

      // In contrast to quicksort, we only need to recurse into the half we are
      // interested in. Instead of recursion we now use iteration.
      if(rank < i) {
        end = i;
      }
      else if(rank > i) {
        start = i + 1;
      }
      else {
        break;
      }
    } // Loop until rank==i
  }

  /**
   * Sort a small array using repetitive insertion sort.
   *
   * @param data Data to sort
   * @param start Interval start
   * @param end Interval end
   */
  private static <T> void insertionSort(T data, Adapter<T> adapter, int start, int end) {
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start && adapter.compareGreater(data, j - 1, j); j--) {
        adapter.swap(data, j, j - 1);
      }
    }
  }

  /**
   * Adapter class to apply QuickSelect to arbitrary data structures.
   *
   * @author Erich Schubert
   *
   * @param <T> Data structure type
   */
  public interface Adapter<T> {
    /**
     * Swap the two elements at positions i and j.
     *
     * @param data Data structure
     * @param i Position i
     * @param j Position j
     */
    void swap(T data, int i, int j);

    /**
     * Compare two elements.
     *
     * @param data Data structure
     * @param i Position i
     * @param j Position j
     * @return {@code true} when the element at position i is greater than that
     *         at position j.
     */
    boolean compareGreater(T data, int i, int j);
  }

  /**
   * Adapter for double arrays.
   */
  public static Adapter<double[]> DOUBLE_ADAPTER = new Adapter<double[]>() {
    @Override
    public void swap(double[] data, int i, int j) {
      double tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(double[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * Adapter for integer arrays.
   */
  public static Adapter<int[]> INTEGER_ADAPTER = new Adapter<int[]>() {
    @Override
    public void swap(int[] data, int i, int j) {
      int tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(int[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * Adapter for float arrays.
   */
  public static Adapter<float[]> FLOAT_ADAPTER = new Adapter<float[]>() {
    @Override
    public void swap(float[] data, int i, int j) {
      float tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(float[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * Adapter for short arrays.
   */
  public static Adapter<short[]> SHORT_ADAPTER = new Adapter<short[]>() {
    @Override
    public void swap(short[] data, int i, int j) {
      short tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(short[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * Adapter for long arrays.
   */
  public static Adapter<long[]> LONG_ADAPTER = new Adapter<long[]>() {
    @Override
    public void swap(long[] data, int i, int j) {
      long tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(long[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * Adapter for byte arrays.
   */
  public static Adapter<byte[]> BYTE_ADAPTER = new Adapter<byte[]>() {
    @Override
    public void swap(byte[] data, int i, int j) {
      byte tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(byte[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * Adapter for char arrays.
   */
  public static Adapter<char[]> CHAR_ADAPTER = new Adapter<char[]>() {
    @Override
    public void swap(char[] data, int i, int j) {
      char tmp = data[i];
      data[i] = data[j];
      data[j] = tmp;
    }

    @Override
    public boolean compareGreater(char[] data, int i, int j) {
      return data[i] > data[j];
    }
  };

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param rank Rank position that we are interested in (integer!)
   * @return Value at the given rank
   */
  public static double quickSelect(double[] data, int rank) {
    quickSelect(data, 0, data.length, rank);
    return data[rank];
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @return Median value
   */
  public static double median(double[] data) {
    return median(data, 0, data.length);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive!)
   * @return Median value
   */
  public static double median(double[] data, int begin, int end) {
    final int length = end - begin;
    assert (length > 0);
    // Integer division is "floor" since we are non-negative.
    final int left = begin + ((length - 1) >> 1);
    quickSelect(data, begin, end, left);
    if(length % 2 == 1) {
      return data[left];
    }
    else {
      quickSelect(data, left + 1, end, left + 1);
      return data[left] + .5 * (data[left + 1] - data[left]);
    }
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param quant Quantile to compute
   * @return Value at quantile
   */
  public static double quantile(double[] data, double quant) {
    return quantile(data, 0, data.length, quant);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive!)
   * @param quant Quantile to compute
   * @return Value at quantile
   */
  public static double quantile(double[] data, int begin, int end, double quant) {
    final int length = end - begin;
    assert (length > 0) : "Quantile on empty set?";
    // Integer division is "floor" since we are non-negative.
    final double dleft = begin + (length - 1) * quant;
    final int ileft = (int) Math.floor(dleft);
    final double err = dleft - ileft;

    quickSelect(data, begin, end, ileft);
    if(err <= Double.MIN_NORMAL) {
      return data[ileft];
    }
    else {
      quickSelect(data, ileft + 1, end, ileft + 1);
      // Mix:
      double mix = data[ileft] + (data[ileft + 1] - data[ileft]) * err;
      return mix;
    }
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * @param data Data to process
   * @param start Interval start
   * @param end Interval end (exclusive)
   * @param rank rank position we are interested in (starting at 0)
   * @return Element at the given rank (starting at 0).
   */
  public static double quickSelect(double[] data, int start, int end, int rank) {
    while(true) {
      // Optimization for small arrays
      // This also ensures a minimum size below
      if(start + SMALL > end) {
        insertionSort(data, start, end);
        return data[rank];
      }

      // Best of 5 pivot picking:
      // Choose pivots by looking at five candidates.
      final int len = end - start;
      final int seventh = (len >> 3) + (len >> 6) + 1;
      final int m3 = (start + end) >> 1; // middle
      final int m2 = m3 - seventh;
      final int m1 = m2 - seventh;
      final int m4 = m3 + seventh;
      final int m5 = m4 + seventh;

      // Explicit (and optimal) sorting network for 5 elements
      // See Knuth for details.
      if(data[m1] > data[m2]) {
        swap(data, m1, m2);
      }
      if(data[m1] > data[m3]) {
        swap(data, m1, m3);
      }
      if(data[m2] > data[m3]) {
        swap(data, m2, m3);
      }
      if(data[m4] > data[m5]) {
        swap(data, m4, m5);
      }
      if(data[m1] > data[m4]) {
        swap(data, m1, m4);
      }
      if(data[m3] > data[m4]) {
        swap(data, m3, m4);
      }
      if(data[m2] > data[m5]) {
        swap(data, m2, m5);
      }
      if(data[m2] > data[m3]) {
        swap(data, m2, m3);
      }
      if(data[m4] > data[m5]) {
        swap(data, m4, m5);
      }

      int best = bestPivot(rank, m1, m2, m3, m4, m5);
      final double pivot = data[best];
      // Move middle element out of the way.
      swap(data, best, end - 1);

      // Begin partitioning
      int i = start, j = end - 2;
      // This is classic quicksort stuff
      while(true) {
        while(i <= j && data[i] <= pivot) {
          i++;
        }
        while(j >= i && data[j] >= pivot) {
          j--;
        }
        if(i >= j) {
          break;
        }
        swap(data, i, j);
        i++;
        j--;
      }

      // Move pivot (former middle element) back into the appropriate place
      swap(data, i, end - 1);

      // Skip duplicates to narrow down the search interval:
      while(rank < i && data[i - 1] == pivot) {
        --i;
      }
      while(rank > i && data[i + 1] == pivot) {
        ++i;
      }

      // In contrast to quicksort, we only need to recurse into the half we are
      // interested in. Instead of recursion we now use iteration.
      if(rank < i) {
        end = i;
      }
      else if(rank > i) {
        start = i + 1;
      }
      else {
        break;
      }
    } // Loop until rank==i
    return data[rank];
  }

  /**
   * Sort a small array using repetitive insertion sort.
   *
   * @param data Data to sort
   * @param start Interval start
   * @param end Interval end
   */
  private static void insertionSort(double[] data, int start, int end) {
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start && data[j - 1] > data[j]; j--) {
        swap(data, j, j - 1);
      }
    }
  }

  /**
   * The usual swap method.
   *
   * @param data Array
   * @param a First index
   * @param b Second index
   */
  private static final void swap(double[] data, int a, int b) {
    double tmp = data[a];
    data[a] = data[b];
    data[b] = tmp;
  }

  /**
   * The usual swap method.
   *
   * @param <T> object type
   * @param data Array
   * @param a First index
   * @param b Second index
   */
  private static final <T> void swap(List<T> data, int a, int b) {
    data.set(b, data.set(a, data.get(b)));
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param <T> object type
   * @param data Data to process
   * @param comparator Comparator to use
   * @param rank Rank position that we are interested in (integer!)
   * @return Value at the given rank
   */
  public static <T> T quickSelect(List<? extends T> data, Comparator<? super T> comparator, int rank) {
    quickSelect(data, comparator, 0, data.size(), rank);
    return data.get(rank);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param <T> object type
   * @param data Data to process
   * @param comparator Comparator to use
   * @return Median value
   */
  public static <T> T median(List<? extends T> data, Comparator<? super T> comparator) {
    return median(data, comparator, 0, data.size());
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * On an odd length, it will return the lower element.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param <T> object type
   * @param data Data to process
   * @param comparator Comparator to use
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive!)
   * @return Median value
   */
  public static <T> T median(List<? extends T> data, Comparator<? super T> comparator, int begin, int end) {
    final int length = end - begin;
    assert (length > 0);
    // Integer division is "floor" since we are non-negative.
    final int left = begin + ((length - 1) >> 1);
    quickSelect(data, comparator, begin, end, left);
    return data.get(left);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param <T> object type
   * @param data Data to process
   * @param comparator Comparator to use
   * @param quant Quantile to compute
   * @return Value at quantile
   */
  public static <T> T quantile(List<? extends T> data, Comparator<? super T> comparator, double quant) {
    return quantile(data, comparator, 0, data.size(), quant);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * It will prefer the lower element.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param <T> object type
   * @param data Data to process
   * @param comparator Comparator to use
   * @param begin Begin of valid values
   * @param end End of valid values (inclusive!)
   * @param quant Quantile to compute
   * @return Value at quantile
   */
  public static <T> T quantile(List<? extends T> data, Comparator<? super T> comparator, int begin, int end, double quant) {
    final int length = end - begin;
    assert (length > 0) : "Quantile on empty set?";
    // Integer division is "floor" since we are non-negative.
    final double dleft = begin + (length - 1) * quant;
    final int ileft = (int) Math.floor(dleft);

    quickSelect(data, comparator, begin, end, ileft);
    return data.get(ileft);
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * @param <T> object type
   * @param data Data to process
   * @param comparator Comparator to use
   * @param start Interval start
   * @param end Interval end (inclusive)
   * @param rank rank position we are interested in (starting at 0)
   */
  public static <T> void quickSelect(List<? extends T> data, Comparator<? super T> comparator, int start, int end, int rank) {
    while(true) {
      // Optimization for small arrays
      // This also ensures a minimum size below
      if(start + SMALL > end) {
        insertionSort(data, comparator, start, end);
        return;
      }

      // Best of 5 pivot picking:
      // Choose pivots by looking at five candidates.
      final int len = end - start;
      final int seventh = (len >> 3) + (len >> 6) + 1;
      final int m3 = (start + end) >> 1; // middle
      final int m2 = m3 - seventh;
      final int m1 = m2 - seventh;
      final int m4 = m3 + seventh;
      final int m5 = m4 + seventh;

      // Explicit (and optimal) sorting network for 5 elements
      // See Knuth for details.
      if(comparator.compare(data.get(m1), data.get(m2)) > 0) {
        swap(data, m1, m2);
      }
      if(comparator.compare(data.get(m1), data.get(m3)) > 0) {
        swap(data, m1, m3);
      }
      if(comparator.compare(data.get(m2), data.get(m3)) > 0) {
        swap(data, m2, m3);
      }
      if(comparator.compare(data.get(m4), data.get(m5)) > 0) {
        swap(data, m4, m5);
      }
      if(comparator.compare(data.get(m1), data.get(m4)) > 0) {
        swap(data, m1, m4);
      }
      if(comparator.compare(data.get(m3), data.get(m4)) > 0) {
        swap(data, m3, m4);
      }
      if(comparator.compare(data.get(m2), data.get(m5)) > 0) {
        swap(data, m2, m5);
      }
      if(comparator.compare(data.get(m2), data.get(m3)) > 0) {
        swap(data, m2, m3);
      }
      if(comparator.compare(data.get(m4), data.get(m5)) > 0) {
        swap(data, m4, m5);
      }

      int best = bestPivot(rank, m1, m2, m3, m4, m5);
      final T pivot = data.get(best);
      // Move middle element out of the way, just before end
      // (Since we already know that "end" is bigger)
      swap(data, best, end - 1);

      // Begin partitioning
      int i = start, j = end - 2;
      // This is classic quicksort stuff
      while(true) {
        while(i <= j && comparator.compare(data.get(i), pivot) <= 0) {
          i++;
        }
        while(j >= i && comparator.compare(data.get(j), pivot) >= 0) {
          j--;
        }
        if(i >= j) {
          break;
        }
        swap(data, i, j);
      }

      // Move pivot (former middle element) back into the appropriate place
      swap(data, i, end - 1);

      // Skip duplicates to narrow down the search interval:
      while(rank < i && comparator.compare(data.get(i - 1), pivot) == 0) {
        --i;
      }
      while(rank > i && comparator.compare(data.get(i + 1), pivot) == 0) {
        ++i;
      }

      // In contrast to quicksort, we only need to recurse into the half we are
      // interested in. Instead of recursion we now use iteration.
      if(rank < i) {
        end = i;
      }
      else if(rank > i) {
        start = i + 1;
      }
      else {
        break;
      }
    } // Loop until rank==i
  }

  /**
   * Sort a small array using repetitive insertion sort.
   *
   * @param <T> object type
   * @param data Data to sort
   * @param start Interval start
   * @param end Interval end
   */
  private static <T> void insertionSort(List<T> data, Comparator<? super T> comparator, int start, int end) {
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start && comparator.compare(data.get(j - 1), data.get(j)) > 0; j--) {
        swap(data, j, j - 1);
      }
    }
  }
}
