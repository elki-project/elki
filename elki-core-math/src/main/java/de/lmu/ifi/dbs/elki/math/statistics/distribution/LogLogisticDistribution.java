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
package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import java.util.Random;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Log-Logistic distribution also known as Fisk distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "fisk", "loglog" })
public class LogLogisticDistribution extends AbstractDistribution {
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
    this(shape, location, scale, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param shape Shape
   * @param location TODO
   * @param scale Scale
   * @param random Random number generator
   */
  public LogLogisticDistribution(double shape, double location, double scale, Random random) {
    super(random);
    this.shape = shape;
    this.scale = scale;
    this.location = location;
  }

  /**
   * Constructor.
   * 
   * @param shape Shape
   * @param location TODO
   * @param scale Scale
   * @param random Random number generator
   */
  public LogLogisticDistribution(double shape, double location, double scale, RandomFactory random) {
    super(random);
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
  public double nextRandom() {
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
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double shape, location, scale;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter shapeP = new DoubleParameter(SHAPE_ID);
      if(config.grab(shapeP)) {
        shape = shapeP.doubleValue();
      }

      DoubleParameter locationP = new DoubleParameter(LOCATION_ID, 0.);
      if(config.grab(locationP)) {
        location = locationP.doubleValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID, 1.);
      if(config.grab(scaleP)) {
        scale = scaleP.doubleValue();
      }
    }

    @Override
    protected LogLogisticDistribution makeInstance() {
      return new LogLogisticDistribution(shape, location, scale, rnd);
    }
  }
}
