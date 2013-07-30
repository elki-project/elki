package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogGammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Robust parameter estimation for the LogGamma distribution.
 * 
 * A modified algorithm for LogGamma distributions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has LogGammaDistribution - - estimates
 */
public class LogGammaLogMADEstimator implements DistributionEstimator<LogGammaDistribution> {
  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static final LogGammaLogMADEstimator STATIC = new LogGammaLogMADEstimator();

  /**
   * Private constructor.
   */
  private LogGammaLogMADEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public <A> LogGammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    // Modifiable copy:
    double[] x = new double[len];
    double shift = Double.MAX_VALUE;
    for (int i = 0; i < len; i++) {
      x[i] = adapter.getDouble(data, i);
      shift = Math.min(shift, x[i]);
    }
    shift -= 1; // So no negative values arise after log
    for (int i = 0; i < len; i++) {
      final double val = x[i] - shift;
      if (val > 1.) {
        x[i] = Math.log(val);
      } else {
        x[i] = 0.;
      }
    }
    double median = QuickSelect.median(x);
    if (!(median > 0)) {
      median = Double.MIN_NORMAL;
    }
    // Compute deviations:
    for (int i = 0; i < len; i++) {
      x[i] = Math.abs(x[i] - median);
    }
    double mad = QuickSelect.median(x);
    if (!(mad > 0)) {
      throw new ArithmeticException("Cannot estimate LogGamma parameters on a distribution with zero MAD.");
    }

    final double theta = (mad * mad) / median;
    final double k = median / theta;
    return new LogGammaDistribution(k, 1 / theta, shift);
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
    protected LogGammaLogMADEstimator makeInstance() {
      return STATIC;
    }
  }
}
