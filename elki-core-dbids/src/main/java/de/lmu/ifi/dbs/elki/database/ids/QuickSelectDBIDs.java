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
package de.lmu.ifi.dbs.elki.database.ids;

import java.util.Comparator;

/**
 * QuickSelect computes ("selects") the element at a given rank and can be used
 * to compute Medians and arbitrary quantiles by computing the appropriate rank.
 *
 * This algorithm is essentially an incomplete QuickSort that only descends into
 * that part of the data that we are interested in, and also attributed to
 * Charles Antony Richard Hoare
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @assoc - - - ArrayModifiableDBIDs
 */
public class QuickSelectDBIDs {
  /**
   * For small arrays, use a simpler method:
   */
  private static final int SMALL = 47;

  /**
   * Do not instantiate - static methods only!
   */
  private QuickSelectDBIDs() {
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
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param comparator Comparator to use
   * @param rank Rank position that we are interested in (integer!)
   */
  public static void quickSelect(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator, int rank) {
    quickSelect(data, comparator, 0, data.size(), rank);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param comparator Comparator to use
   * @return Median position
   */
  public static int median(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator) {
    return median(data, comparator, 0, data.size());
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * On an odd length, it will return the lower element.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param comparator Comparator to use
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive!)
   * @return Median position
   */
  public static int median(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator, int begin, int end) {
    final int length = end - begin;
    assert (length > 0);
    // Integer division is "floor" since we are non-negative.
    final int left = begin + ((length - 1) >> 1);
    quickSelect(data, comparator, begin, end, left);
    return left;
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param comparator Comparator to use
   * @param quant Quantile to compute
   * @return Quantile position
   */
  public static int quantile(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator, double quant) {
    return quantile(data, comparator, 0, data.size(), quant);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * It will prefer the lower element.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param comparator Comparator to use
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive)
   * @param quant Quantile to compute
   * @return Quantile position
   */
  public static int quantile(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator, int begin, int end, double quant) {
    final int length = end - begin;
    assert (length > 0) : "Quantile on empty set?";
    // Integer division is "floor" since we are non-negative.
    final double dleft = begin + (length - 1) * quant;
    final int ileft = (int) Math.floor(dleft);

    quickSelect(data, comparator, begin, end, ileft);
    return ileft;
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * @param data Data to process
   * @param comparator Comparator to use
   * @param start Interval start
   * @param end Interval end (exclusive)
   * @param rank rank position we are interested in (starting at 0)
   */
  public static void quickSelect(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator, int start, int end, int rank) {
    DBIDArrayIter refi = data.iter(), refj = data.iter(), pivot = data.iter();
    while(true) {
      // Optimization for small arrays
      // This also ensures a minimum size below
      if(start + SMALL > end) {
        insertionSort(data, comparator, start, end, refi, refj);
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
      if(comparator.compare(refi.seek(m1), refj.seek(m2)) > 0) {
        data.swap(m1, m2);
      }
      if(comparator.compare(refi.seek(m1), refj.seek(m3)) > 0) {
        data.swap(m1, m3);
      }
      if(comparator.compare(refi.seek(m2), refj.seek(m3)) > 0) {
        data.swap(m2, m3);
      }
      if(comparator.compare(refi.seek(m4), refj.seek(m5)) > 0) {
        data.swap(m4, m5);
      }
      if(comparator.compare(refi.seek(m1), refj.seek(m4)) > 0) {
        data.swap(m1, m4);
      }
      if(comparator.compare(refi.seek(m3), refj.seek(m4)) > 0) {
        data.swap(m3, m4);
      }
      if(comparator.compare(refi.seek(m2), refj.seek(m5)) > 0) {
        data.swap(m2, m5);
      }
      if(comparator.compare(refi.seek(m2), refj.seek(m3)) > 0) {
        data.swap(m2, m3);
      }
      if(comparator.compare(refi.seek(m4), refj.seek(m5)) > 0) {
        data.swap(m4, m5);
      }

      int best = bestPivot(rank, m1, m2, m3, m4, m5);
      // Move middle element out of the way.
      data.swap(best, end - 1);
      pivot.seek(end - 1);

      // Begin partitioning
      int i = start, j = end - 2;
      // This is classic quicksort stuff
      while(true) {
        while(i <= j && comparator.compare(refi.seek(i), pivot) <= 0) {
          i++;
        }
        while(j >= i && comparator.compare(refj.seek(j), pivot) >= 0) {
          j--;
        }
        if(i >= j) {
          break;
        }
        data.swap(i, j);
      }

      // Move pivot (former middle element) back into the appropriate place
      data.swap(i, end - 1);

      // In contrast to quicksort, we only need to recurse into the half we are
      // interested in. Instead of recursion we now use iteration.

      pivot.seek(i); // Pivot has moved.
      // Skip duplicates to narrow down the search interval:
      while(rank < i && comparator.compare(refi.seek(i - 1), pivot) == 0) {
        --i;
      }
      while(rank > i && comparator.compare(refi.seek(i + 1), pivot) == 0) {
        ++i;
      }
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
  private static void insertionSort(ArrayModifiableDBIDs data, Comparator<? super DBIDRef> comparator, int start, int end, DBIDArrayIter iter1, DBIDArrayIter iter2) {
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start; j--) {
        if(comparator.compare(iter1.seek(j - 1), iter2.seek(j)) <= 0) {
          break;
        }
        data.swap(j, j - 1);
      }
    }
  }

  /**
   * QuickSelect is essentially quicksort, except that we only "sort" that half
   * of the array that we are interested in.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param rank Rank position that we are interested in (integer!)
   */
  public static void quickSelect(ModifiableDoubleDBIDList data, int rank) {
    quickSelect(data, 0, data.size(), rank);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @return Median position
   */
  public static int median(ModifiableDoubleDBIDList data) {
    return median(data, 0, data.size());
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * On an odd length, it will return the lower element.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive!)
   * @return Median position
   */
  public static int median(ModifiableDoubleDBIDList data, int begin, int end) {
    final int length = end - begin;
    assert (length > 0);
    // Integer division is "floor" since we are non-negative.
    final int left = begin + ((length - 1) >> 1);
    quickSelect(data, begin, end, left);
    return left;
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param quant Quantile to compute
   * @return Quantile position
   */
  public static int quantile(ModifiableDoubleDBIDList data, double quant) {
    return quantile(data, 0, data.size(), quant);
  }

  /**
   * Compute the median of an array efficiently using the QuickSelect method.
   *
   * It will prefer the lower element.
   *
   * Note: the array is <b>modified</b> by this.
   *
   * @param data Data to process
   * @param begin Begin of valid values
   * @param end End of valid values (exclusive)
   * @param quant Quantile to compute
   * @return Quantile position
   */
  public static int quantile(ModifiableDoubleDBIDList data, int begin, int end, double quant) {
    final int length = end - begin;
    assert (length > 0) : "Quantile on empty set?";
    // Integer division is "floor" since we are non-negative.
    final double dleft = begin + (length - 1) * quant;
    final int ileft = (int) Math.floor(dleft);

    quickSelect(data, begin, end, ileft);
    return ileft;
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
  public static void quickSelect(ModifiableDoubleDBIDList data, int start, int end, int rank) {
    DoubleDBIDListIter refi = data.iter(), refj = data.iter(),
        pivot = data.iter();
    while(true) {
      // Optimization for small arrays
      // This also ensures a minimum size below
      if(start + SMALL > end) {
        insertionSort(data, start, end, refi, refj);
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
      if(refi.seek(m1).doubleValue() > refj.seek(m2).doubleValue()) {
        data.swap(m1, m2);
      }
      if(refi.seek(m1).doubleValue() > refj.seek(m3).doubleValue()) {
        data.swap(m1, m3);
      }
      if(refi.seek(m2).doubleValue() > refj.seek(m3).doubleValue()) {
        data.swap(m2, m3);
      }
      if(refi.seek(m4).doubleValue() > refj.seek(m5).doubleValue()) {
        data.swap(m4, m5);
      }
      if(refi.seek(m1).doubleValue() > refj.seek(m4).doubleValue()) {
        data.swap(m1, m4);
      }
      if(refi.seek(m3).doubleValue() > refj.seek(m4).doubleValue()) {
        data.swap(m3, m4);
      }
      if(refi.seek(m2).doubleValue() > refj.seek(m5).doubleValue()) {
        data.swap(m2, m5);
      }
      if(refi.seek(m2).doubleValue() > refj.seek(m3).doubleValue()) {
        data.swap(m2, m3);
      }
      if(refi.seek(m4).doubleValue() > refj.seek(m5).doubleValue()) {
        data.swap(m4, m5);
      }

      int best = bestPivot(rank, m1, m2, m3, m4, m5);
      // Move middle element out of the way.
      data.swap(best, end - 1);
      final double pivotv = pivot.seek(end - 1).doubleValue();

      // Begin partitioning
      int i = start, j = end - 2;
      // This is classic quicksort stuff
      while(true) {
        while(i <= j && refi.seek(i).doubleValue() <= pivotv) {
          i++;
        }
        while(j >= i && refj.seek(j).doubleValue() >= pivotv) {
          j--;
        }
        if(i >= j) {
          break;
        }
        data.swap(i, j);
      }

      // Move pivot (former middle element) back into the appropriate place
      data.swap(i, end - 1);

      // In contrast to quicksort, we only need to recurse into the half we are
      // interested in. Instead of recursion we now use iteration.

      pivot.seek(i); // Pivot has moved.
      // Skip duplicates to narrow down the search interval:
      while(rank < i && refi.seek(i - 1).doubleValue() == pivotv) {
        --i;
      }
      while(rank > i && refi.seek(i + 1).doubleValue() == pivotv) {
        ++i;
      }
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
  private static void insertionSort(ModifiableDoubleDBIDList data, int start, int end, DoubleDBIDListIter iter1, DoubleDBIDListIter iter2) {
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start; j--) {
        if(iter1.seek(j - 1).doubleValue() < iter2.seek(j).doubleValue()) {
          break;
        }
        data.swap(j, j - 1);
      }
    }
  }
}
