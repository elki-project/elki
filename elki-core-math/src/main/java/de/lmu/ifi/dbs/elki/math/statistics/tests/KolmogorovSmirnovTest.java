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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Kolmogorov-Smirnov test.
 * 
 * Class that tests two given real-valued data samples on whether they might
 * have originated from the same underlying distribution using the
 * Kolmogorov-Smirnov test statistic that compares the two empirical cumulative
 * distribution functions. The KS statistic is defined as the maximum absolute
 * difference of the empirical CDFs.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class KolmogorovSmirnovTest implements GoodnessOfFitTest {
  /**
   * Static instance
   */
  public static KolmogorovSmirnovTest STATIC = new KolmogorovSmirnovTest();

  /**
   * Constructor.
   */
  public KolmogorovSmirnovTest() {
    super();
  }

  @Override
  public double deviation(double[] fullSample, double[] conditionalSample) {
    Arrays.sort(fullSample);
    Arrays.sort(conditionalSample);
    return calculateTestStatistic(fullSample, conditionalSample);
  }

  /**
   * Calculates the maximum distance between the two empirical CDFs of two data
   * samples. The sample positions and CDFs must be synchronized!
   * 
   * @param sample1 first data sample positions
   * @param sample2 second data sample positions
   * @return the largest difference between both functions
   */
  public static double calculateTestStatistic(double[] sample1, double[] sample2) {
    double maximum = 0.0;

    int index1 = 0, index2 = 0;
    double cdf1 = 0, cdf2 = 0;

    // Parallel iteration over both curves. We can stop if we reach either end,
    // As the difference can then only decrease!
    while (index1 < sample1.length && index2 < sample2.length) {
      // Next (!) positions
      final double x1 = sample1[index1], x2 = sample2[index2];
      // Advance on first curve
      if (x1 <= x2) {
        index1++;
        // Handle multiple points with same x:
        while (index1 < sample1.length && sample1[index1] == x1) {
          index1++;
        }
        cdf1 = ((double) index1 + 1.) / (sample1.length + 1.);
      }
      // Advance on second curve
      if (x1 >= x2) {
        index2++;
        // Handle multiple points with same x:
        while (index2 < sample2.length && sample2[index2] == x2) {
          index2++;
        }
        cdf2 = ((double) index2 + 1.) / (sample2.length + 1.);
      }
      maximum = Math.max(maximum, Math.abs(cdf1 - cdf2));
    }

    return maximum;
  }

  /**
   * Parameterizer, to use the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected KolmogorovSmirnovTest makeInstance() {
      return STATIC;
    }
  }

  /**
   * Simplest version of the test: test if a sorted array is approximately
   * uniform distributed on [0:1].
   * 
   * @param test Presorted (!) array
   * @return Maximum deviation from uniform.
   */
  public static double simpleTest(double[] test) {
    // Weibull style empirical quantiles: (i+1) / (n+1)
    double scale = 1. / (test.length + 1.);
    double maxdev = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < test.length; i++) {
      // Expected value at position i (Weibull style):
      double expected = (i + 1.) * scale;
      double dev = Math.abs(test[i] - expected);
      if (dev > maxdev) {
        maxdev = dev;
      }
    }
    return Math.abs(maxdev);
  }

  /**
   * Simplest version of the test: test if a sorted array is approximately
   * uniform distributed on the given interval.
   * 
   * @param test Presorted (!) array
   * @param min Minimum of uniform distribution
   * @param max Maximum of uniform distribution
   * @return Maximum deviation from uniform.
   */
  public static double simpleTest(double[] test, final double min, final double max) {
    double scale = (max - min) / (test.length + 1.);
    double maxdev = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < test.length; i++) {
      // Expected value at position i (Weibull style):
      double expected = (i + 1.) * scale + min;
      double dev = Math.abs(test[i] - expected);
      if (dev > maxdev) {
        maxdev = dev;
      }
    }
    return Math.abs(maxdev);
  }
}
