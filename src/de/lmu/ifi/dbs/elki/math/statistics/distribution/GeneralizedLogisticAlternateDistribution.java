package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.Random;

/**
 * Generalized logistic distribution.
 * 
 * One of multiple ways of generalizing the logistic distribution.
 * 
 * Where {@code shape=0} yields the regular logistic distribution.
 * 
 * @author Erich Schubert
 */
public class GeneralizedLogisticAlternateDistribution implements DistributionWithRandom {
  /**
   * Parameters: location and scale
   */
  double location, scale;

  /**
   * Shape parameter, for generalized logistic distribution.
   */
  double shape;

  /**
   * Random number generator
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param location Location
   * @param scale Scale
   * @param shape Shape parameter
   */
  public GeneralizedLogisticAlternateDistribution(double location, double scale, double shape) {
    this(location, scale, shape, null);
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
    super();
    this.location = location;
    this.scale = scale;
    this.shape = shape;
    this.random = random;
    if(!(shape > -1.) || !(shape < 1.)) {
      throw new ArithmeticException("Invalid shape parameter - must be -1 to +1, is: "+shape);
    }
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
      val = -Math.log(1 - shape * val) / shape;
    }
    double f = 1. + Math.exp(-val);
    return Math.exp(-val * (1 - shape)) / (scale * f * f);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, location, scale, shape);
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
      val = -Math.log(1 - shape * val) / shape;
    }
    return 1. / (1. + Math.exp(-val));
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
      return loc - scale * Math.log((1 - val) / val);
    }
    return loc + scale * (1 - Math.pow((1 - val) / val, shape)) / shape;
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
}
