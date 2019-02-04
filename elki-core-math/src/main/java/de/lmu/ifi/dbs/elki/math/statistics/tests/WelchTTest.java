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
package de.lmu.ifi.dbs.elki.math.statistics.tests;

import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.StudentsTDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Calculates a test statistic according to Welch's t test for two samples
 * Supplies methods for calculating the degrees of freedom according to the
 * Welch-Satterthwaite Equation. Also directly calculates a two-sided p-value
 * for the underlying t-distribution
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
    MeanVariance mv1 = new MeanVariance(), mv2 = new MeanVariance();
    for(double d : sample1) {
      mv1.put(d);
    }
    for(double d : sample2) {
      mv2.put(d);
    }

    final double t = calculateTestStatistic(mv1, mv2);
    final int v = calculateDOF(mv1, mv2);
    return 1 - calculatePValue(t, v);
  }

  /**
   * Calculate the statistic of Welch's t test using statistical moments of the
   * provided data samples
   * 
   * @param mv1 Mean and variance of first sample
   * @param mv2 Mean and variance of second sample
   * @return Welch's t statistic
   */
  public static double calculateTestStatistic(MeanVariance mv1, MeanVariance mv2) {
    final double delta = mv1.getMean() - mv2.getMean();
    final double relvar1 = mv1.getSampleVariance() / mv1.getCount();
    final double relvar2 = mv2.getSampleVariance() / mv2.getCount();
    return delta / Math.sqrt(relvar1 + relvar2);
  }

  /**
   * Calculates the degree of freedom according to Welch-Satterthwaite
   * 
   * @param mv1 Mean and variance of first sample
   * @param mv2 Mean and variance of second sample
   * @return Estimated degrees of freedom.
   */
  public static int calculateDOF(MeanVariance mv1, MeanVariance mv2) {
    final double relvar1 = mv1.getSampleVariance() / mv1.getCount();
    final double relvar2 = mv2.getSampleVariance() / mv2.getCount();
    final double wvariance = relvar1 + relvar2;
    final double div = relvar1 * relvar1 / (mv1.getCount() - 1) + relvar2 * relvar2 / (mv2.getCount() - 1);
    return (int) (wvariance * wvariance / div);
  }

  /**
   * Calculates the two-sided p-Value of the underlying t-Distribution with v
   * degrees of freedom
   * 
   * @param t Integration limit
   * @param v Degrees of freedom
   * @return p-Value
   */
  public static double calculatePValue(double t, int v) {
    return 2 * (1 - StudentsTDistribution.cdf(Math.abs(t), v));
  }

  /**
   * Parameterizer, to use the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected WelchTTest makeInstance() {
      return STATIC;
    }
  }
}