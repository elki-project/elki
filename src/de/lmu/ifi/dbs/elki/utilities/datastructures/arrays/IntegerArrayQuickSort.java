package de.lmu.ifi.dbs.elki.utilities.datastructures.arrays;

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
 * Class to sort an int array, using a modified quicksort.
 * 
 * The implementation is closely based on:
 * <p>
 * Dual-Pivot Quicksort<br />
 * Vladimir Yaroslavskiy
 * </p>
 *
 * and differs mostly in that we sort different kinds of arrays,
 * and allow the use of comparators - useful in particular when
 * the array references external objects.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses IntegerComparator
 */
@Reference(authors = "Vladimir Yaroslavskiy", title = "Dual-Pivot Quicksort", booktitle = "http://iaroslavski.narod.ru/quicksort/", url = "http://iaroslavski.narod.ru/quicksort/")
public class IntegerArrayQuickSort {
  /**
   * Threshold for using insertion sort. Value taken from Javas QuickSort,
   * assuming that it will be similar for our data sets.
   */
  private static final int INSERTION_THRESHOLD = 47;

  /**
   * Sort the full array using the given comparator.
   * 
   * @param data Data to sort
   * @param comp Comparator
   */
  public static void sort(int[] data, IntegerComparator comp) {
    sort(data, 0, data.length, comp);
  }

  /**
   * Sort the array using the given comparator.
   * 
   * @param data Data to sort
   * @param start First index
   * @param end Last index (exclusive)
   * @param comp Comparator
   */
  public static void sort(int[] data, int start, int end, IntegerComparator comp) {
    quickSort(data, start, end - 1, comp);
  }

  /**
   * Actual recursive sort function.
   * 
   * @param data Data to sort
   * @param start First index
   * @param end Last index (inclusive!)
   * @param comp Comparator
   */
  private static void quickSort(int[] data, final int start, final int end, IntegerComparator comp) {
    final int len = end - start;
    if (len < INSERTION_THRESHOLD) {
      // Classic insertion sort.
      for (int i = start + 1; i <= end; i++) {
        for (int j = i; j > start; j--) {
          if (comp.compare(data[j], data[j - 1]) < 0) {
            final int tmp = data[j - 1];
            data[j - 1] = data[j];
            data[j] = tmp;
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
    if (comp.compare(data[m1], data[m2]) > 0) {
      final int tmp = data[m2];
      data[m2] = data[m1];
      data[m1] = tmp;
    }
    if (comp.compare(data[m1], data[m3]) > 0) {
      final int tmp = data[m3];
      data[m3] = data[m1];
      data[m1] = tmp;
    }
    if (comp.compare(data[m2], data[m3]) > 0) {
      final int tmp = data[m3];
      data[m3] = data[m2];
      data[m2] = tmp;
    }
    if (comp.compare(data[m4], data[m5]) > 0) {
      final int tmp = data[m5];
      data[m5] = data[m4];
      data[m4] = tmp;
    }
    if (comp.compare(data[m1], data[m4]) > 0) {
      final int tmp = data[m4];
      data[m4] = data[m1];
      data[m1] = tmp;
    }
    if (comp.compare(data[m3], data[m4]) > 0) {
      final int tmp = data[m4];
      data[m4] = data[m3];
      data[m3] = tmp;
    }
    if (comp.compare(data[m2], data[m5]) > 0) {
      final int tmp = data[m5];
      data[m5] = data[m2];
      data[m2] = tmp;
    }
    if (comp.compare(data[m2], data[m3]) > 0) {
      final int tmp = data[m3];
      data[m3] = data[m2];
      data[m2] = tmp;
    }
    if (comp.compare(data[m4], data[m5]) > 0) {
      final int tmp = data[m5];
      data[m5] = data[m4];
      data[m4] = tmp;
    }

    // Choose the 2 and 4th as pivots, as we want to get three parts
    // Copy to variables v1 and v3, replace them with the start and end
    // Note: do not modify v1 or v3 until we put them back!
    final int lpivot = data[m2];
    final int rpivot = data[m4];
    data[m2] = data[start];
    data[m4] = data[end];

    // A tie is when the two chosen pivots are the same
    final boolean tied = comp.compare(lpivot, rpivot) == 0;

    // Insertion points for pivot areas.
    int left = start + 1;
    int right = end - 1;

    // Note: we merged the ties and no ties cases.
    // This likely is marginally slower, but not at a macro level
    // And you never know with hotspot.
    for (int k = left; k <= right; k++) {
      final int tmp = data[k];
      final int c = comp.compare(tmp, lpivot);
      if (c == 0) {
        continue;
      } else if (c < 0) {
        // Traditional quicksort
        data[k] = data[left];
        data[left] = tmp;
        left++;
      } else if (tied || comp.compare(tmp, rpivot) > 0) {
        // Now look at the right. First skip correct entries there, too
        while (true) {
          final int tmp2 = data[right];
          if (comp.compare(tmp2, rpivot) > 0 && k < right) {
            right--;
          } else {
            break;
          }
        }
        // Now move tmp from k to the right.
        data[k] = data[right];
        data[right] = tmp;
        right--;
        // Test the element we just inserted: left or center?
        if (comp.compare(data[k], lpivot) < 0) {
          final int tmp2 = data[k];
          data[k] = data[left];
          data[left] = tmp2;
          left++;
        } // else: center. cannot be on right.
      }
    }
    // Put the pivot elements back in.
    // Remember: we must not modify v1 and v3 above.
    data[start] = data[left - 1];
    data[left - 1] = lpivot;
    data[end] = data[right + 1];
    data[right + 1] = rpivot;
    // v1 and v3 are now safe to modify again. Perform recursion:
    quickSort(data, start, left - 2, comp);
    // Handle the middle part - if necessary:
    if (!tied) {
      // TODO: the original publication had a special tie handling here.
      // It shouldn't affect correctness, but probably improves situations
      // with a lot of tied elements.
      quickSort(data, left, right, comp);
    }
    quickSort(data, right + 2, end, comp);
  }
}
