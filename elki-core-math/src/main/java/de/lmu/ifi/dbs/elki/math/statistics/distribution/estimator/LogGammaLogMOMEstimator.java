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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogGammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Simple parameter estimation for the Gamma distribution.
 * 
 * This is a very naive estimation, based on the mean and variance of the log
 * transformed values.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has LogGammaDistribution - - estimates
 */
public class LogGammaLogMOMEstimator extends AbstractLogMeanVarianceEstimator<LogGammaDistribution> {
  /**
   * Static estimation using just the mean and variance.
   */
  public static final LogGammaLogMOMEstimator STATIC = new LogGammaLogMOMEstimator();

  /**
   * Private constructor: use static instance.
   */
  private LogGammaLogMOMEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public LogGammaDistribution estimateFromLogMeanVariance(MeanVariance mv, double shift) {
    final double mu = mv.getMean();
    final double var = mv.getSampleVariance();
    if (mu < Double.MIN_NORMAL || var < Double.MIN_NORMAL) {
      throw new ArithmeticException("Cannot estimate Gamma parameters on a distribution with zero mean or variance: " + mv.toString());
    }
    final double theta = mu / var;
    final double k = mu * theta;
    if (!(k > 0.) || !(theta > 0.)) {
      throw new ArithmeticException("LogGamma estimation produced non-positive parameter values: k=" + k + " theta=" + theta);
    }
    return new LogGammaDistribution(k, theta, shift - 1);
  }

  @Override
  public Class<? super LogGammaDistribution> getDistributionClass() {
    return LogGammaDistribution.class;
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
    protected LogGammaLogMOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
