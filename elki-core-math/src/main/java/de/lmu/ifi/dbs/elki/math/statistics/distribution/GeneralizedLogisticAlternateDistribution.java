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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Generalized logistic distribution.
 * 
 * One of multiple ways of generalizing the logistic distribution.
 * 
 * Where {@code shape=0} yields the regular logistic distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class GeneralizedLogisticAlternateDistribution extends AbstractDistribution {
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
  public GeneralizedLogisticAlternateDistribution(double location, double scale, double shape) {
    this(location, scale, shape, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   * @param shape Shape parameter
   * @param random Random number generator
   */
  public GeneralizedLogisticAlternateDistribution(double location, double scale, double shape, Random random) {
    super(random);
    this.location = location;
    this.scale = scale;
    this.shape = shape;
    if(!(shape > -1.) || !(shape < 1.)) {
      throw new ArithmeticException("Invalid shape parameter - must be -1 to +1, is: " + shape);
    }
  }

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   * @param shape Shape parameter
   * @param random Random number generator
   */
  public GeneralizedLogisticAlternateDistribution(double location, double scale, double shape, RandomFactory random) {
    super(random);
    this.location = location;
    this.scale = scale;
    this.shape = shape;
    if(!(shape > -1.) || !(shape < 1.)) {
      throw new ArithmeticException("Invalid shape parameter - must be -1 to +1, is: " + shape);
    }
  }

  /**
   * Scale parameter
   * 
   * @return Scale
   */
  public double getScale() {
    return scale;
  }

  /**
   * Shape parameter
   * 
   * @return Shape
   */
  public double getShape() {
    return shape;
  }

  /**
   * Location parameter
   * 
   * @return Location
   */
  public double getLocation() {
    return location;
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
    val = (val - loc) / scale;
    if(shape != 0.) {
      val = -FastMath.log(1 - shape * val) / shape;
    }
    double f = 1. + FastMath.exp(-val);
    return FastMath.exp(-val * (1 - shape)) / (scale * f * f);
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
  public static double logpdf(double val, double loc, double scale, double shape) {
    val = (val - loc) / scale;
    if(shape != 0.) {
      val = -FastMath.log(1 - shape * val) / shape;
    }
    double f = 1. + FastMath.exp(-val);
    return -val * (1 - shape) - FastMath.log(scale * f * f);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, location, scale, shape);
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
    if(shape != 0.) {
      final double tmp = 1 - shape * val;
      if(tmp < 1e-15) {
        return (shape < 0) ? 0 : 1;
      }
      val = -FastMath.log(tmp) / shape;
    }
    return 1. / (1. + FastMath.exp(-val));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, location, scale, shape);
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
    if(shape == 0.) {
      return loc - scale * FastMath.log((1 - val) / val);
    }
    return loc + scale * (1 - FastMath.pow((1 - val) / val, shape)) / shape;
  }

  @Override
  public double quantile(double val) {
    return quantile(val, location, scale, shape);
  }

  @Override
  public double nextRandom() {
    double u = random.nextDouble();
    return quantile(u, location, scale, shape);
  }

  @Override
  public String toString() {
    return "GeneralizedLogisticAlternateDistribution(location=" + location + ", scale=" + scale + ", shape=" + shape + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double location, scale, shape;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter locationP = new DoubleParameter(LOCATION_ID);
      if(config.grab(locationP)) {
        location = locationP.doubleValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID);
      if(config.grab(scaleP)) {
        scale = scaleP.doubleValue();
      }

      DoubleParameter shapeP = new DoubleParameter(SHAPE_ID);
      if(config.grab(shapeP)) {
        shape = shapeP.doubleValue();
      }
    }

    @Override
    protected GeneralizedLogisticAlternateDistribution makeInstance() {
      return new GeneralizedLogisticAlternateDistribution(location, scale, shape, rnd);
    }
  }
}
