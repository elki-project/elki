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
package elki.math.statistics.distribution.estimator;

import elki.math.StatisticalMoments;
import elki.math.statistics.distribution.ExponentiallyModifiedGaussianDistribution;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

import net.jafama.FastMath;

/**
 * Naive distribution estimation using mean and sample variance.
 * <p>
 * Reference:
 * <p>
 * J. Olivier, M. M. Norberg<br>
 * Positively skewed data: Revisiting the Box-Cox power transformation<br>
 * International Journal of Psychological Research 3(1)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - ExponentiallyModifiedGaussianDistribution
 */
@Reference(authors = "J. Olivier, M. M. Norberg", //
    title = "Positively skewed data: Revisiting the Box-Cox power transformation", //
    booktitle = "International Journal of Psychological Research 3(1)", //
    url = "https://doi.org/10.21500/20112084.846", //
    bibkey = "doi:10.21500/20112084.846")
public class EMGOlivierNorbergEstimator implements MOMDistributionEstimator<ExponentiallyModifiedGaussianDistribution> {
  /**
   * Static estimator class.
   */
  public static final EMGOlivierNorbergEstimator STATIC = new EMGOlivierNorbergEstimator();

  /**
   * Private constructor, use static instance!
   */
  private EMGOlivierNorbergEstimator() {
    // Do not instantiate
  }

  @Override
  public ExponentiallyModifiedGaussianDistribution estimateFromStatisticalMoments(StatisticalMoments moments) {
    // Avoid NaN by disallowing negative kurtosis.
    final double halfsk13 = FastMath.pow(Math.max(0., moments.getSampleSkewness() * .5), 1. / 3.);
    final double st = moments.getSampleStddev();
    final double mu = moments.getMean() - st * halfsk13;
    // Note: we added "abs" here, to avoid even more NaNs.
    final double si = st * Math.sqrt(Math.abs((1. + halfsk13) * (1. - halfsk13)));
    // One more workaround to ensure finite lambda...
    final double la = (halfsk13 > 0) ? 1 / (st * halfsk13) : 1;
    return new ExponentiallyModifiedGaussianDistribution(mu, si, la);
  }

  @Override
  public Class<? super ExponentiallyModifiedGaussianDistribution> getDistributionClass() {
    return ExponentiallyModifiedGaussianDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public EMGOlivierNorbergEstimator make() {
      return STATIC;
    }
  }
}
