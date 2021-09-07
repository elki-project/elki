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
package elki.math.statistics.tests;

import elki.math.statistics.distribution.NormalDistribution;
import elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Perform Anderson-Darling test for a Gaussian distribution.
 * <p>
 * This is a test <em>against</em> normality / goodness of fit. I.e. you can use
 * it to <em>reject</em> the hypothesis that the data is normal distributed.
 * Such tests are sensitive to data set size: on small samples, even large
 * deviations could be by-chance and thus not allow rejection. On the other
 * hand, on large data sets even a slight deviation can be unlikely to happen if
 * the data were indeed normal distributed. Thus, this test is more likely to
 * fail to reject small data sets even when they intuitively do not appear to be
 * normal distributed, while it will reject large data sets that originate from
 * a distribution only slightly different from the normal distribution.
 * <p>
 * Before using, make sure you have understood statistical tests, and the
 * difference between failure-to-reject and acceptance!
 * <p>
 * The data size should be at least 8 before the results start getting somewhat
 * reliable. For large data sets, the chance of rejecting the normal
 * distribution hypothesis increases a lot: no real data looks exactly like a
 * normal distribution.
 * <p>
 * References:
 * <p>
 * T. W. Anderson, D. A. Darling<br>
 * Asymptotic theory of certain 'goodness of fit' criteria based on stochastic
 * processes<br>
 * Annals of mathematical statistics 23(2)
 * <p>
 * M. A. Stephens<br>
 * EDF Statistics for Goodness of Fit and Some Comparisons<br>
 * Journal of the American Statistical Association 69(347)
 * <p>
 * Lorentz Jäntschi and Sorana D. Bolboacă<br>
 * Computation of Probability Associated with Anderson–Darling Statistic<br>
 * Mathematics (MDPI, 2018) <br>
 * 
 * 
 * @author Erich Schubert
 * @author Robert Gehde
 * @since 0.7.0
 */
@Reference(authors = "T. W. Anderson, D. A. Darling", //
    title = "Asymptotic theory of certain 'goodness of fit' criteria based on stochastic processes", //
    booktitle = "Annals of mathematical statistics 23(2)", //
    url = "https://doi.org/10.1214/aoms/1177729437", //
    bibkey = "doi:10.1214/aoms/1177729437")
@Reference(authors = "Jäntschi, Lorentz and Bolboacă, Sorana D.", //
    booktitle = "Mathematics", //
    title = "Computation of Probability Associated with Anderson–Darling Statistic", //
    url = "https://www.mdpi.com/2227-7390/6/6/88")
public class AndersonDarlingTest {
  /**
   * Private constructor. Static methods only.
   */
  private AndersonDarlingTest() {
    // Do not use.
  }

  /**
   * Test a <i>sorted</i> data set against the standard normal distribution.
   * <p>
   * Note: the data will be compared to the standard normal distribution, i.e.
   * with mean 0 and variance 1.
   * <p>
   * The data size should be at least 8 before the results start getting
   * somewhat reliable. For large data sets, the chance of rejecting increases a
   * lot: no real data looks exactly like a normal distribution.
   *
   * @param sorted Sorted input data.
   * @return Test statistic A².
   */
  public static double A2StandardNormal(double[] sorted) {
    final int l = sorted.length;
    assert (l > 0);
    double A2 = 0.;
    // This complicated approach allows us to avoid computing the CDF values
    // twice, nor having to store them.
    int i = 0, j = l - 1;
    int i2 = 1, j2 = (j << 1) + 1;
    for(; i < j; ++i, --j, i2 += 2, j2 -= 2) {
      final double x = NormalDistribution.standardNormalCDF(sorted[i]);
      final double y = NormalDistribution.standardNormalCDF(sorted[j]);
      final double diff1 = FastMath.log(x) + FastMath.log(1 - y);
      final double diff2 = FastMath.log(1 - x) + FastMath.log(y);
      A2 += i2 * diff1 + j2 * diff2;
    }
    if(i == j) {
      final double x = NormalDistribution.standardNormalCDF(sorted[i]);
      A2 += i2 * (FastMath.log(x) + FastMath.log(1 - x));
    }
    A2 /= l;
    A2 += l;
    return -A2;
  }

  /**
   * Test a <i>sorted but not standardized</i> data set.
   * <p>
   * The data size should be at least 8!
   * 
   * @param sorted Sorted input data.
   * @return Test statistic A², after bias removal.
   */
  public static double A2Noncentral(double[] sorted) {
    final int l = sorted.length;
    assert (l > 1);
    double m = 0.;
    for(int i = 0; i < l; ++i) {
      m += sorted[i];
    }
    m /= l; // Mean
    double var = 0.;
    for(int i = 0; i < l; ++i) {
      final double d = sorted[i] - m;
      var += d * d;
    }
    var /= (l - 1); // Variance
    final double isigma = var > 0 ? Math.sqrt(1. / var) : 1.;
    double A2 = 0.;
    // This complicated approach allows us to avoid computing the CDF values
    // twice, nor having to store them.
    int i = 0, j = l - 1;
    int i2 = 1, j2 = (j << 1) + 1;
    for(; i < j; ++i, --j, i2 += 2, j2 -= 2) {
      final double x = NormalDistribution.standardNormalCDF((sorted[i] - m) * isigma);
      final double y = NormalDistribution.standardNormalCDF((sorted[j] - m) * isigma);
      final double diff1 = FastMath.log(x) + FastMath.log(1 - y);
      final double diff2 = FastMath.log(1 - x) + FastMath.log(y);
      A2 += i2 * diff1 + j2 * diff2;
    }
    if(i == j) {
      final double x = NormalDistribution.standardNormalCDF((sorted[i] - m) * isigma);
      A2 += i2 * (FastMath.log(x) + FastMath.log(1 - x));
    }
    A2 /= l;
    A2 += l;
    return -A2;
  }

  /**
   * Remove bias from the Anderson-Darling statistic if the mean and standard
   * deviation were estimated from the data, and a normal distribution was
   * assumed.
   * 
   * @param A2 A2 statistic
   * @param n Sample size
   * @return Unbiased test statistic
   */
  @Reference(authors = "M. A. Stephens", //
      title = "EDF Statistics for Goodness of Fit and Some Comparisons", //
      booktitle = "Journal of the American Statistical Association, Volume 69, Issue 347", //
      url = "https://doi.org/10.1080/01621459.1974.10480196", //
      bibkey = "doi:10.1080/01621459.1974.10480196")
  public static double removeBiasNormalDistribution(double A2, int n) {
    return A2 * (1 + 4. / n - 25. / (n * n));
  }

  /**
   * Calculates the quantile for an Anderson Darling statistic in the case where
   * both center and variance are unknown
   * 
   * @param A2 Anderson Darling statistic
   * @return quantile
   */
  public static double calculateQuantileCase4(double A2) {
    return 1 - pValueCase4(A2);
  }
  
  /**
   * Calculates the p-value for an Anderson Darling statistic in the case where
   * both center and variance are unknown
   * 
   * @param A2 Anderson Darling statistic
   * @return quantile
   */
  private static double pValueCase4(double A2) {
    if(A2 >= 0.6) {
      return Math.exp(1.2937 - 5.709 * A2 + 0.0186 * A2 * A2);
    }
    else if(A2 >= 0.34) {
      return Math.exp(0.9177 - 4.279 * A2 - 1.38 * A2 * A2);
    }
    else if(A2 >= 0.2) {
      return 1 - Math.exp(-8.318 + 42.796 * A2 - 59.938 * A2 * A2);
    }
    else {
      return 1 - Math.exp(-13.436 - 101.14 * A2 + 223.73 * A2 * A2);
    }
  }


  /**
   * Calculates the p-value for an Anderson Darling statistic in the case where
   * both center and variance are known
   * 
   * @param A2 Anderson Darling statistic
   * @param n sample size
   * @return quantile
   */
  public static double calculateQuantileCase0(double A2, int n) {
    return 1 - pValueCase0(A2, n);
  }

  /**
   * Calculates the p-value for an Anderson Darling statistic in the case where
   * both center and variance are known
   * 
   * @param A2 Anderson Darling statistic
   * @param n sample size
   * @return quantile
   */
  private static double pValueCase0(double A2, int n) {
    double x = Math.exp(A2);
    double[] xpows = { 1, //
        Math.pow(x, .25), //
        Math.pow(x, .5), //
        Math.pow(x, .75), //
        x };
    double[] npows = { 1, //
        Math.pow(n, -1), //
        Math.pow(n, -2), //
        Math.pow(n, -3), //
        Math.pow(n, -4) };
    double stat = xpows[0] * (5.6737 * npows[0] - 38.9087 * npows[1] + 88.7461 * npows[2] - 179.547 * npows[3] + 199.3247 * npows[4]) //
        + xpows[1] * (-13.5729 * npows[0] + 83.65 * npows[1] - 181.6768 * npows[2] + 347.6606 * npows[3] - 367.4883 * npows[4]) //
        + xpows[2] * (12.075 * npows[0] - 70.377 * npows[1] + 139.8035 * npows[2] - 245.6051 * npows[3] + 243.5784 * npows[4]) //
        + xpows[3] * (-7.319 * npows[0] + 30.4792 * npows[1] - 49.9105 * npows[2] + 76.7476 * npows[3] - 70.1764 * npows[4])//
        + xpows[4] * (3.7309 * npows[0] - 6.1885 * npows[1] + 7.342 * npows[2] - 9.3021 * npows[3] + 7.7018 * npows[4]);
    return 1.0 / stat;
  }
}
