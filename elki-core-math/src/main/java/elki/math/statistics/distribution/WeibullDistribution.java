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
 * Weibull distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class WeibullDistribution implements Distribution {
  /**
   * Shift offset.
   */
  double theta = 0.;

  /**
   * Shape parameter k.
   */
  double k;

  /**
   * Lambda parameter.
   */
  double lambda;

  /**
   * Constructor.
   * 
   * @param k Shape parameter
   * @param lambda Scale parameter
   */
  public WeibullDistribution(double k, double lambda) {
    this(k, lambda, 0.0);
  }

  /**
   * Constructor.
   * 
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   */
  public WeibullDistribution(double k, double lambda, double theta) {
    this.k = k;
    this.lambda = lambda;
    this.theta = theta;
  }

  /**
   * Get the shape k parameter.
   *
   * @return shape k
   */
  public double getK() {
    return k;
  }

  /**
   * Get the scale lambda parameter.
   *
   * @return scale lambda
   */
  public double getLambda() {
    return lambda;
  }

  /**
   * Get the shift theta parameter.
   *
   * @return shift theta
   */
  public double getTheta() {
    return theta;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, k, lambda, theta);
  }

  @Override
  public double logpdf(double x) {
    return logpdf(x, k, lambda, theta);
  }

  /**
   * PDF of Weibull distribution
   * 
   * @param x Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return PDF at position x.
   */
  public static double pdf(double x, double k, double lambda, double theta) {
    if(x <= theta || x == Double.POSITIVE_INFINITY) {
      return 0.;
    }
    double xl = (x - theta) / lambda;
    double p = FastMath.pow(xl, k - 1), p2 = p * -xl;
    return p2 != Double.NEGATIVE_INFINITY ? k / lambda * p * FastMath.exp(p2) : 0.;
  }

  /**
   * PDF of Weibull distribution
   * 
   * @param x Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return PDF at position x.
   */
  public static double logpdf(double x, double k, double lambda, double theta) {
    if(x <= theta || x == Double.POSITIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    double xl = (x - theta) / lambda;
    return FastMath.log(k / lambda) + (k - 1) * FastMath.log(xl) - FastMath.pow(xl, k);
  }

  /**
   * CDF of Weibull distribution
   * 
   * @param val Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return CDF at position x.
   */
  public static double cdf(double val, double k, double lambda, double theta) {
    return (val > theta) ? //
        (1.0 - FastMath.exp(-FastMath.pow((val - theta) / lambda, k))) //
        : val == val ? 0.0 : Double.NaN;
  }

  @Override
  public double cdf(double val) {
    return cdf(val, k, lambda, theta);
  }

  /**
   * Quantile function of Weibull distribution
   * 
   * @param val Value
   * @param k Shape parameter
   * @param lambda Scale parameter
   * @param theta Shift offset parameter
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double k, double lambda, double theta) {
    if(val < 0.0 || val > 1.0) {
      return Double.NaN;
    }
    else if(val == 0) {
      return 0.0;
    }
    else if(val == 1) {
      return Double.POSITIVE_INFINITY;
    }
    else {
      return theta + lambda * FastMath.pow(-FastMath.log(1.0 - val), 1.0 / k);
    }
  }

  @Override
  public double quantile(double val) {
    return quantile(val, k, lambda, theta);
  }

  @Override
  public double nextRandom(Random random) {
    return theta + lambda * FastMath.pow(-FastMath.log(1 - random.nextDouble()), 1. / k);
  }

  @Override
  public String toString() {
    return "WeibullDistribution(k=" + k + ", lambda=" + lambda + ", theta=" + theta + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Distribution.Parameterizer {
    /** Parameters. */
    double theta, k, lambda;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(LOCATION_ID, 0.) //
          .grab(config, x -> theta = x);
      new DoubleParameter(SCALE_ID) //
          .grab(config, x -> lambda = x);
      new DoubleParameter(SHAPE_ID) //
          .grab(config, x -> k = x);
    }

    @Override
    public WeibullDistribution make() {
      return new WeibullDistribution(k, lambda, theta);
    }
  }
}
