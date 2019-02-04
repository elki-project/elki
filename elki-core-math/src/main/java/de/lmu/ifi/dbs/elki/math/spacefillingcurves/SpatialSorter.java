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

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Interface for spatial sorting - ZCurves, Peano curves, Hilbert curves, ...
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public interface SpatialSorter {
  /**
   * Partitions the specified feature vectors
   * 
   * @param objs the spatial objects to be sorted
   */
  default void sort(List<? extends SpatialComparable> objs) {
    double[] mms = computeMinMax(objs);
    sort(objs, 0, objs.size(), mms, null);
  }

  /**
   * Sort part of the list (start to end).
   * 
   * @param objs the spatial objects to be sorted
   * @param start First index to sort (e.g. 0)
   * @param end End of range (e.g. <code>site()</code>)
   * @param minmax Array with dim pairs of (min, max) of value ranges
   * @param dims Dimensions to sort by, for indexing vectors <em>and</em>
   *        {@code minmax}.
   */
  void sort(List<? extends SpatialComparable> objs, int start, int end, double[] minmax, int[] dims);

  /**
   * Compute the minimum and maximum for each dimension.
   * 
   * @param objs Objects
   * @return Array of min, max pairs (length = 2 * dim)
   */
  static double[] computeMinMax(Iterable<? extends SpatialComparable> objs) {
    Iterator<? extends SpatialComparable> it = objs.iterator();
    if(!it.hasNext()) {
      throw new IllegalArgumentException("Cannot compute minimum and maximum of empty list.");
    }
    SpatialComparable first = it.next();
    final int dim = first.getDimensionality();
    // Compute min and max for each dimension:
    double[] mm = new double[dim << 1];
    for(int d = 0, d2 = 0; d < dim; d++) {
      mm[d2++] = first.getMin(d);
      mm[d2++] = first.getMax(d);
    }
    while(it.hasNext()) {
      SpatialComparable obj = it.next();
      for(int d = 0, d2 = 0; d < dim; d++) {
        mm[d2] = MathUtil.min(mm[d2], obj.getMin(d));
        d2++;
        mm[d2] = MathUtil.max(mm[d2], obj.getMax(d));
        d2++;
      }
    }
    return mm;
  }
}
