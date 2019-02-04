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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Exponentially modified Gaussian (EMG) distribution (ExGaussian distribution)
 * is a combination of a normal distribution and an exponential distribution.
 * 
 * Note that scipy uses a subtly different parameterization.
 *
 * TODO: implement quantile function!
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "exgaussian" })
public class ExponentiallyModifiedGaussianDistribution extends AbstractDistribution {
  /**
   * Mean value for the generator
   */
  private double mean;

  /**
   * Standard deviation
   */
  private double stddev;

  /**
   * Exponential rate.
   */
  private double lambda;

  /**
   * Constructor for ExGaussian distribution
   *
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param lambda Rate
   * @param random Random
   */
  public ExponentiallyModifiedGaussianDistribution(double mean, double stddev, double lambda, Random random) {
    super(random);
    this.mean = mean;
    this.stddev = stddev;
    this.lambda = lambda;
  }

  /**
   * Constructor for ExGaussian distribution
   *
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param lambda Rate
   * @param random Random
   */
  public ExponentiallyModifiedGaussianDistribution(double mean, double stddev, double lambda, RandomFactory random) {
    super(random);
    this.mean = mean;
    this.stddev = stddev;
    this.lambda = lambda;
  }

  /**
   * Constructor for ExGaussian distribution
   *
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param lambda Rate
   */
  public ExponentiallyModifiedGaussianDistribution(double mean, double stddev, double lambda) {
    this(mean, stddev, lambda, (Random) null);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, mean, stddev, lambda);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, mean, stddev, lambda);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mean, stddev, lambda);
  }

  /**
   * @deprecated Not yet implemented!
   */
  @Override
  @Deprecated
  public double quantile(double q) {
    return quantile(q, mean, stddev, lambda);
  }

  @Override
  public double nextRandom() {
    double no = mean + random.nextGaussian() * stddev;
    double ex = -FastMath.log(random.nextDouble()) / lambda;
    return no + ex;
  }

  @Override
  public String toString() {
    return "ExGaussianDistribution(mean=" + mean + ", stddev=" + stddev + ", lambda=" + lambda + ")";
  }

  /**
   * @return the mean
   */
  public double getMean() {
    return mean;
  }

  /**
   * @return the standard deviation
   */
  public double getStddev() {
    return stddev;
  }

  /**
   * @return the lambda value.
   */
  public double getLambda() {
    return lambda;
  }

  /**
   * Probability density function of the ExGaussian distribution.
   *
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @param lambda Rate parameter.
   * @return PDF of the given exgauss distribution at x.
   */
  public static double pdf(double x, double mu, double sigma, double lambda) {
    final double dx = x - mu;
    final double lss = lambda * sigma * sigma;
    final double erfc = NormalDistribution.erfc((lss - dx) / (sigma * MathUtil.SQRT2));
    return erfc > 0. ? .5 * lambda * FastMath.exp(lambda * (lss * .5 - dx)) * erfc : (x == x) ? 0. : Double.NaN;
  }

  /**
   * Probability density function of the ExGaussian distribution.
   *
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @param lambda Rate parameter.
   * @return PDF of the given exgauss distribution at x.
   */
  public static double logpdf(double x, double mu, double sigma, double lambda) {
    final double dx = x - mu;
    final double lss = lambda * sigma * sigma;
    final double erfc = NormalDistribution.erfc((lss - dx) / (sigma * MathUtil.SQRT2));
    return erfc > 0 ? FastMath.log(.5 * lambda * erfc) + lambda * (lss * .5 - dx) : (x == x) ? Double.NEGATIVE_INFINITY : Double.NaN;
  }

  /**
   * Cumulative probability density function (CDF) of an exgauss distribution.
   *
   * @param x value to evaluate CDF at.
   * @param mu Mean value.
   * @param sigma Standard deviation.
   * @param lambda Rate parameter.
   * @return The CDF of the given exgauss distribution at x.
   */
  public static double cdf(double x, double mu, double sigma, double lambda) {
    if(x == Double.NEGATIVE_INFINITY) {
      return 0.;
    }
    if(x == Double.POSITIVE_INFINITY) {
      return 1.;
    }
    final double u = lambda * (x - mu);
    final double v = lambda * sigma;
    final double v2 = v * v;
    final double logphi = FastMath.log(NormalDistribution.cdf(u, v2, v));
    return NormalDistribution.cdf(u, 0., v) - FastMath.exp(-u + v2 * .5 + logphi);
  }

  /**
   * Inverse cumulative probability density function (probit) of an exgauss
   * distribution.
   *
   * @param x value to evaluate probit function at.
   * @param mu Mean value.
   * @param sigma Standard deviation.
   * @param lambda Rate parameter.
   * @return The probit of the given exgauss distribution at x.
   *
   * @deprecated Not yet implemented!
   */
  @Deprecated
  public static double quantile(double x, double mu, double sigma, double lambda) {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Rate option, same as
     * {@link ExponentialDistribution.Parameterizer#RATE_ID}.
     */
    public static final OptionID RATE_ID = ExponentialDistribution.Parameterizer.RATE_ID;

    /** Parameters. */
    double mean, stddev, lambda;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter locP = new DoubleParameter(LOCATION_ID) //
          .setDefaultValue(0.);
      if(config.grab(locP)) {
        mean = locP.doubleValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID);
      if(config.grab(scaleP)) {
        stddev = scaleP.doubleValue();
      }

      DoubleParameter rateP = new DoubleParameter(RATE_ID);
      if(config.grab(rateP)) {
        lambda = rateP.doubleValue();
      }
    }

    @Override
    protected ExponentiallyModifiedGaussianDistribution makeInstance() {
      return new ExponentiallyModifiedGaussianDistribution(mean, stddev, lambda, rnd);
    }
  }
}
