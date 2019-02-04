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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.SkewGeneralizedNormalDistribution;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Estimate the parameters of a skew Normal Distribution (Hoskin's Generalized
 * Normal Distribution), using the methods of L-Moments (LMM).
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
 * @has - - - SkewGeneralizedNormalDistribution
 */
@Reference(authors = "J. R. M. Hosking", //
    title = "Fortran routines for use with the method of L-moments Version 3.03", //
    booktitle = "IBM Research Technical Report", //
    bibkey = "tr/ibm/Hosking00")
public class SkewGNormalLMMEstimator implements LMMDistributionEstimator<SkewGeneralizedNormalDistribution> {
  /**
   * Static instance.
   */
  public static final SkewGNormalLMMEstimator STATIC = new SkewGNormalLMMEstimator();

  /** Polynomial approximation */
  private static final double //
  A0 = 0.20466534e+01, //
      A1 = -0.36544371e+01, //
      A2 = 0.18396733e+01, //
      A3 = -0.20360244;

  /** Polynomial approximation */
  private static final double //
  B1 = -0.20182173e+01, //
      B2 = 0.12420401e+01, //
      B3 = -0.21741801;

  /**
   * Constructor. Private: use static instance.
   */
  private SkewGNormalLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 3;
  }

  @Override
  public SkewGeneralizedNormalDistribution estimateFromLMoments(double[] xmom) {
    if(!(xmom[1] > 0.) || !(Math.abs(xmom[2]) < 1.0)) {
      throw new ArithmeticException("L-Moments invalid");
    }
    // Generalized Normal Distribution estimation:
    double t3 = xmom[2];
    if(Math.abs(t3) >= .95) {
      // Extreme skewness
      return new SkewGeneralizedNormalDistribution(0., -1., 0.);
    }
    if(Math.abs(t3) <= 1e-8) {
      // t3 effectively zero, this is a normal distribution.
      return new SkewGeneralizedNormalDistribution(xmom[0], xmom[1] * MathUtil.SQRTPI, 0.);
    }
    final double tt = t3 * t3;
    double shape = -t3 * (A0 + tt * (A1 + tt * (A2 + tt * A3))) / (1. + tt * (B1 + tt * (B2 + tt * B3)));
    final double e = FastMath.exp(.5 * shape * shape);
    double scale = xmom[1] * shape / (e * NormalDistribution.erf(.5 * shape));
    double location = xmom[0] + scale * (e - 1.) / shape;
    return new SkewGeneralizedNormalDistribution(location, scale, shape);
  }

  @Override
  public Class<? super SkewGeneralizedNormalDistribution> getDistributionClass() {
    return SkewGeneralizedNormalDistribution.class;
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
    protected SkewGNormalLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
