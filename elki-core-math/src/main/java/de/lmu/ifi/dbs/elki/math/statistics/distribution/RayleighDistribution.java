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
 * Rayleigh distribution, a special case of the Weibull distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class RayleighDistribution extends AbstractDistribution {
  /**
   * Location parameter.
   */
  double mu = 0.;

  /**
   * Scale parameter.
   */
  double sigma;

  /**
   * Constructor.
   * 
   * @param sigma Scale parameter
   */
  public RayleighDistribution(double sigma) {
    this(0., sigma, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param mu Position parameter
   * @param sigma Scale parameter
   */
  public RayleighDistribution(double mu, double sigma) {
    this(mu, sigma, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param sigma Scale parameter
   * @param random Random number generator
   */
  public RayleighDistribution(double sigma, Random random) {
    this(0., sigma, random);
  }

  /**
   * Constructor.
   * 
   * @param mu Position parameter
   * @param sigma Scale parameter
   * @param random Random number generator
   */
  public RayleighDistribution(double mu, double sigma, Random random) {
    super(random);
    this.mu = mu;
    this.sigma = sigma;
  }

  /**
   * Constructor.
   * 
   * @param mu Position parameter
   * @param sigma Scale parameter
   * @param random Random number generator
   */
  public RayleighDistribution(double mu, double sigma, RandomFactory random) {
    super(random);
    this.mu = mu;
    this.sigma = sigma;
  }

  /**
   * Get the position parameter.
   *
   * @return Position
   */
  public double getMu() {
    return mu;
  }

  /**
   * Get the scale parameter.
   *
   * @return scale
   */
  public double getSigma() {
    return sigma;
  }

  @Override
  public double pdf(double x) {
    return pdf(x - mu, sigma);
  }

  /**
   * PDF of Rayleigh distribution
   * 
   * @param x Value
   * @param sigma Scale
   * @return PDF at position x.
   */
  public static double pdf(double x, double sigma) {
    if(!(x > 0.) || x == Double.POSITIVE_INFINITY) {
      return x == x ? 0. : Double.NaN;
    }
    final double xs = x / sigma;
    final double v = FastMath.exp(-.5 * xs * xs);
    return v > 0. ? xs / sigma * v : 0.;
  }

  @Override
  public double logpdf(double x) {
    return logpdf(x - mu, sigma);
  }

  /**
   * PDF of Rayleigh distribution
   * 
   * @param x Value
   * @param sigma Scale
   * @return PDF at position x.
   */
  public static double logpdf(double x, double sigma) {
    if(!(x > 0.) || x == Double.POSITIVE_INFINITY) {
      return x == x ? Double.NEGATIVE_INFINITY : Double.NaN;
    }
    final double xs = x / sigma, xs2 = xs * xs;
    return xs2 < Double.POSITIVE_INFINITY ? FastMath.log(xs / sigma) - .5 * xs2 : Double.NEGATIVE_INFINITY;
  }

  @Override
  public double cdf(double val) {
    return cdf(val - mu, sigma);
  }

  /**
   * CDF of Rayleigh distribution
   * 
   * @param x Value
   * @param sigma Scale parameter
   * @return CDF at position x.
   */
  public static double cdf(double x, double sigma) {
    if(x <= 0.) {
      return 0.;
    }
    final double xs = x / sigma;
    return 1. - FastMath.exp(-.5 * xs * xs);
  }

  @Override
  public double quantile(double val) {
    return mu + quantile(val, sigma);
  }

  /**
   * Quantile function of Rayleigh distribution
   * 
   * @param val Value
   * @param sigma Scale parameter
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double sigma) {
    if(!(val >= 0.) || !(val <= 1.)) {
      return Double.NaN;
    }
    if(val == 0.) {
      return 0.;
    }
    if(val == 1.) {
      return Double.POSITIVE_INFINITY;
    }
    return sigma * FastMath.sqrt(-2. * FastMath.log(1. - val));
  }

  @Override
  public double nextRandom() {
    return mu + sigma * FastMath.sqrt(-2. * FastMath.log(random.nextDouble()));
  }

  @Override
  public String toString() {
    return "RayleighDistribution(mu=" + mu + ", sigma=" + sigma + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double mean, scale;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter meanP = new DoubleParameter(LOCATION_ID, 0.);
      if(config.grab(meanP)) {
        mean = meanP.doubleValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID);
      if(config.grab(scaleP)) {
        scale = scaleP.doubleValue();
      }
    }

    @Override
    protected RayleighDistribution makeInstance() {
      return new RayleighDistribution(mean, scale, rnd);
    }
  }
}
