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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Exponential distribution.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public class ExponentialDistribution extends AbstractDistribution {
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
  public ExponentialDistribution(double rate) {
    this(rate, 0.0, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   */
  public ExponentialDistribution(double rate, double location) {
    this(rate, location, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param random Random generator
   */
  public ExponentialDistribution(double rate, Random random) {
    this(rate, 0.0, random);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   * @param random Random generator
   */
  public ExponentialDistribution(double rate, double location, Random random) {
    super(random);
    this.rate = rate;
    this.location = location;
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   * @param random Random generator
   */
  public ExponentialDistribution(double rate, double location, RandomFactory random) {
    super(random);
    this.rate = rate;
    this.location = location;
  }

  /**
   * Get rate parameter.
   *
   * @return Rate
   */
  public double getRate() {
    return rate;
  }

  /**
   * Get the location parameter.
   * 
   * @return Location
   */
  public double getLocation() {
    return location;
  }

  @Override
  public double pdf(double val) {
    return val < location ? 0. : rate * FastMath.exp(-rate * (val - location));
  }

  @Override
  public double logpdf(double val) {
    return val < location ? Double.NEGATIVE_INFINITY : FastMath.log(rate) - rate * (val - location);
  }

  /**
   * PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double pdf(double val, double rate) {
    return val < 0. ? 0. : rate * FastMath.exp(-rate * val);
  }

  /**
   * log PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double logpdf(double val, double rate) {
    return val < 0. ? Double.NEGATIVE_INFINITY : FastMath.log(rate) - rate * val;
  }

  @Override
  public double cdf(double val) {
    return val < location ? 0. : 1 - FastMath.exp(-rate * (val - location));
  }

  /**
   * Cumulative density, static version
   * 
   * @param val Value to compute CDF at
   * @param rate Rate parameter (1/scale)
   * @return cumulative density
   */
  public static double cdf(double val, double rate) {
    return val < 0. ? 0. : 1 - FastMath.exp(-rate * val);
  }

  @Override
  public double quantile(double val) {
    return val >= 0 && val <= 1 ? -FastMath.log(1 - val) / rate + location : Double.NaN;
  }

  /**
   * Quantile function, static version
   * 
   * @param val Value to compute quantile for
   * @param rate Rate parameter
   * @return Quantile
   */
  public static double quantile(double val, double rate) {
    return val >= 0 && val <= 1 ? -FastMath.log(1 - val) / rate : Double.NaN;
  }

  /**
   * This method currently uses the naive approach of returning
   * <code>-log(uniform)</code>.
   * 
   * TODO: there are variants that do not rely on the log method and are faster.
   * We need to implement and evaluate these. For details: see "Computer methods
   * for sampling from the exponential and normal distributions" J. H. Ahrens,
   * U. Dieter, https://dl.acm.org/citation.cfm?id=361593
   */
  @Override
  public double nextRandom() {
    return -FastMath.log(random.nextDouble()) / rate + location;
  }

  @Override
  public String toString() {
    return "ExponentialDistribution(rate=" + rate + ", location=" + location + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Shape parameter gamma.
     */
    public static final OptionID RATE_ID = new OptionID("distribution.exponential.rate", "Exponential distribution rate (lambda) parameter (inverse of scale).");

    /** Parameters. */
    double location, rate;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter locP = new DoubleParameter(LOCATION_ID, 0.);
      if(config.grab(locP)) {
        location = locP.doubleValue();
      }

      DoubleParameter rateP = new DoubleParameter(RATE_ID);
      if(config.grab(rateP)) {
        rate = rateP.doubleValue();
      }
    }

    @Override
    protected ExponentialDistribution makeInstance() {
      return new ExponentialDistribution(rate, location, rnd);
    }
  }
}