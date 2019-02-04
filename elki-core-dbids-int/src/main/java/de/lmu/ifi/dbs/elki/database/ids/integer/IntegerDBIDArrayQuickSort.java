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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import java.util.Comparator;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Class to sort an integer DBID array, using a modified quicksort.
 * <p>
 * Two array iterators will be used to seek to the elements to compare, while
 * the backing storage is a plain integer array.
 * <p>
 * Reference:
 * <p>
 * V. Yaroslavskiy<br>
 * Dual-Pivot Quicksort
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @assoc - - - IntegerArrayDBIDs
 */
@Reference(authors = "V. Yaroslavskiy", //
    title = "Dual-Pivot Quicksort", booktitle = "", //
    url = "http://iaroslavski.narod.ru/quicksort/", //
    bibkey = "web/Yaroslavskiy09")
final class IntegerDBIDArrayQuickSort {
  /**
   * Private constructor. Static methods only.
   */
  private IntegerDBIDArrayQuickSort() {
    // Do not use.
  }

  /**
   * Threshold for using insertion sort. Value taken from Javas QuickSort,
   * assuming that it will be similar for DBIDs.
   */
  private static final int INSERTION_THRESHOLD = 47;

  /**
   * Sort the full array using the given comparator.
   * 
   * @param data Data to sort
   * @param comp Comparator
   */
  public static void sort(int[] data, Comparator<? super DBIDRef> comp) {
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
  public static void sort(int[] data, int start, int end, Comparator<? super DBIDRef> comp) {
    quickSort(data, start, end - 1, comp, new IntegerDBIDVar(), new IntegerDBIDVar(), new IntegerDBIDVar());
  }

  /**
   * Actual recursive sort function.
   * 
   * @param data Data to sort
   * @param start First index
   * @param end Last index (inclusive!)
   * @param comp Comparator
   * @param vl First seeking iterator
   * @param vk Second seeking iterator
   * @param vr Third seeking iterator
   */
  private static void quickSort(int[] data, final int start, final int end, Comparator<? super DBIDRef> comp, IntegerDBIDVar vl, IntegerDBIDVar vk, IntegerDBIDVar vr) {
    final int len = end - start;
    if(len < INSERTION_THRESHOLD) {
      // Classic insertion sort.
      for(int i = start + 1; i <= end; i++) {
        for(int j = i; j > start; j--) {
          vl.internalSetIndex(data[j]);
          vr.internalSetIndex(data[j - 1]);
          if(comp.compare(vl, vr) < 0) {
            int tmp = data[j - 1];
            data[j - 1] = data[j];
            data[j] = tmp;
          }
          else {
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
    if(compare(vl, data[m1], vk, data[m2], comp) > 0) {
      int tmp = data[m2];
      data[m2] = data[m1];
      data[m1] = tmp;
    }
    if(compare(vl, data[m1], vk, data[m3], comp) > 0) {
      int tmp = data[m3];
      data[m3] = data[m1];
      data[m1] = tmp;
    }
    if(compare(vl, data[m2], vk, data[m3], comp) > 0) {
      int tmp = data[m3];
      data[m3] = data[m2];
      data[m2] = tmp;
    }
    if(compare(vl, data[m4], vk, data[m5], comp) > 0) {
      int tmp = data[m5];
      data[m5] = data[m4];
      data[m4] = tmp;
    }
    if(compare(vl, data[m1], vk, data[m4], comp) > 0) {
      int tmp = data[m4];
      data[m4] = data[m1];
      data[m1] = tmp;
    }
    if(compare(vl, data[m3], vk, data[m4], comp) > 0) {
      int tmp = data[m4];
      data[m4] = data[m3];
      data[m3] = tmp;
    }
    if(compare(vl, data[m2], vk, data[m5], comp) > 0) {
      int tmp = data[m5];
      data[m5] = data[m2];
      data[m2] = tmp;
    }
    if(compare(vl, data[m2], vk, data[m3], comp) > 0) {
      int tmp = data[m3];
      data[m3] = data[m2];
      data[m2] = tmp;
    }
    if(compare(vl, data[m4], vk, data[m5], comp) > 0) {
      int tmp = data[m5];
      data[m5] = data[m4];
      data[m4] = tmp;
    }

    // Choose the 2 and 4th as pivots, as we want to get three parts
    // Copy to variables v1 and v3, replace them with the start and end
    // Note: do not modify v1 or v3 until we put them back!
    vl.internalSetIndex(data[m2]);
    vr.internalSetIndex(data[m4]);
    data[m2] = data[start];
    data[m4] = data[end];

    // A tie is when the two chosen pivots are the same
    final boolean tied = comp.compare(vl, vr) == 0;

    // Insertion points for pivot areas.
    int left = start + 1;
    int right = end - 1;

    // Note: we merged the ties and no ties cases.
    // This likely is marginally slower, but not at a macro level
    // And you never know with hotspot.
    for(int k = left; k <= right; k++) {
      int tmp = data[k];
      vk.internalSetIndex(tmp);
      final int c = comp.compare(vk, vl);
      if(c == 0) {
        continue;
      }
      else if(c < 0) {
        // Traditional quicksort
        data[k] = data[left];
        data[left] = tmp;
        left++;
      }
      else if(tied || comp.compare(vk, vr) > 0) {
        // Now look at the right. First skip correct entries there, too
        while(true) {
          vk.internalSetIndex(data[right]);
          if(comp.compare(vk, vr) > 0 && k < right) {
            right--;
          }
          else {
            break;
          }
        }
        // Now move tmp from k to the right.
        data[k] = data[right];
        data[right] = tmp;
        right--;
        // Test the element we just inserted: left or center?
        vk.internalSetIndex(data[k]);
        if(comp.compare(vk, vl) < 0) {
          tmp = data[k];
          data[k] = data[left];
          data[left] = tmp;
          left++;
        } // else: center. cannot be on right.
      }
    }
    // Put the pivot elements back in.
    // Remember: we must not modify v1 and v3 above.
    data[start] = data[left - 1];
    data[left - 1] = vl.internalGetIndex();
    data[end] = data[right + 1];
    data[right + 1] = vr.internalGetIndex();
    // v1 and v3 are now safe to modify again. Perform recursion:
    quickSort(data, start, left - 2, comp, vl, vk, vr);
    // Handle the middle part - if necessary:
    if(!tied) {
      // TODO: the original publication had a special tie handling here.
      // It shouldn't affect correctness, but probably improves situations
      // with a lot of tied elements.
      quickSort(data, left, right, comp, vl, vk, vr);
    }
    quickSort(data, right + 2, end, comp, vl, vk, vr);
  }

  /**
   * Compare two elements.
   * 
   * @param i1 First scratch variable
   * @param p1 Value for first
   * @param i2 Second scratch variable
   * @param p2 Value for second
   * @param comp Comparator
   * @return Comparison result
   */
  private static int compare(IntegerDBIDVar i1, int p1, IntegerDBIDVar i2, int p2, Comparator<? super DBIDRef> comp) {
    i1.internalSetIndex(p1);
    i2.internalSetIndex(p2);
    return comp.compare(i1, i2);
  }
}
