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

import de.lmu.ifi.dbs.elki.utilities.Alias;

/**
 * Laplace distribution also known as double exponential distribution
 * 
 * @author Erich Schubert
 */
@Alias("DoubleExponentialDistribution")
public class LaplaceDistribution implements DistributionWithRandom {
  /**
   * Random generator.
   */
  Random rnd;

  /**
   * Rate, inverse of mean
   */
  double rate;

  /**
   * Location parameter.
   */
  double location;

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   */
  public LaplaceDistribution(double rate) {
    this(rate, 0.0, null);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   */
  public LaplaceDistribution(double rate, double location) {
    this(rate, location, null);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param random Random generator
   */
  public LaplaceDistribution(double rate, Random random) {
    this(rate, 0.0, random);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   * @param random Random generator
   */
  public LaplaceDistribution(double rate, double location, Random random) {
    super();
    this.rate = rate;
    this.location = location;
    this.rnd = random;
  }

  @Override
  public double pdf(double val) {
    return .5 * rate * Math.exp(-rate * Math.abs(val - location));
  }

  /**
   * PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double pdf(double val, double rate) {
    return .5 * rate * Math.exp(-rate * Math.abs(val));
  }

  @Override
  public double cdf(double val) {
    final double v = .5 * Math.exp(-rate * (val - location));
    return (val < location) ? v : 1 - v;
  }

  /**
   * Cumulative density, static version
   * 
   * @param val Value to compute CDF at
   * @param rate Rate parameter (1/scale)
   * @return cumulative density
   */
  public static double cdf(double val, double rate) {
    final double v = .5 * Math.exp(-rate * val);
    return (val < 0.) ? v : 1 - v;
  }

  @Override
  public double quantile(double val) {
    if (val < .5) {
      return Math.log(2 * val) / rate + location;
    } else {
      return -Math.log(2. - 2. * val) / rate + location;
    }
  }

  /**
   * Quantile function, static version
   * 
   * @param val Value to compute quantile for
   * @param rate Rate parameter
   * @param location Location parameter
   * @return Quantile
   */
  public static double quantile(double val, double rate, double location) {
    if (val < .5) {
      return Math.log(2 * val) / rate + location;
    } else {
      return -Math.log(2. - 2. * val) / rate + location;
    }
  }

  /**
   * This method currently uses the naive approach of returning
   * <code>-log(uniform)</code>.
   */
  @Override
  public double nextRandom() {
    double val = rnd.nextDouble();
    if (val < .5) {
      return Math.log(2 * val) / rate + location;
    } else {
      return -Math.log(2. - 2. * val) / rate + location;
    }
  }

  @Override
  public String toString() {
    return "LaplaceDistribution(rate=" + rate + ", location=" + location + ")";
  }
}
