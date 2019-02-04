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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ExponentialDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate Exponential distribution parameters using the mean, which is the
 * maximum-likelihood estimate (MLE), but not very robust.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - ExponentialDistribution
 */
public class ExponentialMOMEstimator implements MeanVarianceDistributionEstimator<ExponentialDistribution> {
  /**
   * Static instance.
   */
  public static final ExponentialMOMEstimator STATIC = new ExponentialMOMEstimator();

  /**
   * Private constructor, use static instance!
   */
  private ExponentialMOMEstimator() {
    // Do not instantiate
  }

  @Override
  public ExponentialDistribution estimateFromMeanVariance(MeanVariance mv) {
    final double scale = mv.getMean();
    if(!(scale > 0.)) {
      throw new ArithmeticException("Data with non-positive mean cannot be exponential distributed.");
    }
    return new ExponentialDistribution(1. / scale);
  }

  @Override
  public Class<? super ExponentialDistribution> getDistributionClass() {
    return ExponentialDistribution.class;
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
    protected ExponentialMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
