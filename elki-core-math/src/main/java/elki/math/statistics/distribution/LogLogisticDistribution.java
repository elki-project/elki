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

import elki.utilities.Alias;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Log-Logistic distribution also known as Fisk distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "fisk", "loglog" })
public class LogLogisticDistribution implements Distribution {
  /**
   * Parameters: scale, location, and shape
   */
  double scale, location, shape;

  /**
   * Constructor.
   * 
   * @param shape Shape
   * @param location Location
   * @param scale Scale
   */
  public LogLogisticDistribution(double shape, double location, double scale) {
    this.shape = shape;
    this.scale = scale;
    this.location = location;
  }

  /**
   * Get the distribution shape.
   * 
   * @return Shape parameter
   */
  public double getShape() {
    return shape;
  }

  /**
   * Get the distribution location.
   * 
   * @return Location parameter
   */
  public double getLocation() {
    return location;
  }

  /**
   * Get the distribution scale.
   * 
   * @return Scale parameter
   */
  public double getScale() {
    return scale;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, shape, location, scale);
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param shape Shape
   * @param location TODO
   * @param scale Scale
   * @return PDF
   */
  public static double pdf(double val, double shape, double location, double scale) {
    if(val < location) {
      return 0;
    }
    val = (val - location) / scale;
    double f = shape / scale * FastMath.pow(val, shape - 1.);
    if(f == Double.POSITIVE_INFINITY) {
      return 0;
    }
    double d = 1. + FastMath.pow(val, shape);
    return f / (d * d);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, shape, location, scale);
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param shape Shape
   * @param location Location
   * @param scale Scale
   * @return logPDF
   */
  public static double logpdf(double val, double shape, double location, double scale) {
    if(val < location) {
      return Double.NEGATIVE_INFINITY;
    }
    val = (val - location) / scale;
    final double lval = FastMath.log(val);
    if(lval == Double.POSITIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    return FastMath.log(shape / scale) + (shape - 1.) * lval //
        - 2. * FastMath.log1p(FastMath.exp(lval * shape));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, shape, location, scale);
  }

  /**
   * Cumulative density function.
   * 
   * @param val Value
   * @param shape Shape
   * @param location Location
   * @param scale Scale
   * @return CDF
   */
  public static double cdf(double val, double shape, double location, double scale) {
    if(val < location) {
      return 0;
    }
    val = (val - location) / scale;
    return 1. / (1. + FastMath.pow(val, -shape));
  }

  @Override
  public double quantile(double val) {
    return quantile(val, shape, location, scale);
  }

  /**
   * Quantile function.
   * 
   * @param val Value
   * @param shape Shape
   * @param location TODO
   * @param scale Scale
   * @return Quantile
   */
  public static double quantile(double val, double shape, double location, double scale) {
    return val < 0 || val > 1 ? Double.NaN : scale * FastMath.pow(val / (1. - val), 1. / shape) + location;
  }

  @Override
  public double nextRandom(Random random) {
    double u = random.nextDouble();
    return scale * FastMath.pow(u / (1. - u), 1. / shape) + location;
  }

  @Override
  public String toString() {
    return "LogLogisticDistribution(shape=" + shape + ", location=" + location + ", scale=" + scale + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Distribution.Parameterizer {
    /** Parameters. */
    double shape, location, scale;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(SHAPE_ID) //
          .grab(config, x -> shape = x);
      new DoubleParameter(LOCATION_ID, 0.) //
          .grab(config, x -> location = x);
      new DoubleParameter(SCALE_ID, 1.) //
          .grab(config, x -> scale = x);
    }

    @Override
    public LogLogisticDistribution make() {
      return new LogLogisticDistribution(shape, location, scale);
    }
  }
}
