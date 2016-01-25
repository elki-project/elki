package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Inverse Gaussian distribution aka Wald distribution
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "InverseGaussianDistribution", "invgauss" })
public class WaldDistribution extends AbstractDistribution {
  /**
   * Location value
   */
  private double mean;

  /**
   * Shape parameter
   */
  private double shape;

  /**
   * Constructor for wald distribution
   * 
   * @param mean Mean
   * @param shape Shape parameter
   * @param random Random generator
   */
  public WaldDistribution(double mean, double shape, Random random) {
    super(random);
    this.mean = mean;
    this.shape = shape;
  }

  /**
   * Constructor for wald distribution
   * 
   * @param mean Mean
   * @param shape Shape parameter
   * @param random Random generator
   */
  public WaldDistribution(double mean, double shape, RandomFactory random) {
    super(random);
    this.mean = mean;
    this.shape = shape;
  }

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param shape Shape parameter
   */
  public WaldDistribution(double mean, double shape) {
    this(mean, shape, (Random) null);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, mean, shape);
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
  public double nextRandom() {
    double v = random.nextGaussian();
    v *= v;
    double x = mean + mean * .5 / shape * (mean * v - Math.sqrt(4. * mean * shape * v + mean * mean * v * v));
    double u = random.nextDouble();
    if (u * (mean + x) <= mean) {
      return x;
    } else {
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
   * 
   * @param x The value.
   * @param mu The mean.
   * @param shape Shape parameter
   * @return PDF of the given Wald distribution at x.
   */
  public static double pdf(double x, double mu, double shape) {
    if (!(x > 0)) {
      return 0;
    }
    final double v = (x - mu);
    return Math.sqrt(shape / (MathUtil.TWOPI * x * x * x)) * Math.exp(-shape * v * v / (2. * mu * mu * x));
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
    if (!(x > 0.)) {
      return 0.;
    }
    // TODO: accelerate by caching exp(2 * shape / mu).
    final double v0 = x / mu;
    final double v1 = Math.sqrt(shape / x);
    double c1 = NormalDistribution.standardNormalCDF(v1 * (v0 - 1.));
    double c2 = NormalDistribution.standardNormalCDF(-v1 * (v0 + 1.));
    if (c2 > 0.) {
      return c1 + Math.exp(2 * shape / mu) * c2;
    } else {
      return c1;
    }
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
    throw new NotImplementedException(ExceptionMessages.UNSUPPORTED_NOT_YET);
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
    double mean, shape;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter meanP = new DoubleParameter(LOCATION_ID);
      if (config.grab(meanP)) {
        mean = meanP.doubleValue();
      }

      DoubleParameter shapeP = new DoubleParameter(SHAPE_ID);
      if (config.grab(shapeP)) {
        shape = shapeP.doubleValue();
      }
    }

    @Override
    protected WaldDistribution makeInstance() {
      return new WaldDistribution(mean, shape, rnd);
    }
  }
}
