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
package elki.math.statistics.distribution;

import java.util.Random;

import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

import net.jafama.FastMath;

/**
 * Log-Gamma Distribution, with random generation and density functions.
 * <p>
 * This distribution can be outlined as Y ~ exp(Gamma) or equivalently Log(Y) ~
 * Gamma.
 * <p>
 * Note: this is a different loggamma than scipy uses, but corresponds to the
 * Log Gamma Distribution of Wolfram, who notes that it is "at times confused
 * with ExpGammaDistribution".
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class LogGammaDistribution implements Distribution {
  /**
   * Alpha == k.
   */
  private final double k;

  /**
   * Theta == 1 / Beta.
   */
  private final double theta;

  /**
   * Translation offset.
   */
  private final double shift;

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   */
  public LogGammaDistribution(double k, double theta) {
    this(k, theta, 0.);
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param shift Location offset
   */
  public LogGammaDistribution(double k, double theta, double shift) {
    if(!(k > 0.0) || !(theta > 0.0)) { // Note: also tests for NaNs!
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution: " + k + " " + theta);
    }
    this.k = k;
    this.theta = theta;
    this.shift = shift;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, k, theta, shift);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, k, theta, shift);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, k, theta, shift);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, k, theta, shift);
  }

  @Override
  public double nextRandom(Random random) {
    return FastMath.exp(GammaDistribution.nextRandom(k, theta, random)) + shift;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "LogGammaDistribution(k=" + k + ", theta=" + theta + ", shift=" + shift + ")";
  }

  /**
   * @return the value of k
   */
  public double getK() {
    return k;
  }

  /**
   * @return the standard deviation
   */
  public double getTheta() {
    return theta;
  }

  /**
   * The CDF, static version.
   * 
   * @param x Value
   * @param k Shape k
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return cdf value
   */
  public static double cdf(double x, double k, double theta, double shift) {
    x = (x - shift);
    return x <= 0. ? 0. : GammaDistribution.regularizedGammaP(k, FastMath.log1p(x) * theta);
  }

  /**
   * The log CDF, static version.
   * 
   * @param x Value
   * @param k Shape k
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return cdf value
   */
  public static double logcdf(double x, double k, double theta, double shift) {
    x = (x - shift);
    return x <= 0. ? -Double.NEGATIVE_INFINITY : GammaDistribution.logregularizedGammaP(k, FastMath.log1p(x) * theta);
  }

  /**
   * LogGamma distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return probability density
   */
  public static double pdf(double x, double k, double theta, double shift) {
    x = (x - shift);
    return x <= 0. ? 0. : FastMath.pow(theta, k) / GammaDistribution.gamma(k) * FastMath.pow(1 + x, -(theta + 1.)) * FastMath.pow(FastMath.log1p(x), k - 1.);
  }

  /**
   * LogGamma distribution logPDF
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return log probability density
   */
  public static double logpdf(double x, double k, double theta, double shift) {
    x = (x - shift);
    if(x <= 0.) {
      return Double.NEGATIVE_INFINITY;
    }
    final double log1px = FastMath.log1p(x);
    return k * FastMath.log(theta) - GammaDistribution.logGamma(k) - (theta + 1.) * log1px + (k - 1) * FastMath.log(log1px);
  }

  /**
   * Compute probit (inverse cdf) for LogGamma distributions.
   * 
   * @param p Probability
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param shift Shift parameter
   * @return Probit for Gamma distribution
   */
  public static double quantile(double p, double k, double theta, double shift) {
    return FastMath.exp(GammaDistribution.quantile(p, k, theta)) + shift;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Shifting offset parameter.
     */
    public static final OptionID SHIFT_ID = new OptionID("distribution.loggamma.shift", "Shift offset parameter.");

    /** Parameters. */
    double k, theta, shift;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(GammaDistribution.Par.K_ID) //
          .grab(config, x -> k = x);
      new DoubleParameter(GammaDistribution.Par.THETA_ID) //
          .grab(config, x -> theta = x);
      new DoubleParameter(SHIFT_ID) //
          .grab(config, x -> shift = x);
    }

    @Override
    public LogGammaDistribution make() {
      return new LogGammaDistribution(k, theta, shift);
    }
  }
}
