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
 * Ralph B. D'Agostino<br>
 * Tests for the Normal Distribution<br>
 * Goodness-of-Fit Techniques
 * <p>
 * L. Jäntschi and S. D. Bolboacă<br>
 * Computation of Probability Associated with Anderson–Darling Statistic<br>
 * Mathematics (MDPI, 2018) <br>
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
public class AndersonDarlingTest {
  /**
   * Private constructor. Static methods only.
   */
  private AndersonDarlingTest() {
    // Do not use.
  }

  /**
   * Cut-off for extreme values.
   */
  private static final double SMALL = FastMath.log(0.00000001);

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
    for(int i = 0; i < l; ++i) {
      final double x = NormalDistribution.standardNormalCDF(sorted[i]);
      final double logx = x > 0.00000001 ? FastMath.log(x) : SMALL;
      final double log1mx = x < 0.99999999 ? FastMath.log(1 - x) : SMALL;
      A2 += ((i << 1) + 1.) / l * logx + (((l - i) << 1) - 1.) / l * log1mx;
    }
    return -l - A2;
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
    final double isigma = var > 0 ? Math.sqrt((l - 1) / var) : 1.;
    double A2 = 0.;
    for(int i = 0; i < l; ++i) {
      final double x = NormalDistribution.standardNormalCDF((sorted[i] - m) * isigma);
      final double logx = x > 0.00000001 ? FastMath.log(x) : SMALL;
      final double log1mx = x < 0.99999999 ? FastMath.log(1 - x) : SMALL;
      A2 += ((i << 1) + 1.) / l * logx + (((l - i) << 1) - 1.) / l * log1mx;
    }
    return -l - A2;
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
   * Remove bias from the Anderson-Darling statistic if the mean and standard
   * deviation were estimated from the data, and a normal distribution was
   * assumed.
   * 
   * @param A2 A2 statistic
   * @param n Sample size
   * @return Unbiased test statistic
   */
  @Reference(authors = "R. B. D'Agostino", //
      title = "Tests for the Normal Distribution", //
      booktitle = "Goodness-of-Fit Techniques", //
      url = "https://doi.org/10.1201/9780203753064-9", //
      bibkey = "doi:10.1201/9780203753064-9")
  public static double removeBiasNormalDistributionDAgostino(double A2, int n) {
    return A2 * (1 + .75 / n + 2.25 / (n * n));
  }

  /**
   * Calculates the quantile for an Anderson Darling statistic in the case where
   * both center and variance are unknown.
   * <p>
   * Note: the equations assume a correction with
   * {@link #removeBiasNormalDistributionDAgostino}.
   * 
   * @param A2 Anderson Darling statistic
   * @return quantile
   */
  public static double calculateQuantileCase3(double A2) {
    return 1 - pValueCase3(A2);
  }

  /**
   * Calculates the p-value for an Anderson Darling statistic in the case where
   * both center and variance are unknown.
   * <p>
   * Note: the equations assume a correction with
   * {@link #removeBiasNormalDistributionDAgostino}.
   * 
   * @param A2 Anderson Darling statistic
   * @return quantile
   */
  @Reference(authors = "R. B. D'Agostino", //
      title = "Tests for the Normal Distribution", //
      booktitle = "Goodness-of-Fit Techniques", //
      url = "https://doi.org/10.1201/9780203753064-9", //
      bibkey = "doi:10.1201/9780203753064-9")
  public static double pValueCase3(double A2) {
    return A2 == Double.POSITIVE_INFINITY ? 0 : //
        A2 >= 0.6 ? FastMath.exp(1.2937 - 5.709 * A2 + 0.0186 * A2 * A2) : //
            A2 >= 0.34 ? FastMath.exp(0.9177 - 4.279 * A2 - 1.38 * A2 * A2) : //
                A2 >= 0.2 ? 1 - FastMath.exp(-8.318 + 42.796 * A2 - 59.938 * A2 * A2) : //
                    1 - FastMath.exp(-13.436 + 101.14 * A2 - 223.73 * A2 * A2);
  }

  /**
   * Calculates the p-value for an Anderson Darling statistic in the case where
   * both center and variance are known.
   * <p>
   * Note: the equations assume a correction with
   * {@link #removeBiasNormalDistributionDAgostino}.
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
   * both center and variance are known.
   * <p>
   * Note: the equations assume a correction with
   * {@link #removeBiasNormalDistributionDAgostino}.
   * <p>
   * Reference:
   * <p>
   * L. Jäntschi and S. D. Bolboacă<br>
   * Computation of Probability Associated with Anderson-Darling Statistic<br>
   * Mathematics 6(6)
   * 
   * @param A2 Anderson Darling statistic
   * @param n sample size
   * @return quantile
   */
  @Reference(authors = "L. Jäntschi and S. D. Bolboacă", //
      booktitle = "Mathematics 6(6)", //
      title = "Computation of Probability Associated with Anderson-Darling Statistic", //
      url = "https://doi.org/10.3390/math6060088", //
      bibkey = "doi:10.3390/math6060088")
  public static double pValueCase0(double A2, int n) {
    if(A2 == Double.POSITIVE_INFINITY) {
      return 0;
    }
    final double x = FastMath.exp(A2), sx = Math.sqrt(x), ssx = Math.sqrt(sx);
    final double npows1 = 1. / n, npows2 = npows1 * npows1,
        npows3 = npows1 * npows2, npows4 = npows2 * npows2;
    final double stat = (5.6737 - 38.9087 * npows1 + 88.7461 * npows2 - 179.547 * npows3 + 199.3247 * npows4) //
        + ssx * (-13.5729 + 83.65 * npows1 - 181.6768 * npows2 + 347.6606 * npows3 - 367.4883 * npows4) //
        + sx * (12.075 - 70.377 * npows1 + 139.8035 * npows2 - 245.6051 * npows3 + 243.5784 * npows4) //
        + sx * ssx * (-7.319 + 30.4792 * npows1 - 49.9105 * npows2 + 76.7476 * npows3 - 70.1764 * npows4)//
        + x * (3.7309 - 6.1885 * npows1 + 7.342 * npows2 - 9.3021 * npows3 + 7.7018 * npows4);
    return 1.0 / stat;
  }
}
