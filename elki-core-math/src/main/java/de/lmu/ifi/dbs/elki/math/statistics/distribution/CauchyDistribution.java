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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Cauchy distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class CauchyDistribution extends AbstractDistribution {
  /**
   * The location (x0) parameter.
   */
  final double location;

  /**
   * The shape (gamma) parameter.
   */
  final double shape;

  /**
   * Constructor with default random.
   * 
   * @param location Location (x0)
   * @param shape Shape (gamma)
   */
  public CauchyDistribution(double location, double shape) {
    this(location, shape, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param location Location (x0)
   * @param shape Shape (gamma)
   * @param random Random generator
   */
  public CauchyDistribution(double location, double shape, Random random) {
    super(random);
    this.location = location;
    this.shape = shape;
  }

  /**
   * Constructor.
   * 
   * @param location Location (x0)
   * @param shape Shape (gamma)
   * @param random Random generator
   */
  public CauchyDistribution(double location, double shape, RandomFactory random) {
    super(random);
    this.location = location;
    this.shape = shape;
  }

  /**
   * Get the location parameter.
   * 
   * @return Location
   */
  public double getLocation() {
    return location;
  }

  /**
   * Return the shape parameter.
   * 
   * @return Shape
   */
  public double getShape() {
    return shape;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, location, shape);
  }

  @Override
  public double logpdf(double x) {
    return logpdf(x, location, shape);
  }

  @Override
  public double cdf(double x) {
    return cdf(x, location, shape);
  }

  @Override
  public double quantile(double x) {
    return quantile(x, location, shape);
  }

  @Override
  public double nextRandom() {
    final double r = random.nextDouble() - .5;
    return location + shape / FastMath.tan(Math.PI * r);
  }

  /**
   * PDF function, static version.
   * 
   * @param x Value
   * @param location Location (x0)
   * @param shape Shape (gamma)
   * @return PDF value
   */
  public static double pdf(double x, double location, double shape) {
    final double v = (x - location) / shape;
    return 1. / (Math.PI * shape * (1 + v * v));
  }

  /**
   * PDF function, static version.
   * 
   * @param x Value
   * @param location Location (x0)
   * @param shape Shape (gamma)
   * @return PDF value
   */
  public static double logpdf(double x, double location, double shape) {
    final double v = (x - location) / shape;
    return -FastMath.log(Math.PI * shape * (1 + v * v));
  }

  /**
   * PDF function, static version.
   * 
   * @param x Value
   * @param location Location (x0)
   * @param shape Shape (gamma)
   * @return PDF value
   */
  public static double cdf(double x, double location, double shape) {
    return FastMath.atan2(x - location, shape) / Math.PI + .5;
  }

  /**
   * PDF function, static version.
   * 
   * @param x Value
   * @param location Location (x0)
   * @param shape Shape (gamma)
   * @return PDF value
   */
  public static double quantile(double x, double location, double shape) {
    return (x == .5) ? location : (x <= .5) //
        ? x <= 0. ? x < 0. ? Double.NaN : Double.NEGATIVE_INFINITY //
            : location - shape / FastMath.tan(Math.PI * x) //
        : x >= 1. ? x > 1. ? Double.NaN : Double.POSITIVE_INFINITY //
            : location + shape / FastMath.tan(Math.PI * (1 - x));
  }

  @Override
  public String toString() {
    return "CauchyDistribution(location=" + location + ", shape=" + shape + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Shape parameter gamma.
     */
    public static final OptionID SHAPE_ID = new OptionID("distribution.cauchy.shape", "Cauchy distribution gamma/shape parameter.");

    /** Parameters. */
    double location, shape;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter locP = new DoubleParameter(LOCATION_ID);
      if(config.grab(locP)) {
        location = locP.doubleValue();
      }

      DoubleParameter shapeP = new DoubleParameter(SHAPE_ID);
      if(config.grab(shapeP)) {
        shape = shapeP.doubleValue();
      }
    }

    @Override
    protected CauchyDistribution makeInstance() {
      return new CauchyDistribution(location, shape, rnd);
    }
  }
}
