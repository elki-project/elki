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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Naive maximum-likelihood estimations for the normal distribution using mean
 * and sample variance.
 * 
 * While this is the most commonly used estimator, it is not very robust against
 * extreme values.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - NormalDistribution
 */
public class NormalMOMEstimator implements MeanVarianceDistributionEstimator<NormalDistribution> {
  /**
   * Static estimator, using mean and variance.
   */
  public static NormalMOMEstimator STATIC = new NormalMOMEstimator();

  /**
   * Private constructor, use static instance!
   */
  private NormalMOMEstimator() {
    // Do not instantiate
  }

  @Override
  public NormalDistribution estimateFromMeanVariance(MeanVariance mv) {
    return new NormalDistribution(mv.getMean(), Math.max(mv.getSampleStddev(), Double.MIN_NORMAL));
  }

  @Override
  public Class<? super NormalDistribution> getDistributionClass() {
    return NormalDistribution.class;
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
    protected NormalMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
