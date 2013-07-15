package de.lmu.ifi.dbs.elki.database.ids.integer;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Class to sort a double and an integer DBID array, using a quicksort with a
 * best of 5 heuristic.
 * 
 * @author Erich Schubert
 */
class DoubleIntegerArrayQuickSort {
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
    if (len < INSERTION_THRESHOLD) {
      // Classic insertion sort.
      for (int i = start + 1; i < end; i++) {
        for (int j = i; j > start; j--) {
          if (keys[j] < keys[j - 1]) {
            swap(keys, vals, j, j - 1);
          } else {
            break;
          }
        }
      }
      return;
    }

    // Choose pivots by looking at five candidates.
    final int seventh = (len >> 3) + (len >> 6) + 1;
    final int m3 = (start + end) >> 1; // middle
    final int m2 = m3 - seventh;
    final int m1 = m2 - seventh;
    final int m4 = m3 + seventh;
    final int m5 = m4 + seventh;

    // Mixture of insertion and merge sort:
    if (keys[m1] > keys[m2]) {
      swap(keys, vals, m1, m2);
    }
    if (keys[m3] > keys[m4]) {
      swap(keys, vals, m3, m4);
    }
    // Merge 1+2 and 3+4
    if (keys[m2] > keys[m4]) {
      swap(keys, vals, m2, m4);
    }
    if (keys[m1] > keys[m3]) {
      swap(keys, vals, m1, m3);
    }
    if (keys[m2] > keys[m3]) {
      swap(keys, vals, m2, m3);
    }
    // Insertion sort m5:
    if (keys[m4] > keys[m5]) {
      swap(keys, vals, m4, m5);
      if (keys[m3] > keys[m4]) {
        swap(keys, vals, m3, m4);
        if (keys[m2] > keys[m3]) {
          swap(keys, vals, m2, m3);
          if (keys[m1] > keys[m1]) {
            swap(keys, vals, m1, m2);
          }
        }
      }
    }

    // Move pivot to the front.
    double pivotkey = keys[m3];
    int pivotval = vals[m3];
    keys[m3] = keys[start];
    vals[m3] = vals[start];

    // The interval to pivotize
    int left = start + 1;
    int right = end - 1;

    // This is the classic QuickSort loop:
    while (true) {
      while (left <= right && keys[left] <= pivotkey) {
        left++;
      }
      while (left <= right && pivotkey <= keys[right]) {
        right--;
      }
      if (right <= left) {
        break;
      }
      swap(keys, vals, left, right);
      left++;
      right--;
    }

    // Move pivot back into the appropriate place
    keys[start] = keys[right];
    vals[start] = vals[right];
    keys[right] = pivotkey;
    vals[right] = pivotval;

    // Recursion:
    if (start + 1 < right) {
      quickSort(keys, vals, start, right);
    }
    if (right + 2 < end) {
      quickSort(keys, vals, right + 1, end);
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
