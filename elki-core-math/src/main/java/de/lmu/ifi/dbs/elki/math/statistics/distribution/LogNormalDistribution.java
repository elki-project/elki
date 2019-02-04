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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Log-Normal distribution.
 * 
 * The parameterization of this class is somewhere inbetween of GNU R and SciPy.
 * Similar to GNU R we use the logmean and logstddev. Similar to Scipy, we also
 * have a location parameter that shifts the distribution.
 * 
 * Our implementation maps to SciPy's as follows:
 * <tt>scipy.stats.lognorm(logstddev, shift, FastMath.exp(logmean))</tt>
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Alias({ "lognormal" })
public class LogNormalDistribution extends AbstractDistribution {
  /**
   * Mean value for the generator
   */
  private double logmean;

  /**
   * Standard deviation
   */
  private double logstddev;

  /**
   * Additional shift factor
   */
  private double shift = 0.;

  /**
   * Constructor for Log-Normal distribution
   * 
   * @param logmean Mean
   * @param logstddev Standard Deviation
   * @param shift Shifting offset
   * @param random Random generator
   */
  public LogNormalDistribution(double logmean, double logstddev, double shift, Random random) {
    super(random);
    this.logmean = logmean;
    this.logstddev = logstddev;
    this.shift = shift;
  }

  /**
   * Constructor for Log-Normal distribution
   * 
   * @param logmean Mean
   * @param logstddev Standard Deviation
   * @param shift Shifting offset
   * @param random Random generator
   */
  public LogNormalDistribution(double logmean, double logstddev, double shift, RandomFactory random) {
    super(random);
    this.logmean = logmean;
    this.logstddev = logstddev;
    this.shift = shift;
  }

  /**
   * Constructor.
   * 
   * @param logmean Mean
   * @param logstddev Standard deviation
   * @param shift Shifting offset
   */
  public LogNormalDistribution(double logmean, double logstddev, double shift) {
    this(logmean, logstddev, shift, (Random) null);
  }

  /**
   * Get the log mean value.
   *
   * @return logmean
   */
  public double getLogMean() {
    return logmean;
  }

  /**
   * Get the log standard deviation.
   *
   * @return log standard deviation
   */
  public double getLogStddev() {
    return logstddev;
  }

  /**
   * Get the distribution shift.
   * 
   * @return Shift
   */
  public double getShift() {
    return shift;
  }

  @Override
  public double pdf(double val) {
    return pdf(val - shift, logmean, logstddev);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val - shift, logmean, logstddev);
  }

  @Override
  public double cdf(double val) {
    return cdf(val - shift, logmean, logstddev);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, logmean, logstddev) + shift;
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi)*sigma*x) * e^(-log(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double pdf(double x, double mu, double sigma) {
    if(x <= 0.) {
      return 0.;
    }
    final double xrel = (FastMath.log(x) - mu) / sigma;
    return 1 / (MathUtil.SQRTTWOPI * sigma * x) * FastMath.exp(-.5 * xrel * xrel);
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi)*sigma*x) * e^(-log(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return logPDF of the given normal distribution at x.
   */
  public static double logpdf(double x, double mu, double sigma) {
    if(x <= 0.) {
      return Double.NEGATIVE_INFINITY;
    }
    final double xrel = (FastMath.log(x) - mu) / sigma;
    return MathUtil.LOG_ONE_BY_SQRTTWOPI - FastMath.log(sigma * x) - .5 * xrel * xrel;
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * 
   * @param x value to evaluate CDF at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The CDF of the given normal distribution at x.
   */
  public static double cdf(double x, double mu, double sigma) {
    if(x <= 0.) {
      return 0.;
    }
    return .5 * (1 + NormalDistribution.erf((FastMath.log(x) - mu) / (MathUtil.SQRT2 * sigma)));
  }

  /**
   * Inverse cumulative probability density function (probit) of a normal
   * distribution.
   * 
   * @param x value to evaluate probit function at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The probit of the given normal distribution at x.
   */
  public static double quantile(double x, double mu, double sigma) {
    return FastMath.exp(mu + sigma * NormalDistribution.standardNormalQuantile(x));
  }

  @Override
  public double nextRandom() {
    return FastMath.exp(logmean + random.nextGaussian() * logstddev) + shift;
  }

  @Override
  public String toString() {
    return "LogNormalDistribution(logmean=" + logmean + ", logstddev=" + logstddev + ", shift=" + shift + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * LogMean parameter
     */
    public static final OptionID LOGMEAN_ID = new OptionID("distribution.lognormal.logmean", "Mean of the distribution before logscaling.");

    /**
     * LogScale parameter
     */
    public static final OptionID LOGSTDDEV_ID = new OptionID("distribution.lognormal.logstddev", "Standard deviation of the distribution before logscaling.");

    /**
     * Shift parameter
     */
    public static final OptionID SHIFT_ID = new OptionID("distribution.lognormal.shift", "Shifting offset, so the distribution does not begin at 0.");

    /** Parameters. */
    double shift, logmean, logsigma;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter logmeanP = new DoubleParameter(LOGMEAN_ID);
      if(config.grab(logmeanP)) {
        logmean = logmeanP.doubleValue();
      }

      DoubleParameter logsigmaP = new DoubleParameter(LOGSTDDEV_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(logsigmaP)) {
        logsigma = logsigmaP.doubleValue();
      }

      DoubleParameter shiftP = new DoubleParameter(SHIFT_ID, 0.);
      if(config.grab(shiftP)) {
        shift = shiftP.doubleValue();
      }
    }

    @Override
    protected LogNormalDistribution makeInstance() {
      return new LogNormalDistribution(logmean, logsigma, shift, rnd);
    }
  }
}
