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

import elki.math.MathUtil;
import elki.utilities.Alias;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Log-Normal distribution.
 * <p>
 * The parameterization of this class is somewhere inbetween of GNU R and SciPy.
 * Similar to GNU R we use the logmean and logstddev. Similar to Scipy, we also
 * have a location parameter that shifts the distribution.
 * <p>
 * Our implementation maps to SciPy's as follows:
 * <code>scipy.stats.lognorm(logstddev, shift, FastMath.exp(logmean))</code>
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Alias({ "lognormal" })
public class LogNormalDistribution implements Distribution {
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
   */
  public LogNormalDistribution(double logmean, double logstddev, double shift) {
    this.logmean = logmean;
    this.logstddev = logstddev;
    this.shift = shift;
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
   * <p>
   * \[
   * \frac{1}{\sqrt{2\pi}\log(\sigma) x}
   * \exp \left(-\frac{(\log (x)-\log (\mu))^2}{2\log (\sigma)^2}\right)
   * \]
   * 
   * @param x The value.
   * @param logmu The log mean
   * @param logsigma The log standard deviation.
   * @return PDF of the given log normal distribution at x
   */
  public static double pdf(double x, double logmu, double logsigma) {
    if(x <= 0.) {
      return 0.;
    }
    final double xrel = (FastMath.log(x) - logmu) / logsigma;
    return 1 / (MathUtil.SQRTTWOPI * logsigma * x) * FastMath.exp(-.5 * xrel * xrel);
  }

  /**
   * Log probability density function of the normal distribution.
   * <p>
   * \[
   * \log\left(\frac{1}{\sqrt{2\pi}\sigma x} \exp \left(-\frac{(\log
   * x-\mu)^2}{2\sigma^2}\right)\right)
   * \]
   * \[ =
   * \log\frac{1}{\sqrt{2\pi}} - \log(\log(\sigma) x) - \frac{1}{2}
   * \left(\frac{\log (x) - \log (\mu)}{\log(\sigma)}\right)^2
   * \]
   * 
   * @param x The value
   * @param logmu The log mean
   * @param logsigma The log standard deviation
   * @return logPDF of the given log normal distribution at x
   */
  public static double logpdf(double x, double logmu, double logsigma) {
    if(x <= 0.) {
      return Double.NEGATIVE_INFINITY;
    }
    final double xrel = (FastMath.log(x) - logmu) / logsigma;
    return MathUtil.LOG_ONE_BY_SQRTTWOPI - FastMath.log(logsigma * x) - .5 * xrel * xrel;
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * <p>
   * \[
   * \frac{1}{2} \left(1+\text{erf}\left(\frac{\log (x)-\log (\mu)}{\sqrt{2} \log(\sigma)}\right)\right)
   * \]
   * 
   * @param x value to evaluate CDF at
   * @param logmu log mean value
   * @param logsigma log standard deviation
   * @return The CDF of the given log normal distribution at x
   */
  public static double cdf(double x, double logmu, double logsigma) {
    if(x <= 0.) {
      return 0.;
    }
    return .5 * (1 + NormalDistribution.erf((FastMath.log(x) - logmu) / (MathUtil.SQRT2 * logsigma)));
  }

  /**
   * Inverse cumulative probability density function (probit) of a normal
   * distribution.
   * <p>
   * \[
   * \exp\left(\log (\mu)+\sqrt{2}\log (\sigma) \text{erf}^{-1}(2x-1)\right)
   * \]
   * 
   * @param x value to evaluate probit function at
   * @param logmu log mean value
   * @param logsigma log standard deviation
   * @return The probit of the given log normal distribution at x
   */
  public static double quantile(double x, double logmu, double logsigma) {
    return FastMath.exp(logmu + logsigma * NormalDistribution.standardNormalQuantile(x));
  }

  @Override
  public double nextRandom(Random random) {
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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new DoubleParameter(LOGMEAN_ID) //
          .grab(config, x -> logmean = x);
      new DoubleParameter(LOGSTDDEV_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> logsigma = x);
      new DoubleParameter(SHIFT_ID, 0.) //
          .grab(config, x -> shift = x);
    }

    @Override
    public LogNormalDistribution make() {
      return new LogNormalDistribution(logmean, logsigma, shift);
    }
  }
}
