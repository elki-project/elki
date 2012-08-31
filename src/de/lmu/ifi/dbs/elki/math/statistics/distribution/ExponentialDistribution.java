package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * Exponential distribution.
 * 
 * @author Erich Schubert
 */
public class ExponentialDistribution implements DistributionWithRandom {
  /**
   * Random generator.
   */
  Random rnd;

  /**
   * Rate, inverse of mean
   */
  double rate;

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   */
  public ExponentialDistribution(double rate) {
    this(rate, new Random());
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param random Random generator
   */
  public ExponentialDistribution(double rate, Random random) {
    super();
    this.rate = rate;
    this.rnd = random;
  }

  @Override
  public double pdf(double val) {
    return rate * Math.exp(-rate * val);
  }

  /**
   * PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double pdf(double val, double rate) {
    return rate * Math.exp(-rate * val);
  }

  @Override
  public double cdf(double val) {
    return 1 - Math.exp(-rate * val);
  }

  /**
   * Cumulative density, static version
   * 
   * @param val Value to compute CDF at
   * @param rate Rate parameter (1/scale)
   * @return cumulative density
   */
  public static double cdf(double val, double rate) {
    return 1 - Math.exp(-rate * val);
  }

  @Override
  public double quantile(double val) {
    return -Math.log1p(-val) / rate;
  }

  /**
   * Quantile function, static version
   * 
   * @param val Value to compute quantile for
   * @param rate
   * @return
   */
  public static double quantile(double val, double rate) {
    return -Math.log1p(-val) / rate;
  }

  /**
   * This method currently uses the naive approach of returning
   * <code>-log(uniform)</code>.
   * 
   * TODO: there are variants that do not rely on the log method and are faster.
   * We need to implement and evaluate these. For details: see
   * "Computer methods for sampling from the exponential and normal distributions"
   * J. H. Ahrens, U. Dieter, https://dl.acm.org/citation.cfm?id=361593
   */
  @Override
  public double nextRandom() {
    return -Math.log(rnd.nextDouble()) / rate;
  }
}