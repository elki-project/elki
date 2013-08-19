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
 * Rayleigh distribution.
 * 
 * @author Erich Schubert
 */
public class RayleighDistribution implements DistributionWithRandom {
  /**
   * Position parameter.
   */
  double mu = 0.0;

  /**
   * Scale parameter.
   */
  double sigma;

  /**
   * Random number generator.
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param sigma Scale parameter
   */
  public RayleighDistribution(double sigma) {
    this(0., sigma, null);
  }

  /**
   * Constructor.
   * 
   * @param mu Position parameter
   * @param sigma Scale parameter
   */
  public RayleighDistribution(double mu, double sigma) {
    this(mu, sigma, null);
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
    super();
    this.mu = mu;
    this.sigma = sigma;
    this.random = random;
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
    if(x > 0.) {
      x = x / sigma;
      return x / sigma * Math.exp(-.5 * x * x);
    }
    else {
      return 0.;
    }
  }

  /**
   * CDF of Rayleigh distribution
   * 
   * @param val Value
   * @param sigma Scale parameter
   * @return CDF at position x.
   */
  public static double cdf(double val, double sigma) {
    if(val > 0.) {
      double x = val / sigma;
      return 1.0 - Math.exp(-.5 * x * x);
    }
    else {
      return 0.0;
    }
  }

  @Override
  public double cdf(double val) {
    return cdf(val - mu, sigma);
  }

  /**
   * Quantile function of Rayleigh distribution
   * 
   * @param val Value
   * @param sigma Scale parameter
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double sigma) {
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
      return sigma * Math.sqrt(-2. * Math.log(val));
    }
  }

  @Override
  public double quantile(double val) {
    return mu + quantile(val, sigma);
  }

  @Override
  public double nextRandom() {
    return mu + sigma * Math.sqrt(-2. * Math.log(random.nextDouble()));
  }

  @Override
  public String toString() {
    return "RayleighDistribution(mu=" + mu + ", sigma=" + sigma + ")";
  }
}
