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
 * Bounded midpoint splitting.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class BoundedMidpointSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final BoundedMidpointSplit STATIC = new BoundedMidpointSplit();

  @Override
  public Info findSplit(Relation<? extends NumberVector> relation, int dims, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, int left, int right, SortDBIDsBySingleDimension comp) {
    double[] minmax = Util.minmaxRange(dims, relation, iter, left, right);
    final int dim = Util.argmaxdiff(minmax);
    double mid = 0.5 * (minmax[dims + dim] + minmax[dim]);
    int r = Util.pivot(relation, sorted, iter, dim, left, right, mid);
    if(r == right) { // Duplicate points!
      return null;
    }
    // if too unbalanced, fall back to a quantile:
    final int q = (right - left) >>> 4;
    if(left + q > r) {
      comp.setDimension(dim);
      QuickSelectDBIDs.quickSelect(sorted, comp, r, right, r = left + q);
      mid = relation.get(iter.seek(r)).doubleValue(dim);
    }
    else if(right - q < r) {
      comp.setDimension(dim);
      QuickSelectDBIDs.quickSelect(sorted, comp, left, r, r = right - q);
      mid = relation.get(iter.seek(r)).doubleValue(dim);
    }
    assert left < r && r < right : "Useless split selected: " + left + " < " + r + " < " + right;
    return new Info(dim, r, mid);
  }

  /**
   * Parameterizer
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public BoundedMidpointSplit make() {
      return STATIC;
    }
  }
}
