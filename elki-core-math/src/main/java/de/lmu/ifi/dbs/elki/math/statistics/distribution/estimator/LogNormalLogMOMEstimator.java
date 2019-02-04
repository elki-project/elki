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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogNormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Naive distribution estimation using mean and sample variance.
 * 
 * This is a maximum-likelihood-estimator (MLE).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - LogNormalDistribution
 */
public class LogNormalLogMOMEstimator implements LogMeanVarianceEstimator<LogNormalDistribution> {
  /**
   * Static estimator, using mean and variance.
   */
  public static LogNormalLogMOMEstimator STATIC = new LogNormalLogMOMEstimator();

  /**
   * Private constructor, use static instance!
   */
  private LogNormalLogMOMEstimator() {
    super();
  }

  @Override
  public LogNormalDistribution estimateFromLogMeanVariance(MeanVariance mv, double shift) {
    return new LogNormalDistribution(mv.getMean(), Math.max(mv.getSampleStddev(), Double.MIN_NORMAL), shift);
  }

  @Override
  public Class<? super LogNormalDistribution> getDistributionClass() {
    return LogNormalDistribution.class;
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
    protected LogNormalLogMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
