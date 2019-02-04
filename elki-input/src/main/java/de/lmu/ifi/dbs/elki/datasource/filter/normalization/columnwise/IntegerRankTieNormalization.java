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
package de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;

import it.unimi.dsi.fastutil.ints.IntComparator;

/**
 * Normalize vectors according to their rank in the attributes.
 * 
 * Note: <b>ranks are multiplied by 2</b>, to be able to give ties an integer
 * rank. (e.g. when the first two records are tied, they both have rank "1"
 * then, followed by the next on "4")
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Alias("de.lmu.ifi.dbs.elki.datasource.filter.normalization.RankTieNormalization")
public class IntegerRankTieNormalization implements ObjectFilter {
  /**
   * Constructor.
   */
  public IntegerRankTieNormalization() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    final int len = objects.dataLength();
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    int[] order = new int[len];
    for(int i = 0; i < len; i++) {
      order[i] = i;
    }
    Sorter comparator = new Sorter();

    for(int r = 0; r < objects.metaLength(); r++) {
      final SimpleTypeInformation<?> type = objects.meta(r);
      final List<?> column = objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      @SuppressWarnings("unchecked")
      final List<? extends NumberVector> castColumn = (List<? extends NumberVector>) column;
      // Get the replacement type information
      final int dim = ((VectorFieldTypeInformation<?>) type).getDimensionality();
      final VectorFieldTypeInformation<IntegerVector> outType = new VectorFieldTypeInformation<>(IntegerVector.STATIC, dim);

      // Output vectors
      int[][] posvecs = new int[len][dim];
      // Sort for each dimension
      for(int d = 0; d < dim; d++) {
        // Sort
        comparator.setup(castColumn, d);
        IntegerArrayQuickSort.sort(order, comparator);
        // Transfer positions to output vectors
        for(int sta = 0; sta < order.length;) {
          double v = castColumn.get(order[sta]).doubleValue(d);
          // Compute ties
          int end = sta + 1;
          while(end < order.length && !(v < castColumn.get(order[end]).doubleValue(d))) {
            end++;
          }
          final int pos = (sta + end - 1);
          for(int i = sta; i < end; i++) {
            posvecs[order[i]][d] = pos;
          }
          sta = end;
        }
      }

      // Prepare output data
      final List<IntegerVector> outColumn = new ArrayList<>(len);
      for(int i = 0; i < len; i++) {
        outColumn.add(new IntegerVector(posvecs[i]));
      }
      bundle.appendColumn(outType, outColumn);
    }
    return bundle;
  }

  /**
   * Class to sort an index array by a particular dimension.
   * 
   * @author Erich Schubert
   */
  private static class Sorter implements IntComparator {
    /**
     * Column to use for sorting.
     */
    List<? extends NumberVector> col;

    /**
     * Dimension to use for sorting.
     */
    int dim;

    /**
     * Configure the sorting class.
     * 
     * @param col Column to read
     * @param dim Dimension to use.
     */
    public void setup(List<? extends NumberVector> col, int dim) {
      this.col = col;
      this.dim = dim;
    }

    @Override
    public int compare(int x, int y) {
      final double vx = col.get(x).doubleValue(dim), vy = col.get(y).doubleValue(dim);
      return (vx < vy) ? -1 : (vx == vy) ? 0 : +1;
    }
  }
}
