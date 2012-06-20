package experimentalcode.students.brusis;

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
 * @author Jan Brusis
 * @author Erich Schubert
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
    double[] fullCDF = empiricalCDF(fullSample);
    double[] conditionalCDF = empiricalCDF(conditionalSample);
    return calculateTestStatistic(fullSample, conditionalSample, fullCDF, conditionalCDF);
  }

  /**
   * For every value x in the sample, calculates the percentage of values <= x
   * (the empirical cumulative density function, CDF)
   * 
   * @param sample array with the sample values
   * @return array with the percentages
   */
  public static double[] empiricalCDF(double[] sample) {
    final int size = sample.length;
    final double[] F = new double[size];

    for(int i = 0; i < size;) {
      // Count ties:
      int count = 1;
      final double x = sample[i];
      for(int j = i + 1; j < size; j++) {
        if(sample[j] != x) {
          break;
        }
        count++;
      }

      for(int j = 0; j < count; j++) {
        F[i + j] = ((double) i + count) / size;
      }
      i += count; // Advance
    }

    return F;
  }

  /**
   * Calculates the maximum distance between the two empirical CDFs of two data
   * samples. The sample positions and CDFs must be synchronized!
   * 
   * @param sample1 first data sample positions
   * @param sample2 second data sample positions
   * @param f1 array of percentages for first sample
   * @param f2 array of percentages for second sample
   * @return the largest distance between both functions
   */
  public static double calculateTestStatistic(double[] sample1, double[] sample2, double[] f1, double[] f2) {
    double maximum = 0.0;

    int index1 = 0, index2 = 0;
    double cdf1 = 0, cdf2 = 0;

    // Parallel iteration over both curves. We can stop if we reach either end,
    // As the difference can then only decrease!
    while(index1 < sample1.length && index2 < sample2.length) {
      if(sample1[index1] <= sample2[index2]) {
        // Advance on first curve
        cdf1 = sample1[index1];
        index1++;
      }
      else {
        // Advance on second curve
        cdf2 = sample2[index2];
        index2++;
      }
      maximum = Math.max(maximum, Math.abs(cdf1 - cdf2));
    }

    return maximum;
  }

  /**
   * Parameterizer, to use the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected KolmogorovSmirnovTest makeInstance() {
      return STATIC;
    }
  }
}