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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialSingleMeanComparator;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Sort-Tile-Recursive aims at tiling the data space with a grid-like structure
 * for partitioning the dataset into the required number of buckets.
 * <p>
 * Reference:
 * <p>
 * S. T. Leutenegger, M. A. Lopez, J. Edgington<br>
 * STR: A simple and efficient algorithm for R-tree packing<br>
 * Proc. 13th International Conference on Data Engineering (ICDE 1997)
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "S. T. Leutenegger, M. A. Lopez, J. Edgington", //
    title = "STR: A simple and efficient algorithm for R-tree packing", //
    booktitle = "Proc. 13th International Conference on Data Engineering (ICDE 1997)", //
    url = "https://doi.org/10.1109/ICDE.1997.582015", //
    bibkey = "DBLP:conf/icde/LeuteneggerEL97")
@Alias({ "str", "STR" })
public class SortTileRecursiveBulkSplit extends AbstractBulkSplit {
  /**
   * Static instance.
   */
  public static final SortTileRecursiveBulkSplit STATIC = new SortTileRecursiveBulkSplit();

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
    final int s = (int) FastMath.ceil(FastMath.pow(p, 1.0 / (dims - depth)));

    final double len = end - start; // double intentional!
    for(int i = 0; i < s; i++) {
      // We don't completely sort, but only ensure the quantile is invariant.
      int s2 = start + (int) ((i * len) / s);
      int e2 = start + (int) (((i + 1) * len) / s);
      // LoggingUtil.warning("STR " + dim + " s2:" + s2 + " e2:" + e2);
      if(e2 < end) {
        c.setDimension(depth);
        QuickSelect.quickSelect(objs, c, s2, end, e2);
      }
      if(depth + 1 == dims) {
        ret.add(objs.subList(s2, e2));
      }
      else {
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
    protected SortTileRecursiveBulkSplit makeInstance() {
      return STATIC;
    }
  }
}
