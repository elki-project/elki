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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Perform a two-sample Anderson-Darling rank test, and standardize the
 * statistic according to Scholz and Stephens. Ties are handled as discussed in
 * Equation 7 of Scholz and Stephens.
 * <p>
 * To access the non-standardized A2 scores, use the function
 * {@link #unstandardized}.
 * <p>
 * Compared to the Cramer-van Mises test, the Anderson-Darling test puts more
 * weight on the tail of the distribution. This variant only uses the ranks.
 * <p>
 * References:
 * <p>
 * Darling's note on this equation
 * <p>
 * D. A. Darling<br>
 * The Kolmogorov-Smirnov, Cramer-von Mises tests.<br>
 * Annals of Mathematical Statistics 28(4)
 * <p>
 * More detailed discussion by Pettitt
 * <p>
 * A. N. Pettitt<br>
 * A two-sample Anderson-Darling rank statistic<br>
 * Biometrika 63 (1)
 * <p>
 * F. W. Scholz, M. A. Stephens<br>
 * K-sample Anderson–Darling tests<br>
 * Journal of the American Statistical Association, 82(399)
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "F. W. Scholz, M. A. Stephens", //
    title = "K-sample Anderson–Darling tests", //
    booktitle = "Journal of the American Statistical Association, 82(399)", //
    url = "https://doi.org/10.1080/01621459.1987.10478517", //
    bibkey = "doi:10.1080/01621459.1987.10478517")
@Reference(authors = "D. A. Darling", //
    title = "The Kolmogorov-Smirnov, Cramer-von Mises tests", //
    booktitle = "Annals of mathematical statistics 28(4)", //
    url = "https://doi.org/10.1214/aoms/1177706788", //
    bibkey = "doi:10.1214/aoms/1177706788")
@Reference(authors = "A. N. Pettitt", //
    title = "A two-sample Anderson-Darling rank statistic", //
    booktitle = "Biometrika 63 (1)", //
    url = "https://doi.org/10.1093/biomet/63.1.161", //
    bibkey = "doi:10.1093/biomet/63.1.161")
public class StandardizedTwoSampleAndersonDarlingTest implements GoodnessOfFitTest {
  /**
   * Static instance.
   */
  public static final StandardizedTwoSampleAndersonDarlingTest STATIC = new StandardizedTwoSampleAndersonDarlingTest();

  @Override
  public double deviation(double[] sample1, double[] sample2) {
    final double A2 = unstandardized(sample1, sample2);
    final int N = sample1.length + sample2.length;
    final double H = 1. / sample1.length + 1. / sample2.length;
    // Compute g and h in a single pass:
    double g = 0., h = 1. / (N - 1);
    for(int i = N - 2; i > 0; i--) {
      g += h / (N - i);
      h += 1. / i;
    }
    final double a = 4 * g - 6 + (10 - 6 * g) * H;
    final double b = 12 * g - 22 + 8 * h + (2 * g - 14 * h - 4) * H;
    final double c = 36 * h + 4 + (2 * h - 6) * H;
    final double d = 24;
    final double N2 = N * N, N3 = N2 * N;
    final double sigmasq = (a * N3 + b * N2 + c * N + d) / ((N - 1.) * (N - 2.) * (N - 3.));
    return sigmasq > 0 ? (A2 - (2 - 1)) / Math.sqrt(sigmasq) : 0.;
  }

  /**
   * K-samples version of the Anderson-Darling test.
   *
   * @param samples Samples
   * @return A2 score
   */
  public double deviation(double[][] samples) {
    final int N = totalLength(samples);
    final double A2 = unstandardized(samples, N);
    final int k = samples.length;
    double H = 0.;
    for(double[] sample : samples) {
      H += 1. / sample.length;
    }
    // Compute g and h in a single pass:
    double g = 0., h = 1. / (N - 1);
    for(int i = N - 2; i > 0; i--) {
      g += h / (N - i);
      h += 1. / i;
    }
    final int k2 = k * k;
    final double hk = h * k;
    final double a = (4 * g - 6) * (k - 1) + (10 - 6 * g) * H;
    final double b = (2 * g - 4) * k2 + 8 * hk + (2 * g - 14 * h - 4) * H - 8 * h + 4 * g - 6;
    final double c = (6 * h + 2 * g - 2) * k2 + (4 * h - 4 * g + 6) * k + (2 * h - 6) * H + 4 * h;
    final double d = (2 * h + 6) * k2 - 4 * hk;
    final double N2 = N * N, N3 = N2 * N;
    final double sigmasq = (a * N3 + b * N2 + c * N + d) / ((N - 1.) * (N - 2.) * (N - 3.));
    return sigmasq > 0 ? (A2 - (k - 1)) / Math.sqrt(sigmasq) : 0.;
  }

  /**
   * Compute the non-standardized A2 test statistic for the k-samples test.
   *
   * @param samples Samples
   * @return Test statistic
   */
  public double unstandardized(double[][] samples) {
    final int N = totalLength(samples);
    return unstandardized(samples, N);
  }

  /**
   * Compute the non-standardized A2 test statistic for the k-samples test.
   * <p>
   * This is based on Scholz and Stephens, Equation 7.
   *
   * @param samples Samples
   * @param N total length
   * @return Test statistic
   */
  private double unstandardized(double[][] samples, int N) {
    final int k = samples.length;
    // Sort the sample data (API allows mutation!), and join:
    final double[] combined = new double[N];
    {
      int p = 0;
      for(double[] samp : samples) {
        Arrays.sort(samp);
        System.arraycopy(samp, 0, combined, p, samp.length);
        p += samp.length;
      }
      assert (p == N);
    }
    // Sort joined data
    Arrays.sort(combined);

    int[] m = new int[k];
    double[] Ak = new double[k];
    for(int j = 0; j < N;) {
      // Find multiplicity in combined sample:
      final double x = combined[j++];
      int lj = 1; // Count multiplicity:
      while(j < N && combined[j] == x) {
        ++j;
        ++lj;
      }
      // Process each sample:
      for(int i = 0; i < k; i++) {
        final double[] sam = samples[i];
        // Multiplicity in sample:
        int mi = m[i];
        assert (mi >= sam.length || sam[mi] >= x);
        int fi = 0; // Multiplicity
        while(mi < sam.length && sam[mi] == x) {
          ++mi;
          ++fi;
        }
        if(fi > 0) {
          assert (m[i] + fi == mi);
          m[i] = mi; // Store
        }
        double bi = j - .5 * lj;
        double v = N * (mi - .5 * fi) - sam.length * bi;
        Ak[i] += lj * v * v / (bi * (N - bi) - .25 * N * lj);
      }
    }
    double A2 = 0.;
    for(int j = 0; j < k; j++) {
      A2 += Ak[j] / samples[j].length;
    }
    A2 *= (N - 1.) / (N * N);
    return A2;
  }

  /**
   * Compute the non-standardized A2 test statistic for the k-samples test.
   * <p>
   * This is based on Scholz and Stephens, Equation 7.
   *
   * @param sample1 First sample
   * @param sample2 Second sample
   * @return Test statistic
   */
  public double unstandardized(double[] sample1, double[] sample2) {
    final int n1 = sample1.length, n2 = sample2.length, N = n1 + n2;
    // Sort the sample data (API allows mutation!), and join:
    final double[] combined = new double[N];
    Arrays.sort(sample1);
    System.arraycopy(sample1, 0, combined, 0, n1);
    Arrays.sort(sample2);
    System.arraycopy(sample2, 0, combined, n1, n2);
    // Sort joined data
    Arrays.sort(combined);

    int m1 = 0, m2 = 0; // Current position
    double Ak1 = 0., Ak2 = 0.; // Aggregates
    for(int j = 0; j < N;) {
      // Find multiplicity in combined sample:
      final double x = combined[j++];
      int lj = 1; // Count multiplicity:
      while(j < N && combined[j] == x) {
        ++j;
        ++lj;
      }
      final double bi = j - .5 * lj;
      {// First sample:
        assert (m1 >= n1 || sample1[m1] >= x);
        int f1 = 0; // Multiplicity
        while(m1 < n1 && sample1[m1] == x) {
          ++m1;
          ++f1;
        }
        double v = N * (m1 - .5 * f1) - n1 * bi;
        Ak1 += lj * v * v / (bi * (N - bi) - .25 * N * lj);
      }
      { // Second sample
        assert (m2 >= n2 || sample2[m2] >= x);
        int f2 = 0; // Multiplicity
        while(m2 < n2 && sample2[m2] == x) {
          ++m2;
          ++f2;
        }
        double v = N * (m2 - .5 * f2) - n2 * bi;
        Ak2 += lj * v * v / (bi * (N - bi) - .25 * N * lj);
      }
    }
    double A2 = Ak1 / n1 + Ak2 / n2;
    A2 *= (N - 1.) / (N * N);
    return A2;
  }

  /**
   * Total length of a set of Samples.
   *
   * @param samples Samples
   * @return Sum of the lengths.
   */
  private int totalLength(double[][] samples) {
    int N = 0;
    for(double[] samp : samples) {
      N += samp.length;
    }
    return N;
  }
}
