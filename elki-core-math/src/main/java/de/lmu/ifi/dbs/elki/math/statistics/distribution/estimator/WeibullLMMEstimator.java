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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.WeibullDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Estimate parameters of the Weibull distribution using the method of L-Moments
 * (LMM).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - WeibullDistribution
 */
public class WeibullLMMEstimator implements LMMDistributionEstimator<WeibullDistribution> {
  /**
   * Static instance.
   */
  public static final WeibullLMMEstimator STATIC = new WeibullLMMEstimator();

  /** Estimation constants */
  private static final double A0 = GeneralizedExtremeValueLMMEstimator.A0,
      A1 = GeneralizedExtremeValueLMMEstimator.A1,
      A2 = GeneralizedExtremeValueLMMEstimator.A2,
      A3 = GeneralizedExtremeValueLMMEstimator.A3,
      A4 = GeneralizedExtremeValueLMMEstimator.A4;

  /** Estimation constants */
  private static final double B1 = GeneralizedExtremeValueLMMEstimator.B1,
      B2 = GeneralizedExtremeValueLMMEstimator.B2,
      B3 = GeneralizedExtremeValueLMMEstimator.B3;

  /** Estimation constants */
  private static final double C1 = GeneralizedExtremeValueLMMEstimator.C1,
      C2 = GeneralizedExtremeValueLMMEstimator.C2,
      C3 = GeneralizedExtremeValueLMMEstimator.C3;

  /** Estimation constants */
  private static final double D1 = GeneralizedExtremeValueLMMEstimator.D1,
      D2 = GeneralizedExtremeValueLMMEstimator.D2;

  /** Maximum number of iterations. */
  static int MAXIT = 20;

  /**
   * Constructor. Private: use static instance!
   */
  private WeibullLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public WeibullDistribution estimateFromLMoments(double[] xmom) {
    /*
     * double l = xmom[2], l2 = l * l, l3 = l2 * l, l4 = l3 * l, l5 = l4 * l, l6
     * = l5 * l; double k = 285.3 * l6 - 658.6 * l5 + 622.8 * l4 - 317.2 * l3 +
     * 98.52 * l2 - 21.256 * l + 3.516;
     * 
     * double gam = GammaDistribution.gamma(1. + 1. / k); double lambda =
     * xmom[1] / (1. - Math.pow(2., -1. / k) * gam); double mu = xmom[0] -
     * lambda * gam;
     * 
     * return new WeibullDistribution(k, lambda, mu);
     */
    double t3 = -xmom[2];
    if(Math.abs(t3) < 1e-50 || (t3 >= 1.)) {
      throw new ArithmeticException("Invalid moment estimation.");
    }
    // Approximation for t3 between 0 and 1:
    double g;
    if(t3 > 0.) {
      double z = 1. - t3;
      g = (-1. + z * (C1 + z * (C2 + z * C3))) / (1. + z * (D1 + z * D2));
    }
    else {
      // Approximation for t3 between -.8 and 0L:
      g = (A0 + t3 * (A1 + t3 * (A2 + t3 * (A3 + t3 * A4)))) / (1. + t3 * (B1 + t3 * (B2 + t3 * B3)));
      if(t3 < -.8) {
        // Newton-Raphson iteration for t3 < -.8
        if(t3 <= -.97) {
          g = 1. - FastMath.log1p(t3) * MathUtil.ONE_BY_LOG2;
        }
        double t0 = .5 * (t3 + 3.);
        for(int it = 1;; it++) {
          double x2 = FastMath.pow(2., -g), xx2 = 1. - x2;
          double x3 = FastMath.pow(3., -g), xx3 = 1. - x3;
          double t = xx3 / xx2;
          double deriv = (xx2 * x3 * MathUtil.LOG3 - xx3 * x2 * MathUtil.LOG2) / (xx2 * xx2);
          double oldg = g;
          g -= (t - t0) / deriv;
          if(Math.abs(g - oldg) < 1e-14 * g) {
            break;
          }
          if(it >= MAXIT) {
            throw new ArithmeticException("Newton-Raphson did not converge.");
          }
        }
      }
    }
    double gam = FastMath.exp(GammaDistribution.logGamma(1. + g));
    final double mu, sigma, k;
    k = 1. / g;
    sigma = xmom[1] / (gam * (1. - FastMath.pow(2., -g)));
    mu = -xmom[0] + sigma * gam;
    return new WeibullDistribution(k, sigma, mu);
  }

  @Override
  public Class<? super WeibullDistribution> getDistributionClass() {
    return WeibullDistribution.class;
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
    protected WeibullLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
