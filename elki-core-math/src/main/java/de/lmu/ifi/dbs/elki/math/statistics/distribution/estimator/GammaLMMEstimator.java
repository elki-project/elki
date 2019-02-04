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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the parameters of a Gamma Distribution, using the methods of
 * L-Moments (LMM).
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking<br>
 * Fortran routines for use with the method of L-moments Version 3.03<br>
 * IBM Research.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - GammaDistribution
 */
@Reference(authors = "J. R. M. Hosking", //
    title = "Fortran routines for use with the method of L-moments Version 3.03", //
    booktitle = "IBM Research Technical Report", //
    bibkey = "tr/ibm/Hosking00")
public class GammaLMMEstimator implements LMMDistributionEstimator<GammaDistribution> {
  /**
   * Static instance.
   */
  public static final GammaLMMEstimator STATIC = new GammaLMMEstimator();

  /** Coefficients for polynomial approximation */
  private static double //
  A1 = -0.3080, //
      A2 = -0.05812, //
      A3 = 0.01765;

  /** Coefficients for polynomial approximation */
  private static double //
  B1 = 0.7213, //
      B2 = -0.5947, //
      B3 = -2.1817, //
      B4 = 1.2113;

  /**
   * Constructor. Private: use static instance.
   */
  private GammaLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 2;
  }

  @Override
  public GammaDistribution estimateFromLMoments(double[] xmom) {
    double cv = xmom[1] / xmom[0];
    double alpha;
    if(cv < .5) {
      double t = Math.PI * cv * cv;
      alpha = (1. + A1 * t) / (t * (1. + t * (A2 + t * A3)));
    }
    else {
      double t = 1. - cv;
      alpha = t * (B1 + t * B2) / (1. + t * (B3 + t * B4));
    }
    final double theta = alpha / xmom[0];
    if(!(alpha > 0.0) || !(theta > 0.0)) {
      throw new ArithmeticException("Gamma estimation produced non-positive parameter values: k=" + alpha + " theta=" + theta);
    }
    return new GammaDistribution(alpha, theta);
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
    protected GammaLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
