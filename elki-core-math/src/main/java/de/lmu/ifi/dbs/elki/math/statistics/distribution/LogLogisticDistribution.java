package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Log-Logistic distribution also known as Fisk distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "fisk", "loglog" })
public class LogLogisticDistribution extends AbstractDistribution {
  /**
   * Parameters: scale and shape
   */
  double scale, shape;

  /**
   * Constructor.
   * 
   * @param scale Scale
   * @param shape Shape
   */
  public LogLogisticDistribution(double scale, double shape) {
    this(scale, shape, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param scale Scale
   * @param shape Shape
   * @param random Random number generator
   */
  public LogLogisticDistribution(double scale, double shape, Random random) {
    super(random);
    this.scale = scale;
    this.shape = shape;
  }

  /**
   * Constructor.
   * 
   * @param scale Scale
   * @param shape Shape
   * @param random Random number generator
   */
  public LogLogisticDistribution(double scale, double shape, RandomFactory random) {
    super(random);
    this.scale = scale;
    this.shape = shape;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, scale, shape);
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
  public double logpdf(double val) {
    return logpdf(val, scale, shape);
  }

  /**
   * Probability density function.
   * 
   * @param val Value
   * @param scale Scale
   * @param shape Shape
   * @return logPDF
   */
  public static double logpdf(double val, double scale, double shape) {
    if(val < 0) {
      return Double.NEGATIVE_INFINITY;
    }
    val = Math.abs(val / scale);
    return Math.log(shape / scale) + (shape - 1.) * Math.log(val) - 2. * Math.log1p(Math.pow(val, shape));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, scale, shape);
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
  public double quantile(double val) {
    return quantile(val, scale, shape);
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
  public double nextRandom() {
    double u = random.nextDouble();
    return scale * Math.pow(u / (1. - u), 1. / shape);
  }

  @Override
  public String toString() {
    return "LogLogisticDistribution(scale=" + scale + ", shape=" + shape + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double scale, shape;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

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
    protected LogLogisticDistribution makeInstance() {
      return new LogLogisticDistribution(scale, shape, rnd);
    }
  }
}
