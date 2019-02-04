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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialSingleMeanComparator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * This is variation of the original STR bulk load for non-rectangular data
 * spaces. Instead of iterating through the dimensions and splitting each by
 * (approximately) the same factor, this variation tries to adjust the factor to
 * the extends of the data space. I.e. if the data set is twice as wide as high,
 * this should produce twice as many partitions on the X than on the Y axis.
 * 
 * Whether or not this offers benefits greatly depends on the distance queries
 * used. But for symmetric distances, the resulting pages should be more
 * rectangular, which often is beneficial.
 * 
 * See {@link SortTileRecursiveBulkSplit} for the original STR bulk load.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class AdaptiveSortTileRecursiveBulkSplit extends AbstractBulkSplit {
  /**
   * Static instance.
   */
  public static final AdaptiveSortTileRecursiveBulkSplit STATIC = new AdaptiveSortTileRecursiveBulkSplit();

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries) {
    final int dims = spatialObjects.get(0).getDimensionality();
    final int p = (int) FastMath.ceil(spatialObjects.size() / (double) maxEntries);
    List<List<T>> ret = new ArrayList<>(p);
    strPartition(spatialObjects, 0, spatialObjects.size(), 0, dims, maxEntries, new SpatialSingleMeanComparator(0), ret);
    return ret;
  }

  /**
   * Recursively partition.
   * 
   * @param objs Object list
   * @param start Subinterval start
   * @param end Subinterval end
   * @param depth Iteration depth (must be less than dimensionality!)
   * @param dims Total number of dimensions
   * @param maxEntries Maximum page size
   * @param c Comparison helper
   * @param ret Output list
   * @param <T> data type
   */
  protected <T extends SpatialComparable> void strPartition(List<T> objs, int start, int end, int depth, int dims, int maxEntries, SpatialSingleMeanComparator c, List<List<T>> ret) {
    final int p = (int) FastMath.ceil((end - start) / (double) maxEntries);

    // Compute min and max:
    double[] mm = new double[dims * 2];
    for (int d = 0; d < mm.length; d += 2) {
      mm[d] = Double.POSITIVE_INFINITY; // min <- +inf
      mm[d + 1] = Double.NEGATIVE_INFINITY; // max <- -inf
    }
    for (int i = start; i < end; i++) {
      T o = objs.get(i);
      for (int d1 = 0, d2 = 0; d2 < mm.length; d1++, d2 += 2) {
        mm[d2] = Math.min(mm[d2], o.getMin(d1));
        mm[d2 + 1] = Math.max(mm[d2 + 1], o.getMax(d1));
      }
    }
    // Find maximum and compute extends
    double maxex = 0.0;
    int sdim = depth;
    double[] exts = new double[dims];
    for (int d = 0; d < mm.length; d += 2) {
      final double extend = mm[d + 1] - mm[d];
      if (extend > maxex) {
        maxex = extend;
        sdim = d >>> 1;
      }
      exts[d >>> 1] = extend;
    }
    // Compute sum of the k largest extends:
    Arrays.sort(exts);
    double extsum = 0.;
    for (int d = depth; d < exts.length; d++) {
      extsum += exts[d];
    }
    // Chose the number of partitions:
    final int s;
    if (maxex > 0. && depth + 1 < dims) {
      s = (int) FastMath.ceil(FastMath.pow(p, 1.0 / (dims - depth)) * (dims - depth) * maxex / extsum);
    } else {
      s = (int) FastMath.ceil(FastMath.pow(p, 1.0 / (dims - depth)));
    }

    final double len = end - start; // double intentional!
    for (int i = 0; i < s; i++) {
      // We don't completely sort, but only ensure the quantile is invariant.
      int s2 = start + (int) ((i * len) / s);
      int e2 = start + (int) (((i + 1) * len) / s);
      // LoggingUtil.warning("STR " + dim + " s2:" + s2 + " e2:" + e2);
      if (e2 < end) {
        c.setDimension(sdim);
        QuickSelect.quickSelect(objs, c, s2, end, e2);
      }
      if (depth + 1 == dims) {
        ret.add(objs.subList(s2, e2));
      } else {
        // Descend
        strPartition(objs, s2, e2, depth + 1, dims, maxEntries, c, ret);
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected AdaptiveSortTileRecursiveBulkSplit makeInstance() {
      return STATIC;
    }
  }
}
