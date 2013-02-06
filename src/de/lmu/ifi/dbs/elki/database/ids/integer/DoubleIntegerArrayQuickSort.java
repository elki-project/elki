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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Class to sort a double and an integer DBID array, using a modified quicksort.
 * 
 * The implementation is closely based on:
 * <p>
 * Dual-Pivot Quicksort<br />
 * Vladimir Yaroslavskiy
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "Vladimir Yaroslavskiy", title = "Dual-Pivot Quicksort", booktitle = "http://iaroslavski.narod.ru/quicksort/", url = "http://iaroslavski.narod.ru/quicksort/")
class DoubleIntegerArrayQuickSort {
  /**
   * Threshold for using insertion sort. Value taken from Javas QuickSort,
   * assuming that it will be similar for DBIDs.
   */
  private static final int INSERTION_THRESHOLD = 47;

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
    quickSort(keys, values, start, end - 1);
  }

  /**
   * Actual recursive sort function.
   * 
   * @param keys Keys for sorting
   * @param vals Values for sorting
   * @param start First index
   * @param end Last index (inclusive!)
   */
  private static void quickSort(double[] keys, int[] vals, final int start, final int end) {
    final int len = end - start;
    if (len < INSERTION_THRESHOLD) {
      // Classic insertion sort.
      for (int i = start + 1; i <= end; i++) {
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

    // Explicit (and optimal) sorting network for 5 elements
    // See Knuth for details.
    if (keys[m1] > keys[m2]) {
      swap(keys, vals, m1, m2);
    }
    if (keys[m1] > keys[m3]) {
      swap(keys, vals, m1, m3);
    }
    if (keys[m2] > keys[m3]) {
      swap(keys, vals, m2, m3);
    }
    if (keys[m4] > keys[m5]) {
      swap(keys, vals, m4, m5);
    }
    if (keys[m1] > keys[m4]) {
      swap(keys, vals, m1, m4);
    }
    if (keys[m3] > keys[m4]) {
      swap(keys, vals, m3, m4);
    }
    if (keys[m2] > keys[m5]) {
      swap(keys, vals, m2, m5);
    }
    if (keys[m2] > keys[m3]) {
      swap(keys, vals, m2, m3);
    }
    if (keys[m4] > keys[m5]) {
      swap(keys, vals, m4, m5);
    }

    // Choose the 2 and 4th as pivots, as we want to get three parts
    // Copy to variables v1 and v3, replace them with the start and end
    // Note: do not modify v1 or v3 until we put them back!
    double keyl = keys[m2];
    int vall = vals[m2];
    double keyr = keys[m4];
    int valr = vals[m4];
    keys[m2] = keys[start];
    vals[m2] = vals[start];
    keys[m4] = keys[end];
    vals[m4] = vals[end];

    // A tie is when the two chosen pivots are the same
    final boolean tied = Double.compare(keyl, keyr) == 0;

    // Insertion points for pivot areas.
    int left = start + 1;
    int right = end - 1;

    // Note: we merged the ties and no ties cases.
    // This likely is marginally slower, but not at a macro level
    // And you never know with hotspot.
    for (int k = left; k <= right; k++) {
      double keyt = keys[k];
      int valt = vals[k];
      final int c = Double.compare(keyt, keyl);
      if (c == 0) {
        continue;
      } else if (c < 0) {
        // Traditional quicksort
        keys[k] = keys[left];
        vals[k] = vals[left];
        keys[left] = keyt;
        vals[left] = valt;
        left++;
      } else if (tied || keyt > keyr) {
        // Now look at the right. First skip correct entries there, too
        while (true) {
          if (keys[right] > keyr && k < right) {
            right--;
          } else {
            break;
          }
        }
        // Now move tmp from k to the right.
        keys[k] = keys[right];
        vals[k] = vals[right];
        keys[right] = keyt;
        vals[right] = valt;
        right--;
        // Test the element we just inserted: left or center?
        if (keys[k] < keyl) {
          swap(keys, vals, k, left);
          left++;
        } // else: center. cannot be on right.
      }
    }
    // Put the pivot elements back in.
    // Remember: we must not modify v1 and v3 above.
    keys[start] = keys[left - 1];
    vals[start] = vals[left - 1];
    keys[left - 1] = keyl;
    vals[left - 1] = vall;
    keys[end] = keys[right + 1];
    vals[end] = vals[right + 1];
    keys[right + 1] = keyr;
    vals[right + 1] = valr;
    // v1 and v3 are now safe to modify again. Perform recursion:
    quickSort(keys, vals, start, left - 2);
    // Handle the middle part - if necessary:
    if (!tied) {
      // TODO: the original publication had a special tie handling here.
      // It shouldn't affect correctness, but probably improves situations
      // with a lot of tied elements.
      quickSort(keys, vals, left, right);
    }
    quickSort(keys, vals, right + 2, end);
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
