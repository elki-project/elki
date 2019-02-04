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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.InverseGaussianDistribution;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate parameter of the inverse Gaussian (Wald) distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - InverseGaussianDistribution
 */
@Alias("de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator.WaldMOMEstimator")
public class InverseGaussianMOMEstimator implements MeanVarianceDistributionEstimator<InverseGaussianDistribution> {
  /**
   * Static instance.
   */
  public static final InverseGaussianMOMEstimator STATIC = new InverseGaussianMOMEstimator();

  @Override
  public InverseGaussianDistribution estimateFromMeanVariance(MeanVariance mv) {
    double mean = mv.getMean();
    return new InverseGaussianDistribution(mean, mean * mean * mean / mv.getSampleVariance());
  }

  @Override
  public Class<? super InverseGaussianDistribution> getDistributionClass() {
    return InverseGaussianDistribution.class;
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected InverseGaussianMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
