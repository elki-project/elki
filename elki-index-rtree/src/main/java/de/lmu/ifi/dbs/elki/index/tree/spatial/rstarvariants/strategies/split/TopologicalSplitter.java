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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Encapsulates the required parameters for a topological split of a R*-Tree.
 * <p>
 * Reference:
 * <p>
 * Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger<br>
 * The R*-tree: an efficient and robust access method for points and
 * rectangles<br>
 * Proc. 1990 ACM SIGMOD Int. Conf. Management of Data
 *
 * @author Elke Achtert
 * @since 0.4.0
 * 
 * @has - - - Split
 */
@Reference(authors = "Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger", //
    title = "The R*-tree: an efficient and robust access method for points and rectangles", //
    booktitle = "Proc. 1990 ACM SIGMOD Int. Conf. Management of Data", //
    url = "https://doi.org/10.1145/93597.98741", //
    bibkey = "DBLP:conf/sigmod/BeckmannKSS90")
@Alias("de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.TopologicalSplitter")
public class TopologicalSplitter implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final TopologicalSplitter STATIC = new TopologicalSplitter();

  /**
   * Constructor.
   */
  public TopologicalSplitter() {
    // Nothing to do.
  }

  @Override
  public <E extends SpatialComparable, A> long[] split(A entries, ArrayAdapter<E, A> getter, int minEntries) {
    Split<A, E> split = new Split<>(entries, getter);
    split.chooseSplitAxis(minEntries);
    split.chooseSplitPoint(minEntries);

    assert (split.splitPoint < split.size) : "Invalid split produced. Size: " + getter.size(entries) + " minEntries: " + minEntries + " split.size: " + split.size;
    long[] assignment = BitsUtil.zero(split.size);
    for(int i = split.splitPoint; i < split.size; i++) {
      BitsUtil.setI(assignment, split.bestSorting[i].second);
    }
    return assignment;
  }

  /**
   * Internal data for an actual split.
   * 
   * @author Erich Schubert
   * 
   * @param <E> Actual entry type
   */
  private class Split<A, E extends SpatialComparable> {
    /**
     * The index of the split point.
     */
    int splitPoint = -1;

    /**
     * Indicates whether the sorting according to maximal or to minimal value
     * has been used for choosing the split axis and split point.
     */
    DoubleIntPair[] bestSorting;

    /**
     * The entries sorted according to their max values of their MBRs.
     */
    DoubleIntPair[] maxSorting;

    /**
     * The entries sorted according to their min values of their MBRs.
     */
    DoubleIntPair[] minSorting;

    /**
     * The entries we process.
     */
    private A entries;

    /**
     * The getter class for the entries.
     */
    private ArrayAdapter<E, A> getter;

    /**
     * List size.
     */
    private int size;

    /**
     * Dimensionality.
     */
    private int dimensionality;

    /**
     * Constructor.
     * 
     * @param entries Entires to split
     * @param getter Array adapter for entries
     */
    public Split(A entries, ArrayAdapter<E, A> getter) {
      this.entries = entries;
      this.getter = getter;
      this.size = getter.size(entries);
      this.dimensionality = getter.get(entries, 0).getDimensionality();
      initMinMaxArrays();
    }

    /**
     * Chooses a split axis.
     * 
     * @param minEntries number of minimum entries in the node to be split
     */
    void chooseSplitAxis(int minEntries) {
      // best value for the surface
      double minSurface = Double.MAX_VALUE;
      int splitAxis = -1;

      for(int d = 0; d < dimensionality; d++) {
        double sumOfAllMargins = 0;
        fillAndSort(d);

        // Note: this has a somewhat surprising evaluation order.
        // We compute the sum as in the original paper:
        // it says "sum of all margin-values".
        // Except that we don't match them as you would do in a split, but
        // Iterate over all possible splits from both sides (as well as min and
        // max) in parallel, since union can be computed incrementally.
        ModifiableHyperBoundingBox mbr_min_left = new ModifiableHyperBoundingBox(get(minSorting[0]));
        ModifiableHyperBoundingBox mbr_min_right = new ModifiableHyperBoundingBox(get(minSorting[size - 1]));
        ModifiableHyperBoundingBox mbr_max_left = new ModifiableHyperBoundingBox(get(maxSorting[0]));
        ModifiableHyperBoundingBox mbr_max_right = new ModifiableHyperBoundingBox(get(maxSorting[size - 1]));

        for(int k = 1; k < size - minEntries; k++) {
          mbr_min_left.extend(get(minSorting[k]));
          mbr_min_right.extend(get(minSorting[size - 1 - k]));
          mbr_max_left.extend(get(maxSorting[k]));
          mbr_max_right.extend(get(maxSorting[size - 1 - k]));
          if(k >= minEntries - 1) {
            // Yes, build the sum. This value is solely used for finding the
            // preferred split axis!
            // Note that mbr_min_left and mbr_max_left do not add up to a
            // complete split, but when the sum is complete, it will also
            // include their proper counterpart.
            sumOfAllMargins += SpatialUtil.perimeter(mbr_min_left) + SpatialUtil.perimeter(mbr_min_right) + SpatialUtil.perimeter(mbr_max_left) + SpatialUtil.perimeter(mbr_max_right);
          }
        }
        if(sumOfAllMargins < minSurface) {
          splitAxis = d;
          minSurface = sumOfAllMargins;
        }
      }
      if(splitAxis != dimensionality) {
        fillAndSort(splitAxis);
      }
    }

    /**
     * Init the arrays we use.
     */
    protected void initMinMaxArrays() {
      maxSorting = new DoubleIntPair[size];
      minSorting = new DoubleIntPair[size];
      // Prefill
      for(int j = 0; j < size; j++) {
        minSorting[j] = new DoubleIntPair(0, -1);
        maxSorting[j] = new DoubleIntPair(0, -1);
      }
    }

    /**
     * Fill the array with the dimension projection needed for sorting.
     * 
     * @param dim Relevant dimension.
     */
    protected void fillAndSort(final int dim) {
      // sort the entries according to their minimal and according to their
      // maximal value in the current dimension.
      for(int j = 0; j < size; j++) {
        E e = get(j);
        minSorting[j].first = e.getMin(dim);
        minSorting[j].second = j;
        maxSorting[j].first = e.getMax(dim);
        maxSorting[j].second = j;
      }
      Arrays.sort(minSorting);
      Arrays.sort(maxSorting);
    }

    /**
     * Chooses a split axis.
     * 
     * @param minEntries number of minimum entries in the node to be split
     */
    void chooseSplitPoint(int minEntries) {
      // the split point (first set to minimum entries in the node)
      splitPoint = size;
      // best value for the overlap
      double minOverlap = Double.POSITIVE_INFINITY;
      // the volume of mbr1 and mbr2
      double volume = Double.POSITIVE_INFINITY;
      // indicates whether the sorting according to maximal or to minimal value
      // is best for the split axis
      bestSorting = null;

      assert (size - 2 * minEntries >= 0) : "Cannot split nodes (" + size + " < 2*" + minEntries + ")";
      // test the sorting with respect to the minimal values
      {
        ModifiableHyperBoundingBox mbr1 = mbr(minSorting, 0, minEntries - 1);
        for(int i = 0; i <= size - 2 * minEntries; i++) {
          mbr1.extend(getter.get(entries, minSorting[minEntries + i - 1].second));
          HyperBoundingBox mbr2 = mbr(minSorting, minEntries + i, size);
          double currentOverlap = SpatialUtil.relativeOverlap(mbr1, mbr2);
          if(currentOverlap <= minOverlap) {
            double vol = SpatialUtil.volume(mbr1) + SpatialUtil.volume(mbr2);
            if(currentOverlap < minOverlap || vol < volume) {
              minOverlap = currentOverlap;
              volume = vol;
              splitPoint = minEntries + i;
              bestSorting = minSorting;
            }
          }
        }
      }
      // test the sorting with respect to the maximal values
      {
        ModifiableHyperBoundingBox mbr1 = mbr(maxSorting, 0, minEntries - 1);
        for(int i = 0; i <= size - 2 * minEntries; i++) {
          mbr1.extend(getter.get(entries, maxSorting[minEntries + i - 1].second));
          HyperBoundingBox mbr2 = mbr(maxSorting, minEntries + i, size);
          double currentOverlap = SpatialUtil.relativeOverlap(mbr1, mbr2);
          if(currentOverlap <= minOverlap) {
            double vol = SpatialUtil.volume(mbr1) + SpatialUtil.volume(mbr2);
            if(currentOverlap < minOverlap || vol < volume) {
              minOverlap = currentOverlap;
              volume = vol;
              splitPoint = minEntries + i;
              bestSorting = maxSorting;
            }
          }
        }
      }
      assert (splitPoint < size) : "No split found? Volume outside of double precision?";
    }

    /**
     * Get an entry.
     * 
     * @param off Offset
     * @return Entry
     */
    private E get(int off) {
      return getter.get(entries, off);
    }

    /**
     * Get an entry.
     * 
     * @param pair Entry pair
     * @return Entry
     */
    private E get(DoubleIntPair pair) {
      return getter.get(entries, pair.second);
    }

    /**
     * Computes and returns the mbr of the specified nodes, only the nodes
     * between from and to index are considered.
     * 
     * @param sorting the array of nodes
     * @param from the start index
     * @param to the end index
     * @return the mbr of the specified nodes
     */
    private ModifiableHyperBoundingBox mbr(final DoubleIntPair[] sorting, final int from, final int to) {
      ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(get(sorting[from]));
      for(int i = from + 1; i < to; i++) {
        mbr.extend(get(sorting[i]));
      }
      return mbr;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected TopologicalSplitter makeInstance() {
      return TopologicalSplitter.STATIC;
    }
  }
}
