/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.index.tree.spatial.kd.split;

import elki.data.NumberVector;
import elki.data.VectorUtil.SortDBIDsBySingleDimension;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.QuickSelectDBIDs;
import elki.database.relation.Relation;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Split on the median of the axis with the largest length.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class MedianSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final MedianSplit STATIC = new MedianSplit();

  @Override
  public Info findSplit(Relation<? extends NumberVector> relation, int dims, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, int left, int right, SortDBIDsBySingleDimension comp) {
    double[] minmax = Util.minmaxRange(dims, relation, iter, left, right);
    final int dim = Util.argmaxdiff(minmax);
    final int middle = (left + right) >>> 1;
    if(minmax[dim] >= minmax[dim + dims]) { // Stop on duplicate points.
      return null;
    }
    comp.setDimension(dim);
    QuickSelectDBIDs.quickSelect(sorted, comp, left, right, middle);
    return new Info(dim, middle, relation.get(iter.seek(middle)).doubleValue(dim));
  }

  /**
   * Parameterizer
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public MedianSplit make() {
      return STATIC;
    }
  }
}
