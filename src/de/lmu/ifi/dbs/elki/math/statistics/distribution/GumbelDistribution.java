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
 * Gumbel distribution, also known as Log-Weibull distribution.
 * 
 * @author Erich Schubert
 */
public class GumbelDistribution implements DistributionWithRandom {
  /**
   * Mode parameter mu.
   */
  double mu;

  /**
   * Shape parameter beta.
   */
  double beta;

  /**
   * Random number generator.
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param mu Mode
   * @param beta Shape
   */
  public GumbelDistribution(double mu, double beta) {
    this(mu, beta, null);
  }

  /**
   * Constructor.
   * 
   * @param mu Mode
   * @param beta Shape
   * @param random Random number generator
   */
  public GumbelDistribution(double mu, double beta, Random random) {
    super();
    this.mu = mu;
    this.beta = beta;
    this.random = random;
  }

  /**
   * PDF of Weibull distribution
   * 
   * @param x Value
   * @param mu Mode
   * @param beta Shape
   * @return PDF at position x.
   */
  public static double pdf(double x, double mu, double beta) {
    final double z = (x - mu) / beta;
    return Math.exp(-z - Math.exp(-z)) / beta;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, mu, beta);
  }

  /**
   * CDF of Weibull distribution
   * 
   * @param val Value
   * @param mu Mode
   * @param beta Shape
   * @return CDF at position x.
   */
  public static double cdf(double val, double mu, double beta) {
    return Math.exp(-Math.exp(-(val - mu) / beta));
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mu, beta);
  }

  /**
   * Quantile function of Weibull distribution
   * 
   * @param val Value
   * @param mu Mode
   * @param beta Shape
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double mu, double beta) {
    return mu + beta * Math.log(1 / Math.log(1 / val));
  }

  @Override
  public double quantile(double val) {
    return quantile(val, mu, beta);
  }

  @Override
  public double nextRandom() {
    return mu + beta * Math.log(1 / Math.log(1 / random.nextDouble()));
  }

  @Override
  public String toString() {
    return "GumbelDistribution(mu=" + mu + ", beta=" + beta + ")";
  }
}
