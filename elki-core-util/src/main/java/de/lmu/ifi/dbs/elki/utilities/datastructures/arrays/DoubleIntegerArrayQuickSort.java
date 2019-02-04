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
package de.lmu.ifi.dbs.elki.utilities.datastructures.arrays;

/**
 * Class to sort a double and an integer DBID array, using a quicksort with a
 * best of 5 heuristic.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public final class DoubleIntegerArrayQuickSort {
  /**
   * Private constructor. Static methods only.
   */
  private DoubleIntegerArrayQuickSort() {
    // Do not use.
  }

  /**
   * Threshold for using insertion sort.
   */
  private static final int INSERTION_THRESHOLD = 22;

  /**
   * Sort the full array using the given comparator.
   * 
   * @param keys Keys for sorting
   * @param values Values for sorting
   * @param len Length to sort.
   */
  public static void sort(double[] keys, int[] values, int len) {
    sort(keys, values, 0, len);
  }

  /**
   * Sort the array using the given comparator.
   * 
   * @param keys Keys for sorting
   * @param values Values for sorting
   * @param start First index
   * @param end Last index (exclusive)
   */
  public static void sort(double[] keys, int[] values, int start, int end) {
    quickSort(keys, values, start, end);
  }

  /**
   * Actual recursive sort function.
   * 
   * @param keys Keys for sorting
   * @param vals Values for sorting
   * @param start First index
   * @param end Last index (exclusive!)
   */
  private static void quickSort(double[] keys, int[] vals, final int start, final int end) {
    final int len = end - start;
    if(len < INSERTION_THRESHOLD) {
      insertionSort(keys, vals, start, end);
      return;
    }
    final int last = end - 1;

    // Choose pivots by looking at five candidates.
    final int seventh = (len >> 3) + (len >> 6) + 1;
    final int m3 = (start + end) >> 1; // middle
    final int m2 = m3 - seventh;
    final int m1 = m2 - seventh;
    final int m4 = m3 + seventh;
    final int m5 = m4 + seventh;

    // Mixture of insertion and merge sort:
    sort5(keys, vals, m1, m2, m3, m4, m5);

    // Move pivot to the front.
    double pivotkey = keys[m3];
    int pivotval = vals[m3];
    keys[m3] = keys[start];
    vals[m3] = vals[start];

    // The interval to pivotize
    int left = start + 1; // Without pivot
    int right = last; // inclusive

    // This is the classic QuickSort loop:
    while(true) {
      // Move duplicates to right partition, i.e. < here, <= below.
      while(left <= right && keys[left] < pivotkey) {
        left++;
      }
      while(left <= right && pivotkey <= keys[right]) {
        right--;
      }
      if(right <= left) {
        break;
      }
      swap(keys, vals, left, right);
      left++;
      right--;
    }
    // right now points to the last element smaller than the pivot.
    // Move pivot back into the appropriate place
    keys[start] = keys[right];
    vals[start] = vals[right];
    keys[right] = pivotkey;
    vals[right] = pivotval;

    // Recursion when more than one element only:
    if(start + 1 < right) {
      quickSort(keys, vals, start, right);
    }
    int rstart = right + 1;
    // Avoid recursing on duplicates of the pivot:
    while(rstart < last && keys[rstart] <= keys[right]) {
      rstart++;
    }
    // Recurse when _more_ than 1 element only
    if(rstart < last) {
      quickSort(keys, vals, rstart, end);
    }
  }

  /**
   * An explicit sort, for the five pivot candidates.
   * 
   * Note that this <em>must</em> only be used with
   * {@code m1 < m2 < m3 < m4 < m5}.
   * 
   * @param keys Keys
   * @param vals Values
   * @param m1 Pivot candidate position
   * @param m2 Pivot candidate position
   * @param m3 Pivot candidate position
   * @param m4 Pivot candidate position
   * @param m5 Pivot candidate position
   */
  private static void sort5(double[] keys, int[] vals, final int m1, final int m2, final int m3, final int m4, final int m5) {
    if(keys[m1] > keys[m2]) {
      swap(keys, vals, m1, m2);
    }
    if(keys[m3] > keys[m4]) {
      swap(keys, vals, m3, m4);
    }
    // Merge 1+2 and 3+4
    if(keys[m2] > keys[m4]) {
      swap(keys, vals, m2, m4);
    }
    if(keys[m1] > keys[m3]) {
      swap(keys, vals, m1, m3);
    }
    if(keys[m2] > keys[m3]) {
      swap(keys, vals, m2, m3);
    }
    // Insertion sort m5:
    if(keys[m4] > keys[m5]) {
      swap(keys, vals, m4, m5);
      if(keys[m3] > keys[m4]) {
        swap(keys, vals, m3, m4);
        if(keys[m2] > keys[m3]) {
          swap(keys, vals, m2, m3);
          if(keys[m1] > keys[m1]) {
            swap(keys, vals, m1, m2);
          }
        }
      }
    }
  }

  /**
   * Sort via insertion sort.
   * 
   * @param keys Keys
   * @param vals Values
   * @param start Interval start
   * @param end Interval end
   */
  private static void insertionSort(double[] keys, int[] vals, final int start, final int end) {
    // Classic insertion sort.
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start; j--) {
        if(keys[j] >= keys[j - 1]) {
          break;
        }
        swap(keys, vals, j, j - 1);
      }
    }
  }

  /**
   * Sort the full array using the given comparator.
   * 
   * @param keys Keys for sorting
   * @param values Values for sorting
   * @param len Length to sort.
   */
  public static void sortReverse(double[] keys, int[] values, int len) {
    sortReverse(keys, values, 0, len);
  }

  /**
   * Sort the array using the given comparator.
   * 
   * @param keys Keys for sorting
   * @param values Values for sorting
   * @param start First index
   * @param end Last index (exclusive)
   */
  public static void sortReverse(double[] keys, int[] values, int start, int end) {
    quickSortReverse(keys, values, start, end);
  }

  /**
   * Actual recursive sort function.
   * 
   * @param keys Keys for sorting
   * @param vals Values for sorting
   * @param start First index
   * @param end Last index (exclusive!)
   */
  private static void quickSortReverse(double[] keys, int[] vals, final int start, final int end) {
    final int len = end - start;
    if(len < INSERTION_THRESHOLD) {
      insertionSortReverse(keys, vals, start, end);
      return;
    }
    final int last = end - 1;

    // Choose pivots by looking at five candidates.
    final int seventh = (len >> 3) + (len >> 6) + 1;
    final int m3 = (start + end) >> 1; // middle
    final int m2 = m3 - seventh;
    final int m1 = m2 - seventh;
    final int m4 = m3 + seventh;
    final int m5 = m4 + seventh;

    // Mixture of insertion and merge sort:
    sortReverse5(keys, vals, m1, m2, m3, m4, m5);

    // Move pivot to the front.
    double pivotkey = keys[m3];
    int pivotval = vals[m3];
    keys[m3] = keys[start];
    vals[m3] = vals[start];

    // The interval to pivotize
    int left = start + 1; // Without pivot
    int right = last; // inclusive

    // This is the classic QuickSort loop:
    while(true) {
      // Move duplicates to right partition, i.e. < here, <= below.
      while(left <= right && keys[left] > pivotkey) {
        left++;
      }
      while(left <= right && pivotkey >= keys[right]) {
        right--;
      }
      if(right <= left) {
        break;
      }
      swap(keys, vals, left, right);
      left++;
      right--;
    }
    // right now points to the last element smaller than the pivot.
    // Move pivot back into the appropriate place
    keys[start] = keys[right];
    vals[start] = vals[right];
    keys[right] = pivotkey;
    vals[right] = pivotval;

    // Recursion when more than one element only:
    if(start + 1 < right) {
      quickSortReverse(keys, vals, start, right);
    }
    int rstart = right + 1;
    // Avoid recursing on duplicates of the pivot:
    while(rstart < last && keys[rstart] >= keys[right]) {
      rstart++;
    }
    // Recurse when _more_ than 1 element only
    if(rstart < last) {
      quickSortReverse(keys, vals, rstart, end);
    }
  }

  /**
   * An explicit sort, for the five pivot candidates.
   * 
   * Note that this <em>must</em> only be used with
   * {@code m1 < m2 < m3 < m4 < m5}.
   * 
   * @param keys Keys
   * @param vals Values
   * @param m1 Pivot candidate position
   * @param m2 Pivot candidate position
   * @param m3 Pivot candidate position
   * @param m4 Pivot candidate position
   * @param m5 Pivot candidate position
   */
  private static void sortReverse5(double[] keys, int[] vals, final int m1, final int m2, final int m3, final int m4, final int m5) {
    if(keys[m1] < keys[m2]) {
      swap(keys, vals, m1, m2);
    }
    if(keys[m3] < keys[m4]) {
      swap(keys, vals, m3, m4);
    }
    // Merge 1+2 and 3+4
    if(keys[m2] < keys[m4]) {
      swap(keys, vals, m2, m4);
    }
    if(keys[m1] < keys[m3]) {
      swap(keys, vals, m1, m3);
    }
    if(keys[m2] < keys[m3]) {
      swap(keys, vals, m2, m3);
    }
    // Insertion sort m5:
    if(keys[m4] < keys[m5]) {
      swap(keys, vals, m4, m5);
      if(keys[m3] < keys[m4]) {
        swap(keys, vals, m3, m4);
        if(keys[m2] < keys[m3]) {
          swap(keys, vals, m2, m3);
          if(keys[m1] < keys[m1]) {
            swap(keys, vals, m1, m2);
          }
        }
      }
    }
  }

  /**
   * Sort via insertion sort.
   * 
   * @param keys Keys
   * @param vals Values
   * @param start Interval start
   * @param end Interval end
   */
  private static void insertionSortReverse(double[] keys, int[] vals, final int start, final int end) {
    // Classic insertion sort.
    for(int i = start + 1; i < end; i++) {
      for(int j = i; j > start; j--) {
        if(keys[j] <= keys[j - 1]) {
          break;
        }
        swap(keys, vals, j, j - 1);
      }
    }
  }

  /**
   * Swap two entries.
   * 
   * @param keys Keys
   * @param vals Values
   * @param j First index
   * @param i Second index
   */
  private static void swap(double[] keys, int[] vals, int j, int i) {
    double td = keys[j];
    keys[j] = keys[i];
    keys[i] = td;
    int ti = vals[j];
    vals[j] = vals[i];
    vals[i] = ti;
  }
}
