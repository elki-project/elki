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
 * Split by the best reduction in sum-of-squares.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class LeastSSQSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final LeastSSQSplit STATIC = new LeastSSQSplit();

  @Override
  public Info findSplit(Relation<? extends NumberVector> relation, int dims, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, int left, int right, SortDBIDsBySingleDimension comp) {
    int bestdim = 0, bestpos = (right - left) >>> 1;
    double bestscore = Double.NEGATIVE_INFINITY;
    double[] sum = new double[dims];
    for(iter.seek(left); iter.getOffset() < right; iter.advance()) {
      for(int d = 0; d < dims; d++) {
        sum[d] += relation.get(iter).doubleValue(d);
      }
    }
    for(int dim = 0; dim < dims; dim++) {
      // Sort the objects by the chosen attribute:
      comp.setDimension(dim);
      sorted.sort(left, right, comp);
      // Minimizing the SSQs is the same as maximizing the weighted distance
      // between the centers.
      int i = 1, j = right - left - 1;
      double[] s1 = new double[dims], s2 = sum.clone();
      for(iter.seek(left); j > 1; iter.advance(), i++, j--) {
        NumberVector vec = relation.get(iter);
        for(int d = 0; d < dims; d++) {
          final double v = vec.doubleValue(d);
          s1[d] += v;
          s2[d] -= v;
        }
        double score = 0;
        for(int d = 0; d < dims; d++) {
          double v = s1[d] / i - s2[d] / j;
          score += v * v;
        }
        double s = score * i * j;
        if(s > bestscore) {
          bestscore = s;
          bestdim = dim;
          bestpos = i;
        }
      }
    }
    if(bestscore == 0) { // All duplicate.
      return null;
    }
    bestpos += left;
    comp.setDimension(bestdim);
    QuickSelectDBIDs.quickSelect(sorted, comp, left, right, bestpos);
    return new Info(bestdim, bestpos, relation.get(iter.seek(bestpos)).doubleValue(bestdim));
  }

  /**
   * Parameterizer
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public LeastSSQSplit make() {
      return STATIC;
    }
  }
}
