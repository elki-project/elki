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
package elki.math.statistics.dependence;

import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.optionhandling.Parameterizer;

import net.jafama.FastMath;

/**
 * Mutual Information (MI) dependence measure by dividing each attribute into
 * equal-width bins. MI can be seen as Kullback–Leibler divergence of the joint
 * distribution and the product of the marginal distributions.
 * <p>
 * For normalization, the resulting values are scaled by {@code mi/log(nbins)}.
 * This both cancels out the logarithm base, and normalizes for the number of
 * bins (a uniform distribution will yield a MI with itself of 1).
 * <p>
 * TODO: Offer normalized and non-normalized variants?
 * <p>
 * For a median-based discretization, see {@link MaximumConditionalEntropy}.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MutualInformationEquiwidthDependence implements Dependence {
  /**
   * Static instance.
   */
  public static final MutualInformationEquiwidthDependence STATIC = new MutualInformationEquiwidthDependence();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected MutualInformationEquiwidthDependence() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = Utils.size(adapter1, data1, adapter2, data2);
    final int bins = (int) FastMath.round(Math.sqrt(len));
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
      final double ipX = len / (double) sum1;
      final int[] row = counts[bin1];
      for(int bin2 = 0; bin2 < row.length; bin2++) {
        final int cell = row[bin2];
        // Skip empty cells.
        if(cell != 0) {
          // Mutual information pXY / (pX * pY)
          double pXY = cell / (double) len;
          // Inverse pXpY: 1 / (pX*pY)
          final double ipXpY = ipX * len / margin2[bin2];
          e += pXY * FastMath.log(pXY * ipXpY);
        }
      }
    }
    // Expected value for uniform identical: log(bins)!
    return e / FastMath.log(bins);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public MutualInformationEquiwidthDependence make() {
      return STATIC;
    }
  }
}
