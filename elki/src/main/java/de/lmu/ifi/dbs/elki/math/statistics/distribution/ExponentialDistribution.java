package de.lmu.ifi.dbs.elki.math.statistics.distribution;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

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

  @Override
  public double pdf(double val) {
    if(val < location) {
      return 0.;
    }
    return rate * Math.exp(-rate * (val - location));
  }

  @Override
  public double logpdf(double val) {
    if(val < location) {
      return Double.NEGATIVE_INFINITY;
    }
    return Math.log(rate) - rate * (val - location);
  }

  /**
   * PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double pdf(double val, double rate) {
    if(val < 0.) {
      return 0.;
    }
    return rate * Math.exp(-rate * val);
  }

  /**
   * log PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double logpdf(double val, double rate) {
    if(val < 0.) {
      return Double.NEGATIVE_INFINITY;
    }
    return Math.log(rate) - rate * val;
  }

  @Override
  public double cdf(double val) {
    if(val < location) {
      return 0.;
    }
    return 1 - Math.exp(-rate * (val - location));
  }

  /**
   * Cumulative density, static version
   * 
   * @param val Value to compute CDF at
   * @param rate Rate parameter (1/scale)
   * @return cumulative density
   */
  public static double cdf(double val, double rate) {
    if(val < 0.) {
      return 0.;
    }
    return 1 - Math.exp(-rate * val);
  }

  @Override
  public double quantile(double val) {
    return -Math.log(1 - val) / rate + location;
  }

  /**
   * Quantile function, static version
   * 
   * @param val Value to compute quantile for
   * @param rate Rate parameter
   * @return Quantile
   */
  public static double quantile(double val, double rate) {
    return -Math.log(1 - val) / rate;
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
    return -Math.log(random.nextDouble()) / rate + location;
  }

  @Override
  public String toString() {
    return "ExponentialDistribution(rate=" + rate + ", location=" + location + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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

      DoubleParameter locP = new DoubleParameter(LOCATION_ID);
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