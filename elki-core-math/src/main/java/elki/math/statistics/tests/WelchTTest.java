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
package elki.math.statistics.tests;

import elki.math.MeanVariance;
import elki.math.statistics.distribution.BetaDistribution;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Calculates a test statistic according to Welch's t test for two samples
 * Supplies methods for calculating the degrees of freedom according to the
 * Welch-Satterthwaite Equation. Also directly calculates a two-sided p-value
 * for the underlying t-distribution.
 * <p>
 * Note: if you test i.i.d. normal distributed data, the p-values will be
 * approximately uniformly distributed.
 *
 * @author Jan Brusis
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @assoc - - - StudentsTDistribution
 */
public class WelchTTest implements GoodnessOfFitTest {
  /**
   * Static instance.
   */
  public static final WelchTTest STATIC = new WelchTTest();

  /**
   * Constructor.
   */
  public WelchTTest() {
    super();
  }

  @Override
  public double deviation(double[] sample1, double[] sample2) {
    MeanVariance mv = new MeanVariance();
    double mean1 = mv.put(sample1).getMean();
    double avgvar1 = mv.getSampleVariance() / sample1.length;
    double mean2 = mv.reset().put(sample2).getMean();
    double avgvar2 = mv.getSampleVariance() / sample2.length;
    // Welch t-statistic:
    double t = (mean1 - mean2) / Math.sqrt(avgvar1 + avgvar2);
    // Degrees of freedom using Welch-Satterthwaite:
    double wvariance = avgvar1 + avgvar2;
    double v = Math.round(wvariance * wvariance / //
        (avgvar1 * avgvar1 / ((double) sample1.length - 1) + avgvar2 * avgvar2 / ((double) sample2.length - 1)));
    // Student-t p-value, two-sided:
    return 1 - BetaDistribution.regularizedIncBeta(v / (t * t + v), v * .5, 0.5);
  }

  /**
   * Parameterizer, to use the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public WelchTTest make() {
      return STATIC;
    }
  }
}
