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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GeneralizedExtremeValueDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Estimate the parameters of a Generalized Extreme Value Distribution, using
 * the methods of L-Moments (LMM).
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking, J. R. Wallis, E. F. Wood<br>
 * Estimation of the generalized extreme-value distribution by the method of
 * probability-weighted moments.<br>
 * Technometrics 27.3
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - GeneralizedExtremeValueDistribution
 */
@Reference(authors = "J. R. M. Hosking, J. R. Wallis, E. F. Wood", //
    title = "Estimation of the generalized extreme-value distribution by the method of probability-weighted moments.", //
    booktitle = "Technometrics 27.3", //
    url = "https://doi.org/10.1080/00401706.1985.10488049", //
    bibkey = "doi:10.1080/00401706.1985.10488049")
public class GeneralizedExtremeValueLMMEstimator implements LMMDistributionEstimator<GeneralizedExtremeValueDistribution> {
  /**
   * Static instance.
   */
  public static final GeneralizedExtremeValueLMMEstimator STATIC = new GeneralizedExtremeValueLMMEstimator();

  /**
   * Constants for fast rational approximations.
   */
  protected static final double //
  A0 = 0.28377530, //
      A1 = -1.21096399, //
      A2 = -2.50728214, //
      A3 = -1.13455566, //
      A4 = -0.07138022;

  protected static final double //
  B1 = 2.06189696, //
      B2 = 1.31912239, //
      B3 = 0.25077104;

  protected static final double //
  C1 = 1.59921491, //
      C2 = -0.48832213, //
      C3 = 0.01573152, //
      D1 = -0.64363929, //
      D2 = 0.08985247;

  /**
   * Euler-Mascheroni constant.
   */
  protected static final double EU = 0.57721566490153286;

  /** Maximum number of iterations. */
  static int MAXIT = 20;

  /**
   * Constructor. Private: use static instance.
   */
  private GeneralizedExtremeValueLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public GeneralizedExtremeValueDistribution estimateFromLMoments(double[] xmom) {
    double t3 = xmom[2];
    if(Math.abs(t3) < 1e-50 || (t3 >= 1.)) {
      throw new ArithmeticException("Invalid moment estimation.");
    }
    // Approximation for t3 between 0 and 1:
    double g;
    if(t3 > 0.) {
      double z = 1. - t3;
      g = (-1. + z * (C1 + z * (C2 + z * C3))) / (1. + z * (D1 + z * D2));
      // g: Almost zero?
      if(Math.abs(g) < 1e-50) {
        double k = 0;
        double sigma = xmom[1] * MathUtil.ONE_BY_LOG2;
        double mu = xmom[0] - EU * sigma;
        return new GeneralizedExtremeValueDistribution(mu, sigma, k);
      }
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
          // These sometimes do not converge with FastMath.
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
    k = g;
    sigma = xmom[1] * g / (gam * (1. - FastMath.pow(2., -g)));
    mu = xmom[0] - sigma * (1. - gam) / g;
    return new GeneralizedExtremeValueDistribution(mu, sigma, k);
  }

  @Override
  public Class<? super GeneralizedExtremeValueDistribution> getDistributionClass() {
    return GeneralizedExtremeValueDistribution.class;
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
    protected GeneralizedExtremeValueLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
