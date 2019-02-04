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
 * Generalized Pareto Distribution (GPD), popular for modeling long tail
 * distributions.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class GeneralizedParetoDistribution extends AbstractDistribution {
  /**
   * Parameters (location, scale, shape)
   */
  final double mu, sigma, xi;

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   */
  public GeneralizedParetoDistribution(double mu, double sigma, double xi) {
    this(mu, sigma, xi, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   * @param random Random number generator
   */
  public GeneralizedParetoDistribution(double mu, double sigma, double xi, RandomFactory random) {
    super(random);
    this.mu = mu;
    this.sigma = sigma;
    this.xi = xi;
  }

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   * @param random Random number generator
   */
  public GeneralizedParetoDistribution(double mu, double sigma, double xi, Random random) {
    super(random);
    this.mu = mu;
    this.sigma = sigma;
    this.xi = xi;
  }

  /**
   * Location parameter
   * 
   * @return Location
   */
  public double getMu() {
    return mu;
  }

  /**
   * Scale parameter
   * 
   * @return Sigma
   */
  public double getSigma() {
    return sigma;
  }

  /**
   * Shape parameter
   * 
   * @return xi
   */
  public double getXi() {
    return xi;
  }

  /**
   * PDF of GPD distribution
   * 
   * @param x Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   * @return PDF at position x.
   */
  public static double pdf(double x, double mu, double sigma, double xi) {
    x = (x - mu) / sigma;
    // Check support:
    if(x < 0 || (xi < 0 && x > -1. / xi)) {
      return 0.;
    }
    return ((xi == 0) ? 1. : FastMath.pow(1 + xi * x, -1 / xi - 1)) / sigma;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, mu, sigma, xi);
  }

  /**
   * PDF of GPD distribution
   * 
   * @param x Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   * @return PDF at position x.
   */
  public static double logpdf(double x, double mu, double sigma, double xi) {
    x = (x - mu) / sigma;
    // Check support:
    if(x < 0 || (xi < 0 && x > -1. / xi)) {
      return Double.NEGATIVE_INFINITY;
    }
    if(xi == 0) {
      return Double.POSITIVE_INFINITY;
    }
    return ((xi == -1) ? 0. : FastMath.log(1 + xi * x) * (-1 / xi - 1)) - FastMath.log(sigma);
  }

  @Override
  public double logpdf(double x) {
    return logpdf(x, mu, sigma, xi);
  }

  /**
   * CDF of GPD distribution
   * 
   * @param val Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   * @return CDF at position x.
   */
  public static double cdf(double val, double mu, double sigma, double xi) {
    val = (val - mu) / sigma;
    // Check support:
    if(val < 0) {
      return 0.;
    }
    if(xi < 0 && val > -1. / xi) {
      return 1.;
    }
    return 1 - FastMath.pow(1 + xi * val, -1. / xi);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mu, sigma, xi);
  }

  /**
   * Quantile function of GPD distribution
   * 
   * @param val Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param xi Shape parameter xi (= -kappa)
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double mu, double sigma, double xi) {
    if(val < 0.0 || val > 1.0) {
      return Double.NaN;
    }
    if(xi == 0.) {
      return mu - sigma * FastMath.log(1 - val);
    }
    return mu - sigma / xi * (1 - FastMath.pow(1 - val, -xi));
  }

  @Override
  public double quantile(double val) {
    return quantile(val, mu, sigma, xi);
  }

  @Override
  public String toString() {
    return "GeneralizedParetoDistribution(sigma=" + sigma + ", mu=" + mu + ", xi=" + xi + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double mu, sigma, xi;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter muP = new DoubleParameter(LOCATION_ID);
      if(config.grab(muP)) {
        mu = muP.doubleValue();
      }

      DoubleParameter sigmaP = new DoubleParameter(SCALE_ID);
      if(config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }

      DoubleParameter xiP = new DoubleParameter(SHAPE_ID);
      if(config.grab(xiP)) {
        xi = xiP.doubleValue();
      }
    }

    @Override
    protected GeneralizedParetoDistribution makeInstance() {
      return new GeneralizedParetoDistribution(mu, sigma, xi, rnd);
    }
  }
}
