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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Jensen-Shannon Divergence is closely related to mutual information.
 * 
 * The output value is normalized, such that an evenly distributed and identical
 * distribution will yield a value of 1. Independent distributions may still
 * yield values close to .25, though.
 * 
 * TODO: Offer normalized and non-normalized variants?
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class JensenShannonEquiwidthDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final JensenShannonEquiwidthDependenceMeasure STATIC = new JensenShannonEquiwidthDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected JensenShannonEquiwidthDependenceMeasure() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    final int bins = (int) FastMath.round(FastMath.sqrt(len));
    final int maxbin = bins - 1;

    double min1 = adapter1.getDouble(data1, 0), max1 = min1;
    double min2 = adapter2.getDouble(data2, 0), max2 = min2;
    for(int i = 1; i < len; i++) {
      final double vx = adapter1.getDouble(data1, i);
      final double vy = adapter2.getDouble(data2, i);
      if(vx < min1) {
        min1 = vx;
      }
      else if(vx > max1) {
        max1 = vx;
      }
      if(vy < min2) {
        min2 = vy;
      }
      else if(vy > max2) {
        max2 = vy;
      }
    }
    final double scale1 = (max1 > min1) ? bins / (max1 - min1) : 1;
    final double scale2 = (max2 > min2) ? bins / (max2 - min2) : 1;

    int[] margin1 = new int[bins], margin2 = new int[bins];
    int[][] counts = new int[bins][bins];
    for(int i = 0; i < len; i++) {
      int bin1 = (int) FastMath.floor((adapter1.getDouble(data1, i) - min1) * scale1);
      int bin2 = (int) FastMath.floor((adapter2.getDouble(data2, i) - min2) * scale2);
      bin1 = bin1 < bins ? bin1 : maxbin;
      bin2 = bin2 < bins ? bin2 : maxbin;
      margin1[bin1]++;
      margin2[bin2]++;
      counts[bin1][bin2]++;
    }

    // calculating relative frequencies
    double e = 0;
    for(int bin1 = 0; bin1 < counts.length; bin1++) {
      // Margin value for row i.
      final int sum1 = margin1[bin1];
      // Skip empty rows early.
      if(sum1 == 0) {
        continue;
      }
      // Inverse pX
      final double pX = sum1 / (double) len;
      final int[] row = counts[bin1];
      for(int bin2 = 0; bin2 < row.length; bin2++) {
        final int sum2 = margin2[bin2];
        if(sum2 > 0) {
          final int cell = row[bin2];
          // JS divergence of pXY and (pX * pY)
          double pXY = cell / (double) len;
          final double pXpY = pX * sum2 / len;
          final double iavg = 2. / (pXY + pXpY);
          e += pXY > 0. ? pXY * FastMath.log(pXY * iavg) : 0.;
          e += pXpY * FastMath.log(pXpY * iavg);
        }
      }
    }
    // Expected value for evenly distributed and identical:
    // pX = pY = 1/b and thus pXpY = 1/b^2.
    // for i==j, pXY=1/b and thus iavg = 2*b*b/(b+1)
    // otherwise, pXY=0 and thus iavg = 2*b*b
    // pXY: e += log(b*2/(b+1)) = log(b) + log(2/(b+1))
    // pXpY1: e += 1/b*log(2/(b+1)) = 1/b*log(2/(b+1))
    // pXpY2: e += (b-1)/b*log(2) = (b-1)/b*log(2)
    final double exp = FastMath.log(bins) + (1. + 1. / bins) * FastMath.log(2. / (bins + 1)) + (bins - 1.) / bins * MathUtil.LOG2;
    // e *= .5; // Average, but we need to adjust exp then, too!
    return e / exp;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected JensenShannonEquiwidthDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
