/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
 * Gumbel distribution, also known as Log-Weibull distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class GumbelDistribution implements Distribution {
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
  public double nextRandom(Random random) {
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
  public static class Par implements Distribution.Parameterizer {
    /** Parameters. */
    double mean, shape;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(LOCATION_ID) //
          .grab(config, x -> mean = x);
      new DoubleParameter(SHAPE_ID) //
          .grab(config, x -> shape = x);
    }

    @Override
    public GumbelDistribution make() {
      return new GumbelDistribution(mean, shape);
    }
  }
}
