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
package elki.index.tree.spatial.rstarvariants.strategies.bulk;

import java.util.List;

import elki.data.spatial.SpatialComparable;
import elki.math.spacefillingcurves.SpatialSorter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Bulk loading by spatially sorting the objects, then partitioning the sorted
 * list appropriately.
 * <p>
 * Based conceptually on:
 * <p>
 * On packing R-trees<br>
 * I. Kamel, C. Faloutsos<br>
 * Proc. 2nd Int. Conf. on Information and Knowledge Management (CIKM)
 *
 * @composed - - - SpatialSorter
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "I. Kamel, C. Faloutsos", //
    title = "On packing R-trees", //
    booktitle = "Proc. 2nd Int. Conf. on Information and Knowledge Management", //
    url = "https://doi.org/10.1145/170088.170403", //
    bibkey = "DBLP:conf/cikm/KamelF93")
public class SpatialSortBulkSplit extends AbstractBulkSplit {
  /**
   * Sorting class
   */
  final SpatialSorter sorter;

  /**
   * Constructor.
   * 
   * @param sorter Sorting strategy
   */
  protected SpatialSortBulkSplit(SpatialSorter sorter) {
    super();
    this.sorter = sorter;
  }

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries) {
    sorter.sort(spatialObjects);
    return super.trivialPartition(spatialObjects, minEntries, maxEntries);
  }

  /**
   * Parametization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option ID for spatial sorting
     */
    public static final OptionID SORTER_ID = new OptionID("rtree.bulk.spatial-sort", "Strategy for spatial sorting in bulk loading.");

    /**
     * Sorting class
     */
    SpatialSorter sorter;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<SpatialSorter>(SORTER_ID, SpatialSorter.class) //
          .grab(config, x -> sorter = x);
    }

    @Override
    public SpatialSortBulkSplit make() {
      return new SpatialSortBulkSplit(sorter);
    }
  }
}
