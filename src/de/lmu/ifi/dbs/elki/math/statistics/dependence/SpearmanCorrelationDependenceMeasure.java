package de.lmu.ifi.dbs.elki.math.statistics.dependence;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Spearman rank-correlation coefficient, also known as Spearmans Rho.
 * 
 * @author Erich Schubert
 */
public class SpearmanCorrelationDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final SpearmanCorrelationDependenceMeasure STATIC = new SpearmanCorrelationDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected SpearmanCorrelationDependenceMeasure() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    double[] ranks1 = computeNormalizedRanks(adapter1, data1, len);
    double[] ranks2 = computeNormalizedRanks(adapter2, data2, len);

    // Second pass: variances and covariance
    double v1 = 0., v2 = 0., cov = 0.;
    for(int i = 0; i < len; i++) {
      double d1 = ranks1[i] - .5;
      double d2 = ranks2[i] - .5;
      v1 += d1 * d1;
      v2 += d2 * d2;
      cov += d1 * d2;
    }
    // Note: we did not normalize by len, as this cancels out.
    return cov / Math.sqrt(v1 * v2);
  }

  /**
   * Compute ranks of all objects, normalized to [0;1] (where 0 is the smallest
   * value, 1 is the largest).
   * 
   * @param adapter Data adapter
   * @param data Data array
   * @param len Length of data
   * @return Array of scores
   */
  public static <A> double[] computeNormalizedRanks(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
    // Sort the objects:
    int[] s1 = MathUtil.sequence(0, len);
    IntegerArrayQuickSort.sort(s1, new IntegerComparator() {
      @Override
      public int compare(int x, int y) {
        return Double.compare(adapter.getDouble(data, x), adapter.getDouble(data, y));
      }
    });
    double norm = .5 / (len - 1);
    double[] ret = new double[len];
    for(int i = 0; i < len;) {
      final int start = i++;
      double val = adapter.getDouble(data, s1[start]);
      while(i < len && adapter.getDouble(data, s1[i]) <= val) {
        i++;
      }
      final double score = (start + i - 1) * norm;
      for(int j = start; j < i; j++) {
        ret[s1[j]] = score;
      }
    }
    return ret;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SpearmanCorrelationDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
