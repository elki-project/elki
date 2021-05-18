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

import elki.math.MathUtil;
import elki.utilities.Alias;
import elki.utilities.exceptions.NotImplementedException;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Inverse Gaussian distribution aka Wald distribution.
 * <p>
 * Beware that SciPy uses a different location parameter.
 * <p>
 * <code>InverseGaussian(a, x) ~ scipy.stats.invgauss(a/x, x)</code>
 * <p>
 * Our parameter scheme is in line with common literature. SciPy naming scheme
 * has comparable notion of location and scale across distributions. So both
 * have their benefits.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "invgauss", "wald" })
public class InverseGaussianDistribution implements Distribution {
  /**
   * Location value
   */
  private double mean;

  /**
   * Shape parameter
   */
  private double shape;

  /**
   * Constructor for Inverse Gaussian / Wald distribution.
   *
   * @param mean Mean
   * @param shape Shape parameter
   */
  public InverseGaussianDistribution(double mean, double shape) {
    this.mean = mean;
    this.shape = shape;
  }

  /**
   * Mean parameter.
   * 
   * @return Mean
   */
  public double getMean() {
    return mean;
  }

  /**
   * Shape parameter.
   * 
   * @return Shape
   */
  public double getShape() {
    return shape;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, mean, shape);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, mean, shape);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mean, shape);
  }

  /**
   * @deprecated NOT YET IMPLEMENTED.
   */
  @Override
  @Deprecated
  public double quantile(double q) {
    return quantile(q, mean, shape);
  }

  @Override
  public double nextRandom(Random random) {
    double v = random.nextGaussian();
    v *= v;
    double x = mean + mean * .5 / shape * (mean * v - Math.sqrt(4. * mean * shape * v + mean * mean * v * v));
    double u = random.nextDouble();
    if(u * (mean + x) <= mean) {
      return x;
    }
    else {
      return mean * mean / x;
    }
  }

  @Override
  public String toString() {
    return "WaldDistribution(mean=" + mean + ", shape=" + shape + ")";
  }

  /**
   * Probability density function of the Wald distribution.
   *
   * @param x The value.
   * @param mu The mean.
   * @param shape Shape parameter
   * @return PDF of the given Wald distribution at x.
   */
  public static double pdf(double x, double mu, double shape) {
    if(!(x > 0) || x == Double.POSITIVE_INFINITY) {
      return x == x ? 0 : Double.NaN;
    }
    final double v = (x - mu) / mu;
    double t1 = Math.sqrt(shape / (MathUtil.TWOPI * x * x * x));
    return t1 > 0 ? t1 * FastMath.exp(-shape * v * v * .5 / x) : 0;
  }

  /**
   * Probability density function of the Wald distribution.
   *
   * @param x The value.
   * @param mu The mean.
   * @param shape Shape parameter
   * @return log PDF of the given Wald distribution at x.
   */
  public static double logpdf(double x, double mu, double shape) {
    if(!(x > 0) || x == Double.POSITIVE_INFINITY) {
      return x == x ? Double.NEGATIVE_INFINITY : Double.NaN;
    }
    final double v = (x - mu) / mu;
    return v < Double.MAX_VALUE ? 0.5 * FastMath.log(shape / (MathUtil.TWOPI * x * x * x)) - shape * v * v / (2. * x) : Double.NEGATIVE_INFINITY;
  }

  /**
   * Cumulative probability density function (CDF) of a Wald distribution.
   *
   * @param x value to evaluate CDF at
   * @param mu Mean value
   * @param shape Shape parameter
   * @return The CDF of the given Wald distribution at x.
   */
  public static double cdf(double x, double mu, double shape) {
    if(!(x > 0.)) {
      return x == x ? 0. : Double.NaN;
    }
    // TODO: accelerate by caching exp(2 * shape / mu).
    final double v0 = x / mu;
    final double v1 = Math.sqrt(shape / x);
    if(v1 == 0.) {
      return v0 > 0. ? 1 : 0.;
    }
    double c1 = NormalDistribution.standardNormalCDF(v1 * (v0 - 1.));
    double c2 = NormalDistribution.standardNormalCDF(-v1 * (v0 + 1.));
    return (c2 > 0.) ? c1 + FastMath.exp(2 * shape / mu) * c2 : c1;
  }

  /**
   * Inverse cumulative probability density function (probit) of a Wald
   * distribution.
   *
   * @param x value to evaluate probit function at
   * @param mu Mean value
   * @param shape Shape parameter
   * @return The probit of the given Wald distribution at x.
   *
   * @deprecated NOT YET IMPLEMENTED.
   */
  @Deprecated
  public static double quantile(double x, double mu, double shape) {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Par implements Distribution.Parameterizer {
    /** Parameters. */
    double mean, shape;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(LOCATION_ID) //
          .grab(config, x -> mean = x);
      new DoubleParameter(SHAPE_ID) //
          .grab(config, x -> shape = x);
    }

    @Override
    public InverseGaussianDistribution make() {
      return new InverseGaussianDistribution(mean, shape);
    }
  }
}
