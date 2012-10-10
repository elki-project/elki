package de.lmu.ifi.dbs.elki.datasource.filter.normalization;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.IntegerVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Normalize vectors according to their rank in the attributes.
 * 
 * Note: ranks are multiplied by 2, to be able to give ties an integer rank.
 * (e.g. first two records are tied at "1" then, followed by the next on "4")
 * 
 * @author Erich Schubert
 */
public class RankTieNormalization implements ObjectFilter {
  /**
   * Constructor.
   */
  public RankTieNormalization() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    final int len = objects.dataLength();
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    for(int r = 0; r < objects.metaLength(); r++) {
      final SimpleTypeInformation<?> type = objects.meta(r);
      final List<?> column = objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      @SuppressWarnings("unchecked")
      final List<? extends NumberVector<?>> castColumn = (List<? extends NumberVector<?>>) column;
      // Get the replacement type information
      final int dim = ((VectorFieldTypeInformation<?>) type).getDimensionality();
      final VectorFieldTypeInformation<IntegerVector> outType = new VectorFieldTypeInformation<IntegerVector>(IntegerVector.STATIC, dim);

      // Output vectors
      int[][] posvecs = new int[len][dim];
      // Sort for each dimension
      // TODO: an int[] array would be enough, if we could use a comparator...
      DoubleIntPair[] sorter = new DoubleIntPair[len];
      for(int i = 0; i < sorter.length; i++) {
        sorter[i] = new DoubleIntPair(Double.NaN, -1);
      }
      for(int d = 0; d < dim; d++) {
        // fill array
        for(int i = 0; i < sorter.length; i++) {
          sorter[i].first = castColumn.get(i).doubleValue(d);
          sorter[i].second = i;
        }
        // Sort
        Arrays.sort(sorter);
        // Transfer positions to output vectors
        for(int sta = 0; sta < sorter.length;) {
          // Compute ties
          int end = sta + 1;
          while(end < sorter.length && !(sorter[sta].first < sorter[end].first)) {
            end++;
          }
          final int pos = (sta + end - 1);
          for(int i = sta; i < end; i++) {
            posvecs[sorter[i].second][d] = pos;
          }
          sta = end;
        }
      }

      // Prepare output data
      final List<IntegerVector> outColumn = new ArrayList<IntegerVector>(len);
      for(int i = 0; i < len; i++) {
        outColumn.add(new IntegerVector(posvecs[i]));
      }
      bundle.appendColumn(outType, outColumn);
    }
    return bundle;
  }
}