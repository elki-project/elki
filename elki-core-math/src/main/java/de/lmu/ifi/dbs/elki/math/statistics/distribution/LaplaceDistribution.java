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

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Laplace distribution also known as double exponential distribution
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias("DoubleExponentialDistribution")
public class LaplaceDistribution extends AbstractDistribution {
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
    this(rate, 0., (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   */
  public LaplaceDistribution(double rate, double location) {
    this(rate, location, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param random Random generator
   */
  public LaplaceDistribution(double rate, Random random) {
    this(rate, 0., random);
  }

  /**
   * Constructor.
   * 
   * @param rate Rate parameter (1/scale)
   * @param location Location parameter
   * @param random Random generator
   */
  public LaplaceDistribution(double rate, double location, Random random) {
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
  public LaplaceDistribution(double rate, double location, RandomFactory random) {
    super(random);
    this.rate = rate;
    this.location = location;
  }

  /**
   * Get the rate parameter.
   * 
   * @return Rate parameter
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
    return .5 * rate * FastMath.exp(-rate * Math.abs(val - location));
  }

  /**
   * PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double pdf(double val, double rate) {
    return .5 * rate * FastMath.exp(-rate * Math.abs(val));
  }

  @Override
  public double logpdf(double val) {
    return FastMath.log(.5 * rate) - rate * Math.abs(val - location);
  }

  /**
   * PDF, static version
   * 
   * @param val Value to compute PDF at
   * @param rate Rate parameter (1/scale)
   * @return probability density
   */
  public static double logpdf(double val, double rate) {
    return FastMath.log(.5 * rate) - rate * Math.abs(val);
  }

  @Override
  public double cdf(double val) {
    return cdf(val - location, rate);
  }

  /**
   * Cumulative density, static version
   * 
   * @param val Value to compute CDF at
   * @param rate Rate parameter (1/scale)
   * @return cumulative density
   */
  public static double cdf(double val, double rate) {
    final double v = .5 * FastMath.exp(-rate * Math.abs(val));
    return (v == Double.POSITIVE_INFINITY) ? ((val <= 0) ? 0 : 1) : //
        (val < 0) ? v : 1 - v;
  }

  @Override
  public double quantile(double val) {
    if(val < .5) {
      return FastMath.log(2 * val) / rate + location;
    }
    else {
      return -FastMath.log(2. - 2. * val) / rate + location;
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
    if(val < .5) {
      return FastMath.log(2 * val) / rate + location;
    }
    else {
      return -FastMath.log(2. - 2. * val) / rate + location;
    }
  }

  /**
   * This method currently uses the naive approach of returning
   * <code>-log(uniform)</code>.
   */
  @Override
  public double nextRandom() {
    double val = random.nextDouble();
    if(val < .5) {
      return FastMath.log(2 * val) / rate + location;
    }
    else {
      return -FastMath.log(2. - 2. * val) / rate + location;
    }
  }

  @Override
  public String toString() {
    return "LaplaceDistribution(rate=" + rate + ", location=" + location + ")";
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
    public static final OptionID RATE_ID = new OptionID("distribution.laplace.rate", "Laplace distribution rate (lambda) parameter (inverse of scale).");

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
    protected LaplaceDistribution makeInstance() {
      return new LaplaceDistribution(rate, location, rnd);
    }
  }
}
