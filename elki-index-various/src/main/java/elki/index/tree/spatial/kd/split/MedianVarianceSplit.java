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

import static elki.math.linearalgebra.VMath.argmax;

import elki.data.NumberVector;
import elki.data.VectorUtil.SortDBIDsBySingleDimension;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.QuickSelectDBIDs;
import elki.database.relation.Relation;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Split on the median of the axis with the largest variance.
 * <p>
 * Reference:
 * <p>
 * S. M. Omohundro<br>
 * Efficient Algorithms with Neural Network Behaviour<br>
 * Journal of Complex Systems 1(2)
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "S. M. Omohundro", //
    title = "Efficient Algorithms with Neural Network Behaviour", //
    booktitle = "Journal of Complex Systems 1(2)", //
    url = "https://www.complex-systems.com/abstracts/v01_i02_a04/", //
    bibkey = "journals/jcs/Omohundro87")
public class MedianVarianceSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final MedianVarianceSplit STATIC = new MedianVarianceSplit();

  @Override
  public Info findSplit(Relation<? extends NumberVector> relation, int dims, ArrayModifiableDBIDs sorted, DBIDArrayMIter iter, int left, int right, SortDBIDsBySingleDimension comp) {
    double[] sumvar = Util.sumvar(relation, dims, iter, left, right);
    int bestdim = argmax(sumvar, dims, sumvar.length) - dims;
    if(sumvar[bestdim + dims] == 0) { // All duplicate.
      return null;
    }
    final int middle = (left + right) >>> 1;
    comp.setDimension(bestdim);
    QuickSelectDBIDs.quickSelect(sorted, comp, left, right, middle);
    return new Info(bestdim, middle, relation.get(iter.seek(middle)).doubleValue(bestdim));
  }

  /**
   * Parameterizer
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public MedianVarianceSplit make() {
      return STATIC;
    }
  }
}
