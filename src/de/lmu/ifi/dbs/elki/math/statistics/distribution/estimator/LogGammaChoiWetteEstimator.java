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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.LogGammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate distribution parameters using the method by Choi and Wette.
 * 
 * A modified algorithm for LogGamma distributions.
 * 
 * Reference:
 * <p>
 * Maximum likelihood estimation of the parameters of the gamma distribution and
 * their bias<br />
 * S. C. Choi, R. Wette<br />
 * in: Technometrics
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has LogGammaDistribution - - estimates
 */
@Reference(title = "Maximum likelihood estimation of the parameters of the gamma distribution and their bias", authors = "S. C. Choi, R. Wette", booktitle = "Technometrics", url = "http://www.jstor.org/stable/10.2307/1266892")
public class LogGammaChoiWetteEstimator implements DistributionEstimator<LogGammaDistribution> {
  /**
   * Static estimation, using iterative refinement.
   */
  public static final LogGammaChoiWetteEstimator STATIC = new LogGammaChoiWetteEstimator();

  /**
   * Private constructor.
   */
  private LogGammaChoiWetteEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public <A> LogGammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double shift = Double.MAX_VALUE;
    for (int i = 0; i < len; i++) {
      shift = Math.min(shift, adapter.getDouble(data, i));
    }
    shift -= 1; // So no negative values arise after log
    double meanx = 0, meanlogx = 0;
    for (int i = 0; i < len; i++) {
      final double shifted = adapter.getDouble(data, i) - shift;
      final double val = shifted > 1 ? Math.log(shifted) : 1.;
      final double logx = (val > 0) ? Math.log(val) : meanlogx;
      final double deltax = val - meanx;
      final double deltalogx = logx - meanlogx;
      meanx += deltax / (i + 1.);
      meanlogx += deltalogx / (i + 1.);
    }
    if (!(meanx > 0)) {
      throw new ArithmeticException("Cannot estimate LogGamma distribution with mean ");
    }
    // Initial approximation
    final double logmeanx = Math.log(meanx);
    final double diff = logmeanx - meanlogx;
    double k = (3 - diff + Math.sqrt((diff - 3) * (diff - 3) + 24 * diff)) / (12 * diff);

    // Refine via newton iteration, based on Choi and Wette equation
    while (true) {
      double kdelta = (Math.log(k) - GammaDistribution.digamma(k) - diff) / (1 / k - GammaDistribution.trigamma(k));
      if (Math.abs(kdelta) < 1E-8 || Double.isNaN(kdelta)) {
        break;
      }
      k += kdelta;
    }
    if (!(k > 0)) {
      throw new ArithmeticException("LogGamma estimation failed: k <= 0.");
    }
    // Estimate theta:
    final double theta = k / meanx;
    return new LogGammaDistribution(k, theta, shift);
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
    protected LogGammaChoiWetteEstimator makeInstance() {
      return STATIC;
    }
  }
}
