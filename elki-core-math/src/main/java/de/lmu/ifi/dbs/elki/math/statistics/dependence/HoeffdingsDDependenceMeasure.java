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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Calculate Hoeffding's D as a measure of dependence.
 * <p>
 * References:
 * <p>
 * W. Hoeffding:<br>
 * A non-parametric test of independence<br>
 * The Annals of Mathematical Statistics 19:546–57
 * <p>
 * The resulting value is scaled by 30, so it is in the range {@code [-.5;1]}.
 *
 * @author Yinchong Yang
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "W. Hoeffding", //
    title = "A non-parametric test of independence", //
    booktitle = "The Annals of Mathematical Statistics 19", //
    url = "http://www.jstor.org/stable/2236021", //
    bibkey = "journals/mathstat/Hoeffding48")
public class HoeffdingsDDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final HoeffdingsDDependenceMeasure STATIC = new HoeffdingsDDependenceMeasure();

  /**
   * Constructor - use {@link #STATIC} instance.
   */
  protected HoeffdingsDDependenceMeasure() {
    super();
  }

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int n = size(adapter1, data1, adapter2, data2);
    assert (n > 4) : "Hoeffdings D needs at least 5 elements!";
    if(n <= 4) {
      return Double.NaN;
    }
    double[] r = ranks(adapter1, data1, n);
    double[] s = ranks(adapter2, data2, n);
    // TODO: is it possible to exploit sorting to accelerate computing q?
    double[] q = computeBivariateRanks(adapter1, data1, adapter2, data2, n);

    double d1 = 0, d2 = 0, d3 = 0;
    for(int i = 0; i < n; i++) {
      d1 += q[i] * (q[i] - 1); // Note: our q is 0-indexed.
      d2 += (r[i] - 1) * (r[i] - 2) * (s[i] - 1) * (s[i] - 2);
      d3 += (r[i] - 2) * (s[i] - 2) * q[i];
    }

    // Factor n-2 was moved for better numerical behavior.
    double nom = (n - 3.) * d1 + d2 / (n - 2) - 2. * d3;
    double div = n * (n - 1.) * (n - 3.) * (n - 4.);
    double d = 30 * nom / div;
    return d < 1. ? d : 1.;
  }

  /**
   * Compute bivariate ranks.
   * <p>
   * q[i] is the number of objects such that x[j] &lt; x[i] and y[j] &lt; y[i]
   *
   * @param adapter1 First adapter
   * @param data1 First data set
   * @param adapter2 Second adapter
   * @param data2 Second data set
   * @param len Length
   * @return Bivariate rank statistics.
   */
  protected static <A, B> double[] computeBivariateRanks(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2, int len) {
    double[] ret = new double[len];
    for(int i = 0; i < len; i++) {
      for(int j = i + 1; j < len; j++) {
        double xi = adapter1.getDouble(data1, i),
            xj = adapter1.getDouble(data1, j);
        double yi = adapter2.getDouble(data2, i),
            yj = adapter2.getDouble(data2, j);
        if(xi < xj) {
          ret[j] += (yi < yj) ? 1 : (yi == yj) ? .5 : 0;
        }
        else if(xj < xi) {
          ret[i] += (yj < yi) ? 1 : (yj == yi) ? .5 : 0;
        }
        else { // tied on x
          if(yi < yj) {
            ret[j] += .5;
          }
          else if(yj < yi) {
            ret[i] += .5;
          }
          else { // Double tied
            ret[i] += .25;
            ret[j] += .25;
          }
        }
      }
    }
    return ret;
  }

  // Tabular approximation
  private final static double[] TABVAL = { //
      0.5297, 0.4918, 0.4565, 0.4236, 0.393, //
      0.3648, 0.3387, 0.3146, 0.2924, 0.2719, // 10
      0.253, 0.2355, 0.2194, 0.2045, 0.1908, //
      0.1781, 0.1663, 0.1554, 0.1453, 0.1359, // 20
      0.1273, 0.1192, 0.1117, 0.1047, 0.0982, //
      0.0921, 0.0864, 0.0812, 0.0762, 0.0716, // 30
      0.0673, 0.0633, 0.0595, 0.056, 0.0527, //
      0.0496, 0.0467, 0.044, 0.0414, 0.039, // 40
      0.0368, 0.0347, 0.0327, 0.0308, 0.0291, //
      0.0274, 0.0259, 0.0244, 0.023, 0.0217, // 50
      0.0205, 0.0194, 0.0183, 0.0173, 0.0163, //
      0.0154, 0.0145, 0.0137, 0.013, 0.0123, // 60
      0.0116, 0.011, 0.0104, 0.0098, 0.0093, //
      0.0087, 0.0083, 0.0078, 0.0074, 0.007, // 70
      0.0066, 0.0063, 0.0059, 0.0056, 0.0053, //
      0.005, 0.0047, 0.0045, 0.0042, 0.0025, // 80
      0.0014, 0.0008, 0.0005, 0.0003, 0.0002, 0.0001 };

  // Table positions
  private final static double[] TABPOS = new double[] { //
      1.10, 1.15, 1.20, 1.25, 1.30, 1.35, 1.40, 1.45, 1.50, 1.55, //
      1.60, 1.65, 1.70, 1.75, 1.80, 1.85, 1.90, 1.95, 2.00, 2.05, //
      2.10, 2.15, 2.20, 2.25, 2.30, 2.35, 2.40, 2.45, 2.50, 2.55, //
      2.60, 2.65, 2.70, 2.75, 2.80, 2.85, 2.90, 2.95, 3.00, 3.05, //
      3.10, 3.15, 3.20, 3.25, 3.30, 3.35, 3.40, 3.45, 3.50, 3.55, //
      3.60, 3.65, 3.70, 3.75, 3.80, 3.85, 3.90, 3.95, 4.00, 4.05, //
      4.10, 4.15, 4.20, 4.25, 4.30, 4.35, 4.40, 4.45, 4.50, 4.55, //
      4.60, 4.65, 4.70, 4.75, 4.80, 4.85, 4.90, 4.95, 5.00, //
      5.50, 6.00, 6.50, 7.00, 7.50, 8.00, 8.50 };

  /**
   * Convert Hoeffding D value to a p-value.
   *
   * @param d D value
   * @param n Data set size
   * @return p-value
   */
  public double toPValue(double d, int n) {
    double b = d / 30 + 1. / (36 * n);
    double z = .5 * MathUtil.PISQUARE * MathUtil.PISQUARE * n * b;

    // Exponential approximation
    if(z < 1.1 || z > 8.5) {
      double e = FastMath.exp(0.3885037 - 1.164879 * z);
      return (e > 1) ? 1 : (e < 0) ? 0 : e;
    }
    // Tabular approximation
    for(int i = 0; i < 86; i++) {
      if(TABPOS[i] >= z) {
        // Exact table value
        if(TABPOS[i] == z) {
          return TABVAL[i];
        }
        // Linear interpolation
        double x1 = TABPOS[i], x0 = TABPOS[i - 1];
        double y1 = TABVAL[i], y0 = TABVAL[i - 1];
        return y0 + (y1 - y0) * (z - x0) / (x1 - x0);
      }
    }
    return -1;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HoeffdingsDDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
