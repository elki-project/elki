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

import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Generalized logistic distribution. (Type I, Skew-logistic distribution)
 * <p>
 * One of multiple ways of generalizing the logistic distribution.
 * <br>
 * {@code pdf(x) = shape * exp(-x) / (1 + exp(-x))**(shape+1)}
 * <br>
 * {@code cdf(x) = pow(1+exp(-x), -shape)}
 * <br>
 * Where {@code shape=1} yields the regular logistic distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class GeneralizedLogisticDistribution implements Distribution {
  /**
   * Parameters: location and scale
   */
  double location, scale;

  /**
   * Shape parameter, for generalized logistic distribution.
   */
  double shape;

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   * @param shape Shape parameter
   */
  public GeneralizedLogisticDistribution(double location, double scale, double shape) {
    this.location = location;
    this.scale = scale;
    this.shape = shape;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, location, scale, shape);
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return PDF
   */
  public static double pdf(double val, double loc, double scale, double shape) {
    if(!(val < Double.POSITIVE_INFINITY && val > Double.NEGATIVE_INFINITY)) {
      return val == val ? 0. : Double.NaN;
    }
    val = (val - loc) / scale;
    double e = FastMath.exp(-val);
    double f = 1. + e;
    return e < Double.POSITIVE_INFINITY ? shape * e / (scale * FastMath.pow(f, shape + 1.)) : 0.;
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, location, scale, shape);
  }

  /**
   * log Probability density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return log PDF
   */
  public static double logpdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    double e = FastMath.exp(-val);
    return -(val + (shape + 1.0) * FastMath.log1p(e)) + FastMath.log(shape);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, location, scale, shape);
  }

  /**
   * Cumulative density function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return CDF
   */
  public static double cdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    return FastMath.pow(1. + FastMath.exp(-val), -shape);
  }

  /**
   * log Cumulative density function.
   * 
   * TODO: untested.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return log PDF
   */
  public static double logcdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    return FastMath.log1p(FastMath.exp(-val)) * -shape;
  }

  /**
   * Quantile function.
   * 
   * @param val Value
   * @param loc Location
   * @param scale Scale
   * @param shape Shape
   * @return Quantile
   */
  public static double quantile(double val, double loc, double scale, double shape) {
    return loc - scale * FastMath.log(FastMath.exp(FastMath.log(val) / -shape) - 1);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, location, scale, shape);
  }

  @Override
  public double nextRandom(Random random) {
    double u = random.nextDouble();
    return location - scale * FastMath.log(FastMath.exp(FastMath.log(u) / -shape) - 1);
  }

  @Override
  public String toString() {
    return "GeneralizedLogisticDistribution(location=" + location + ", scale=" + scale + ", shape=" + shape + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Distribution.Parameterizer {
    /** Parameters. */
    double location, scale, shape;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(LOCATION_ID) //
          .grab(config, x -> location = x);
      new DoubleParameter(SCALE_ID) //
          .grab(config, x -> scale = x);
      new DoubleParameter(SHAPE_ID) //
          .grab(config, x -> shape = x);
    }

    @Override
    public GeneralizedLogisticDistribution make() {
      return new GeneralizedLogisticDistribution(location, scale, shape);
    }
  }
}
