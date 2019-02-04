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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Estimate distribution parameters using the method by Choi and Wette.
 * <p>
 * Reference:
 * <p>
 * Maximum likelihood estimation of the parameters of the gamma distribution and
 * their bias<br>
 * S. C. Choi, R. Wette<br>
 * Technometrics
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - GammaDistribution
 */
@Reference(title = "Maximum likelihood estimation of the parameters of the gamma distribution and their bias", //
    authors = "S. C. Choi, R. Wette", //
    booktitle = "Technometrics", //
    url = "https://doi.org/10.2307/1266892", //
    bibkey = "doi:10.2307/1266892")
public class GammaChoiWetteEstimator implements DistributionEstimator<GammaDistribution> {
  /**
   * Static estimation, using iterative refinement.
   */
  public static final GammaChoiWetteEstimator STATIC = new GammaChoiWetteEstimator();

  /**
   * Private constructor.
   */
  private GammaChoiWetteEstimator() {
    // Do not instantiate - use static class
  }

  @Override
  public <A> GammaDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double meanx = 0, meanlogx = 0;
    for(int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      if(val <= 0 || Double.isInfinite(val) || Double.isNaN(val)) {
        continue;
      }
      final double logx = (val > 0) ? FastMath.log(val) : meanlogx;
      final double deltax = val - meanx;
      final double deltalogx = logx - meanlogx;
      meanx += deltax / (i + 1.);
      meanlogx += deltalogx / (i + 1.);
    }
    // Initial approximation
    final double logmeanx = (meanx > 0) ? FastMath.log(meanx) : meanlogx;
    final double diff = logmeanx - meanlogx;
    double k = (3 - diff + FastMath.sqrt((diff - 3) * (diff - 3) + 24 * diff)) / (12 * diff);

    // Refine via newton iteration, based on Choi and Wette equation
    while(true) {
      double kdelta = (FastMath.log(k) - GammaDistribution.digamma(k) - diff) / (1 / k - GammaDistribution.trigamma(k));
      if(Math.abs(kdelta) / k < 1E-8 || !(kdelta < Double.POSITIVE_INFINITY)) {
        break;
      }
      k += kdelta;
    }
    // Estimate theta:
    final double theta = k / meanx;
    if(!(k > 0.0) || !(theta > 0.0)) {
      throw new ArithmeticException("Gamma estimation produced non-positive parameter values: k=" + k + " theta=" + theta);
    }
    return new GammaDistribution(k, theta);
  }

  @Override
  public Class<? super GammaDistribution> getDistributionClass() {
    return GammaDistribution.class;
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
    protected GammaChoiWetteEstimator makeInstance() {
      return STATIC;
    }
  }
}
