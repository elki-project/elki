package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Simple bulk loading strategy by sorting the data along the first dimension.
 * 
 * This is also known as Nearest-X, and attributed to:
 * <p>
 * Roussopoulos, N. and Leifker, D.:<br />
 * Direct spatial search on pictorial databases using packed R-trees<br />
 * In: ACM SIGMOD Record 14-4
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "Roussopoulos, N. and Leifker, D.", title = "Direct spatial search on pictorial databases using packed R-trees", booktitle = "ACM SIGMOD Record 14-4", url = "http://dx.doi.org/10.1145/971699.318900")
public class OneDimSortBulkSplit extends AbstractBulkSplit {
  /**
   * Static instance.
   */
  public static final AbstractBulkSplit STATIC = new OneDimSortBulkSplit();

  /**
   * Constructor.
   */
  protected OneDimSortBulkSplit() {
    super();
  }

  @Override
  public <T extends SpatialComparable> List<List<T>> partition(List<T> spatialObjects, int minEntries, int maxEntries) {
    // Sort by first dimension
    Collections.sort(spatialObjects, new Comparator<SpatialComparable>() {
      @Override
      public int compare(SpatialComparable o1, SpatialComparable o2) {
        double min1 = (o1.getMax(0) + o1.getMin(0)) * .5;
        double min2 = (o2.getMax(0) + o2.getMin(0)) * .5;
        return Double.compare(min1, min2);
      }
    });
    return trivialPartition(spatialObjects, minEntries, maxEntries);
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
    protected AbstractBulkSplit makeInstance() {
      return OneDimSortBulkSplit.STATIC;
    }
  }
}