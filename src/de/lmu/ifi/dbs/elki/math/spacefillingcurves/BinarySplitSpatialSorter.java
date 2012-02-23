package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Spatially sort the data set by repetitive binary splitting, circulating
 * through the dimensions. This is essentially the bulk-loading proposed for the
 * k-d-tree, as it will produce a perfectly balanced k-d-tree. The resulting
 * order is the sequence in which objects would then be stored in the k-d-tree.
 * 
 * Note that when using this for bulk-loading an R-tree, the result will
 * <em>not</em> be a k-d-tree, not even remotely similar, as the splits are not
 * preserved.
 * 
 * Reference (for the bulk-loading):
 * <p>
 * J. L. Bentley<br/>
 * Multidimensional binary search trees used for associative searching<br/>
 * Communications of the ACM, Vol. 18 Issue 9, Sept. 1975
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. L. Bentley", title = "Multidimensional binary search trees used for associative searching", booktitle = "Communications of the ACM, Vol. 18 Issue 9, Sept. 1975", url = "http://dx.doi.org/10.1145/361002.361007")
public class BinarySplitSpatialSorter extends AbstractSpatialSorter {
  /**
   * Constructor.
   */
  public BinarySplitSpatialSorter() {
    super();
  }

  @Override
  public <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax) {
    final int dims = objs.get(0).getDimensionality();
    binarySplitSort(objs, start, end, 1, dims, new DimC());
  }

  /**
   * Sort the array using a binary split in dimension curdim, then recurse with
   * the next dimension.
   * 
   * @param objs List of objects
   * @param start Interval start
   * @param end Interval end (exclusive)
   * @param curdim Current dimension
   * @param dims Number of dimensions
   * @param comp Comparator to use
   */
  private <T extends SpatialComparable> void binarySplitSort(List<T> objs, final int start, final int end, int curdim, final int dims, DimC comp) {
    final int mid = start + ((end - start) >>> 1);
    // Make invariant
    comp.dim = curdim;
    QuickSelect.quickSelect(objs, comp, start, end, mid);
    // Recurse
    final int nextdim = (curdim % dims) + 1;
    if(start < mid - 1) {
      binarySplitSort(objs, start, mid, nextdim, dims, comp);
    }
    if(mid + 2 < end) {
      binarySplitSort(objs, mid + 1, end, nextdim, dims, comp);
    }
  }

  /**
   * Comparator that uses only a particular dimension.
   * 
   * This comparator is meant to be reused, and the dimension to be changed, to
   * reduce the number of objects allocated.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private static class DimC implements Comparator<SpatialComparable> {
    public int dim = -1;

    @Override
    public int compare(SpatialComparable o1, SpatialComparable o2) {
      double m1 = o1.getMax(dim) + o1.getMin(dim);
      double m2 = o2.getMax(dim) + o2.getMin(dim);
      return Double.compare(m1, m2);
    }
  }
}