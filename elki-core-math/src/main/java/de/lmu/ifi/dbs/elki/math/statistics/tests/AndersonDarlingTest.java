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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
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
 * 
 * @author Erich Schubert
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
}
