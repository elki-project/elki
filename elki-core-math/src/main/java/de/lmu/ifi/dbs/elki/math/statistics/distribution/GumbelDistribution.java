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
 * Gumbel distribution, also known as Log-Weibull distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class GumbelDistribution extends AbstractDistribution {
  /**
   * Mode parameter mu.
   */
  double mu;

  /**
   * Shape parameter beta.
   */
  double beta;

  /**
   * Constructor.
   * 
   * @param mu Mode
   * @param beta Shape
   */
  public GumbelDistribution(double mu, double beta) {
    this(mu, beta, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param mu Mode
   * @param beta Shape
   * @param random Random number generator
   */
  public GumbelDistribution(double mu, double beta, Random random) {
    super(random);
    this.mu = mu;
    this.beta = beta;
  }

  /**
   * Constructor.
   * 
   * @param mu Mode
   * @param beta Shape
   * @param random Random number generator
   */
  public GumbelDistribution(double mu, double beta, RandomFactory random) {
    super(random);
    this.mu = mu;
    this.beta = beta;
  }

  /**
   * Get the location
   * 
   * @return Mu
   */
  public double getMu() {
    return mu;
  }

  /**
   * Get the shape
   * 
   * @return Beta
   */
  public double getBeta() {
    return beta;
  }

  /**
   * PDF of Gumbel distribution
   * 
   * @param x Value
   * @param mu Mode
   * @param beta Shape
   * @return PDF at position x.
   */
  public static double pdf(double x, double mu, double beta) {
    final double z = (x - mu) / beta;
    if(x == Double.NEGATIVE_INFINITY) {
      return 0.;
    }
    return FastMath.exp(-z - FastMath.exp(-z)) / beta;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, mu, beta);
  }

  /**
   * log PDF of Gumbel distribution
   * 
   * @param x Value
   * @param mu Mode
   * @param beta Shape
   * @return PDF at position x.
   */
  public static double logpdf(double x, double mu, double beta) {
    if(x == Double.NEGATIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    final double z = (x - mu) / beta;
    return -z - FastMath.exp(-z) - FastMath.log(beta);
  }

  @Override
  public double logpdf(double x) {
    return logpdf(x, mu, beta);
  }

  /**
   * CDF of Gumbel distribution
   * 
   * @param val Value
   * @param mu Mode
   * @param beta Shape
   * @return CDF at position x.
   */
  public static double cdf(double val, double mu, double beta) {
    return FastMath.exp(-FastMath.exp(-(val - mu) / beta));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mu, beta);
  }

  /**
   * Quantile function of Gumbel distribution
   * 
   * @param val Value
   * @param mu Mode
   * @param beta Shape
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double mu, double beta) {
    return mu - beta * FastMath.log(-FastMath.log(val));
  }

  @Override
  public double quantile(double val) {
    return quantile(val, mu, beta);
  }

  @Override
  public double nextRandom() {
    return mu - beta * FastMath.log(-FastMath.log(random.nextDouble()));
  }

  @Override
  public String toString() {
    return "GumbelDistribution(mu=" + mu + ", beta=" + beta + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double mean, shape;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter meanP = new DoubleParameter(LOCATION_ID);
      if(config.grab(meanP)) {
        mean = meanP.doubleValue();
      }

      DoubleParameter shapeP = new DoubleParameter(SHAPE_ID);
      if(config.grab(shapeP)) {
        shape = shapeP.doubleValue();
      }
    }

    @Override
    protected GumbelDistribution makeInstance() {
      return new GumbelDistribution(mean, shape, rnd);
    }
  }
}
