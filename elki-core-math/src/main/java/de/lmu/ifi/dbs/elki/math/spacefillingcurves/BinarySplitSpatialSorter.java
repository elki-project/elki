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
package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Spatially sort the data set by repetitive binary splitting, circulating
 * through the dimensions. This is essentially the bulk-loading proposed for the
 * k-d-tree, as it will produce a perfectly balanced k-d-tree. The resulting
 * order is the sequence in which objects would then be stored in the k-d-tree.
 * <p>
 * Note that when using this for bulk-loading an R-tree, the result will
 * <em>not</em> be a k-d-tree, not even remotely similar, as the splits are not
 * preserved.
 * <p>
 * Reference (for the bulk-loading):
 * <p>
 * J. L. Bentley<br>
 * Multidimensional binary search trees used for associative searching<br>
 * Communications of the ACM 18(9)
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "J. L. Bentley", //
    title = "Multidimensional binary search trees used for associative searching", //
    booktitle = "Communications of the ACM 18(9)", //
    url = "https://doi.org/10.1145/361002.361007", //
    bibkey = "DBLP:journals/cacm/Bentley75")
public class BinarySplitSpatialSorter implements SpatialSorter {
  /**
   * Static instance.
   */
  public static final BinarySplitSpatialSorter STATIC = new BinarySplitSpatialSorter();

  /**
   * Constructor, use {@link #STATIC} instead!
   */
  public BinarySplitSpatialSorter() {
    super();
  }

  @Override
  public void sort(List<? extends SpatialComparable> objs, int start, int end, double[] minmax, int[] dims) {
    final int numdim = (dims != null) ? dims.length : (minmax.length >>> 1);
    binarySplitSort(objs, start, end, 0, numdim, dims, new Sorter(0));
  }

  /**
   * Sort the array using a binary split in dimension curdim, then recurse with
   * the next dimension.
   * 
   * @param objs List of objects
   * @param start Interval start
   * @param end Interval end (exclusive)
   * @param depth Recursion depth
   * @param numdim Number of dimensions
   * @param dims Dimension indexes to sort by.
   * @param comp Comparator to use
   */
  private void binarySplitSort(List<? extends SpatialComparable> objs, final int start, final int end, int depth, final int numdim, int[] dims, Sorter comp) {
    final int mid = start + ((end - start) >>> 1);
    // Make invariant
    comp.setDimension(dims != null ? dims[depth] : depth);
    QuickSelect.quickSelect(objs, comp, start, end, mid);
    // Recurse
    final int nextdim = (depth + 1) % numdim;
    if(start < mid - 1) {
      binarySplitSort(objs, start, mid, nextdim, numdim, dims, comp);
    }
    if(mid + 2 < end) {
      binarySplitSort(objs, mid + 1, end, nextdim, numdim, dims, comp);
    }
  }

  /**
   * Comparator for sorting spatial objects by the mean value in a single
   * dimension.
   * 
   * @author Erich Schubert
   */
  private static class Sorter implements Comparator<SpatialComparable> {
    /**
     * Current dimension.
     */
    int dim;

    /**
     * Constructor.
     * 
     * @param dim Dimension to sort by.
     */
    public Sorter(int dim) {
      super();
      this.dim = dim;
    }

    /**
     * Set the dimension to sort by.
     * 
     * @param dim Dimension
     */
    public void setDimension(int dim) {
      this.dim = dim;
    }

    @Override
    public int compare(SpatialComparable o1, SpatialComparable o2) {
      final double v1 = o1.getMin(dim) + o1.getMax(dim);
      final double v2 = o2.getMin(dim) + o2.getMax(dim);
      return Double.compare(v1, v2);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected BinarySplitSpatialSorter makeInstance() {
      return STATIC;
    }
  }
}
