package experimentalcode.erich;

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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.AbstractBulkSplit;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Bulk loading by spatially sorting the objects, then partitioning the sorted
 * list appropriately.
 * 
 * @author Erich Schubert
 */
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for spatial sorting
     */
    public static final OptionID SORTER_ID = OptionID.getOrCreateOptionID("rtree.bulk.spatial-sort", "Strategy for spatial sorting in bulk loading.");

    /**
     * Sorting class
     */
    SpatialSorter sorter;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<SpatialSorter> sorterP = new ObjectParameter<SpatialSorter>(SORTER_ID, SpatialSorter.class);
      if(config.grab(sorterP)) {
        sorter = sorterP.instantiateClass(config);
      }
    }

    @Override
    protected SpatialSortBulkSplit makeInstance() {
      return new SpatialSortBulkSplit(sorter);
    }
  }
}
