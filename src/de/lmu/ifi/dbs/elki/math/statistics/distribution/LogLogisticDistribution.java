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
 * Log-Logistic distribution also known as Fisk distribution.
 * 
 * @author Erich Schubert
 */
public class LogLogisticDistribution implements Distribution {
  /**
   * Parameters: scale and shape
   */
  double scale, shape;

  /**
   * Random number generator
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param scale Scale
   * @param shape Shape
   */
  public LogLogisticDistribution(double scale, double shape) {
    this(scale, shape, null);
  }

  /**
   * Constructor.
   * 
   * @param scale Scale
   * @param shape Shape
   * @param random Random number generator
   */
  public LogLogisticDistribution(double scale, double shape, Random random) {
    super();
    this.scale = scale;
    this.shape = shape;
    this.random = random;
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param scale Scale
   * @param shape Shape
   * @return PDF
   */
  public static double pdf(double val, double scale, double shape) {
    if(val < 0) {
      return 0;
    }
    val = Math.abs(val / scale);
    double f = shape / scale * Math.pow(val, shape - 1.);
    double d = 1. + Math.pow(val, shape);
    return f / (d * d);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, scale, shape);
  }

  /**
   * Cumulative density function.
   * 
   * @param val Value
   * @param scale Scale
   * @param shape Shape
   * @return CDF
   */
  public static double cdf(double val, double scale, double shape) {
    if(val < 0) {
      return 0;
    }
    return 1. / (1. + Math.pow(val / scale, -shape));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, scale, shape);
  }

  /**
   * Quantile function.
   * 
   * @param val Value
   * @param scale Scale
   * @param shape Shape
   * @return Quantile
   */
  public static double quantile(double val, double scale, double shape) {
    return scale * Math.pow(val / (1. - val), 1. / shape);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, scale, shape);
  }

  @Override
  public double nextRandom() {
    double u = random.nextDouble();
    return scale * Math.pow(u / (1. - u), 1. / shape);
  }

  @Override
  public String toString() {
    return "LogLogisticDistribution(scale=" + scale + ", shape=" + shape + ")";
  }
}
