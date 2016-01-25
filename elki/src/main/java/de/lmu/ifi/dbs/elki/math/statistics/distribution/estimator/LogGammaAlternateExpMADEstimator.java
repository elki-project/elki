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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogGammaAlternateDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Robust parameter estimation for the LogGamma distribution.
 * 
 * A modified algorithm for LogGamma distributions.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.has LogGammaAlternateDistribution - - estimates
 */
public class LogGammaAlternateExpMADEstimator extends AbstractExpMADEstimator<LogGammaAlternateDistribution> {
  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static final LogGammaAlternateExpMADEstimator STATIC = new LogGammaAlternateExpMADEstimator();

  /**
   * Private constructor.
   */
  private LogGammaAlternateExpMADEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public LogGammaAlternateDistribution estimateFromExpMedianMAD(double median, double mad) {
    if (median < Double.MIN_NORMAL) {
      throw new ArithmeticException("Cannot estimate Gamma parameters on a distribution with zero median.");
    }
    if (mad < Double.MIN_NORMAL) {
      throw new ArithmeticException("Cannot estimate Gamma parameters on a distribution with zero MAD.");
    }

    final double b = median / (mad * mad);
    final double k = median * b;
    if (!(k > 0.) || !(b > 0.)) {
      throw new ArithmeticException("LogGammaAlternate estimation produced non-positive parameter values: k=" + k + " b=" + b + " median=" + median + " mad=" + mad);
    }
    return new LogGammaAlternateDistribution(k, Math.log(b), 0.);
  }

  @Override
  public Class<? super LogGammaAlternateDistribution> getDistributionClass() {
    return LogGammaAlternateDistribution.class;
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
    protected LogGammaAlternateExpMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
