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

import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialSingleMeanComparator;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Simple bulk loading strategy by sorting the data along the first dimension.
 * <p>
 * This is also known as Nearest-X, and attributed to:
 * <p>
 * N. Roussopoulos, D. Leifker<br>
 * Direct spatial search on pictorial databases using packed R-trees<br>
 * ACM SIGMOD Record 14-4
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "N. Roussopoulos, D. Leifker", //
    title = "Direct spatial search on pictorial databases using packed R-trees", //
    booktitle = "ACM SIGMOD Record 14-4", //
    url = "https://doi.org/10.1145/971699.318900", //
    bibkey = "doi:10.1145/971699.318900")
public class OneDimSortBulkSplit extends AbstractBulkSplit {
  /**
   * Static instance.
   */
  public static final OneDimSortBulkSplit STATIC = new OneDimSortBulkSplit();

  /**
   * Constructor.
   */
  protected OneDimSortBulkSplit() {
    super();
  }

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries) {
    // Sort by first dimension
    Collections.sort(spatialObjects, new SpatialSingleMeanComparator(0));
    return trivialPartition(spatialObjects, minEntries, maxEntries);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected OneDimSortBulkSplit makeInstance() {
      return OneDimSortBulkSplit.STATIC;
    }
  }
}
