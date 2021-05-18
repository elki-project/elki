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
package elki.math.statistics.distribution;

import java.util.Random;

import elki.utilities.exceptions.NotImplementedException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * Student's t distribution.
 *
 * FIXME: add quantile and random function!
 *
 * @author Jan Brusis
 * @since 0.5.0
 */
public class StudentsTDistribution implements Distribution {
  /**
   * Degrees of freedom
   */
  private final int v;

  /**
   * Constructor.
   *
   * @param v Degrees of freedom
   */
  public StudentsTDistribution(int v) {
    this.v = v;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, v);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, v);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, v);
  }

  @Override
  public double quantile(double val) {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  @Override
  public double nextRandom(Random random) {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  /**
   * Static version of the t distribution's PDF.
   *
   * @param val value to evaluate
   * @param v degrees of freedom
   * @return f(val,v)
   */
  public static double pdf(double val, int v) {
    // TODO: improve precision by computing "exp" last?
    return FastMath.exp(GammaDistribution.logGamma((v + 1) * .5) - GammaDistribution.logGamma(v * .5)) //
        * (1 / Math.sqrt(v * Math.PI)) * FastMath.pow(1 + (val * val) / v, -((v + 1) * .5));
  }

  /**
   * Static version of the t distribution's PDF.
   *
   * @param val value to evaluate
   * @param v degrees of freedom
   * @return f(val,v)
   */
  public static double logpdf(double val, int v) {
    return GammaDistribution.logGamma((v + 1) * .5) - GammaDistribution.logGamma(v * .5) //
        - .5 * FastMath.log(v * Math.PI) + FastMath.log1p(val * val / v) * -.5 * (v + 1);
  }

  /**
   * Static version of the CDF of the t-distribution for t &gt; 0
   *
   * @param val value to evaluate
   * @param v degrees of freedom
   * @return F(val, v)
   */
  public static double cdf(double val, int v) {
    double x = v / (val * val + v);
    return 1 - (0.5 * BetaDistribution.regularizedIncBeta(x, v * .5, 0.5));
  }

  @Override
  public String toString() {
    return "StudentsTDistribution(v=" + v + ")";
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Degrees of freedom.
     */
    public static final OptionID NU_ID = new OptionID("distribution.studentst.nu", "Degrees of freedom.");

    /** Parameters. */
    int nu;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(NU_ID) //
          .grab(config, x -> nu = x);
    }

    @Override
    public StudentsTDistribution make() {
      return new StudentsTDistribution(nu);
    }
  }
}
