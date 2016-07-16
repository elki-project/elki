package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
 * @apiviz.has LogNormalDistribution - - estimates
 */
public class LogNormalLogMOMEstimator extends AbstractLogMeanVarianceEstimator<LogNormalDistribution> {
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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LogNormalLogMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
