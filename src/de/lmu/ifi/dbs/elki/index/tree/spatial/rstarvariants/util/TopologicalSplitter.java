package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.Arrays;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialAdapter;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Encapsulates the required parameters for a topological split of a R*-Tree.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Split
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class TopologicalSplitter implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final TopologicalSplitter STATIC = new TopologicalSplitter();

  /**
   * constructor.
   */
  public TopologicalSplitter() {
    // Nothing to do.
  }

  @Override
  public <E, A> BitSet split(A entries, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> adapter, int minEntries) {
    Split<A, E> split = new Split<A, E>(entries, getter, adapter);
    split.chooseSplitAxis(minEntries);
    split.chooseSplitPoint(minEntries);
    int splitpoint = split.getSplitPoint();
    DoubleIntPair[] sorted = split.getBestSorting();

    BitSet assignment = new BitSet(split.size);
    for(int i = splitpoint; i < split.size; i++) {
      assignment.set(sorted[i].second);
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
  private class Split<A, E> {
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
     * The getter class for the entries
     */
    private ArrayAdapter<E, A> getter;

    /**
     * List size
     */
    private int size;

    /**
     * Dimensionality
     */
    private int dimensionality;

    /**
     * Spatial adapter
     */
    private SpatialAdapter<? super E> adapter;

    /**
     * Constructor.
     */
    public Split(A entries, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> adapter) {
      this.entries = entries;
      this.getter = getter;
      this.adapter = adapter;
      this.size = getter.size(entries);
      this.dimensionality = adapter.getDimensionality(getter.get(entries, 0));
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
        ModifiableHyperBoundingBox mbr_min_left = SpatialUtil.copyMBR(get(minSorting[0]), adapter);
        ModifiableHyperBoundingBox mbr_min_right = SpatialUtil.copyMBR(get(minSorting[size - 1]), adapter);
        ModifiableHyperBoundingBox mbr_max_left = SpatialUtil.copyMBR(get(maxSorting[0]), adapter);
        ModifiableHyperBoundingBox mbr_max_right = SpatialUtil.copyMBR(get(maxSorting[size - 1]), adapter);

        for(int k = 1; k < size - minEntries; k++) {
          mbr_min_left.extend(get(minSorting[k]), adapter);
          mbr_min_right.extend(get(minSorting[size - 1 - k]), adapter);
          mbr_max_left.extend(get(maxSorting[k]), adapter);
          mbr_max_right.extend(get(maxSorting[size - 1 - k]), adapter);
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
      if(splitAxis != dimensionality - 1) {
        fillAndSort(splitAxis);
      }
    }

    /**
     * Init the arrays we use
     */
    protected void initMinMaxArrays() {
      maxSorting = new DoubleIntPair[size];
      minSorting = new DoubleIntPair[size];
      for(int j = 0; j < size; j++) {
        minSorting[j] = new DoubleIntPair(0, -1); // invalid, but allocate the objects
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
        minSorting[j].first = adapter.getMin(e, dim);
        minSorting[j].second = j;
        maxSorting[j].first = adapter.getMax(e, dim);
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
      double minOverlap = Double.MAX_VALUE;
      // the volume of mbr1 and mbr2
      double volume = 0.0;
      // indicates whether the sorting according to maximal or to minimal value
      // is
      // best for the split axis
      bestSorting = null;

      for(int i = 0; i <= size - 2 * minEntries; i++) {
        // test the sorting with respect to the minimal values
        HyperBoundingBox mbr1 = mbr(minSorting, 0, minEntries + i);
        HyperBoundingBox mbr2 = mbr(minSorting, minEntries + i, size);
        double currentOverlap = SpatialUtil.relativeOverlap(mbr1, mbr2);
        double vol1 = SpatialUtil.volume(mbr1);
        double vol2 = SpatialUtil.volume(mbr2);
        if(currentOverlap < minOverlap || (currentOverlap == minOverlap && (vol1 + vol2) < volume)) {
          minOverlap = currentOverlap;
          splitPoint = minEntries + i;
          bestSorting = minSorting;
          volume = vol1 + vol2;
        }
        // test the sorting with respect to the maximal values
        mbr1 = mbr(maxSorting, 0, minEntries + i);
        mbr2 = mbr(maxSorting, minEntries + i, size);
        currentOverlap = SpatialUtil.relativeOverlap(mbr1, mbr2);
        vol1 = SpatialUtil.volume(mbr1);
        vol2 = SpatialUtil.volume(mbr2);
        if(currentOverlap < minOverlap || (currentOverlap == minOverlap && (vol1 + vol2) < volume)) {
          minOverlap = currentOverlap;
          splitPoint = minEntries + i;
          bestSorting = maxSorting;
          volume = vol1 + vol2;
        }
      }
    }

    private E get(int off) {
      return getter.get(entries, off);
    }

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
    private HyperBoundingBox mbr(final DoubleIntPair[] sorting, final int from, final int to) {
      ModifiableHyperBoundingBox mbr = SpatialUtil.copyMBR(get(sorting[from]), adapter);
      for(int i = from + 1; i < to; i++) {
        mbr.extend(get(sorting[i]), adapter);
      }
      return mbr;
    }

    /**
     * Returns the split point.
     * 
     * @return the split point
     */
    public int getSplitPoint() {
      return splitPoint;
    }

    /**
     * Returns whether the sorting according to maximal or to minimal value has
     * been used for choosing the split axis and split point.
     * 
     * @return The sorting to use
     */
    public DoubleIntPair[] getBestSorting() {
      return bestSorting;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected TopologicalSplitter makeInstance() {
      return TopologicalSplitter.STATIC;
    }
  }
}