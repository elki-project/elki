/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.distribution;

import java.util.Random;

import elki.math.MathUtil;
import elki.utilities.exceptions.NotImplementedException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Chi distribution.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - ChiSquaredDistribution
 */
public class ChiDistribution implements Distribution {
  /**
   * Degrees of freedom. Usually integer.
   */
  private double dof;

  /**
   * Chi squared distribution (for random generation)
   */
  private ChiSquaredDistribution chisq;

  /**
   * Constructor.
   *
   * @param dof Degrees of freedom. Usually integer.
   */
  public ChiDistribution(double dof) {
    this.dof = dof;
    this.chisq = new ChiSquaredDistribution(dof);
  }

  @Override
  public double nextRandom(Random random) {
    return Math.sqrt(chisq.nextRandom(random));
  }

  @Override
  public double pdf(double val) {
    return pdf(val, dof);
  }

  /**
   * PDF function
   *
   * @param val Value
   * @param dof Degrees of freedom
   * @return Pdf value
   */
  public static double pdf(double val, double dof) {
    if(val <= 0 || val == Double.POSITIVE_INFINITY) {
      return 0.0;
    }
    final double k = dof * .5;
    return FastMath.exp((dof - 1.0) * FastMath.log(val) + (1 - k) * MathUtil.LOG2 - GammaDistribution.logGamma(k) - val * val / 2.);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, dof);
  }

  /**
   * logPDF function
   *
   * @param val Value
   * @param dof Degrees of freedom
   * @return logPdf value
   */
  public static double logpdf(double val, double dof) {
    if(val <= 0 || val == Double.POSITIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    final double k = dof * .5;
    return (dof - 1.0) * FastMath.log(val) + (1 - k) * MathUtil.LOG2 - GammaDistribution.logGamma(k) - val * val / 2.;
  }

  @Override
  public double cdf(double val) {
    return cdf(val, dof);
  }

  /**
   * Cumulative density function.
   *
   * @param val Value
   * @param dof Degrees of freedom.
   * @return CDF value
   */
  public static double cdf(double val, double dof) {
    double v = val * val * .5;
    // regularizedGammaP will return NaN for infinite values!
    return val < 0. ? 0. : v < Double.POSITIVE_INFINITY ? GammaDistribution.regularizedGammaP(dof * .5, v) : v == v ? 1. : Double.NaN;
  }

  // FIXME: implement!
  @Override
  public double quantile(double val) {
    throw new NotImplementedException();
  }

  @Override
  public String toString() {
    return "ChiDistribution(dof=" + dof + ")";
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Degrees of freedom parameter - same as
     * {@link ChiSquaredDistribution.Par#DOF_ID}.
     */
    public static final OptionID DOF_ID = ChiSquaredDistribution.Par.DOF_ID;

    /** Parameters. */
    double dof;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(DOF_ID) //
          .grab(config, x -> dof = x);
    }

    @Override
    public ChiDistribution make() {
      return new ChiDistribution(dof);
    }
  }
}
