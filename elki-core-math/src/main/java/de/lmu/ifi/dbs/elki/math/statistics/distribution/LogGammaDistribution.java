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
 * Log-Gamma Distribution, with random generation and density functions.
 * 
 * This distribution can be outlined as Y=e^X with X Gamma distributed.
 * 
 * Note: this is a different loggamma than scipy uses.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class LogGammaDistribution extends AbstractDistribution {
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
   * @param shift Location offset
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param random Random generator
   */
  public LogGammaDistribution(double k, double theta, double shift, Random random) {
    super(random);
    if(!(k > 0.0) || !(theta > 0.0)) { // Note: also tests for NaNs!
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution: " + k + " " + theta);
    }

    this.k = k;
    this.theta = theta;
    this.shift = shift;
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param shift Location offset
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param random Random generator
   */
  public LogGammaDistribution(double k, double theta, double shift, RandomFactory random) {
    super(random);
    if(!(k > 0.0) || !(theta > 0.0)) { // Note: also tests for NaNs!
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution: " + k + " " + theta);
    }

    this.k = k;
    this.theta = theta;
    this.shift = shift;
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param shift Location offset
   */
  public LogGammaDistribution(double k, double theta, double shift) {
    this(k, theta, shift, (Random) null);
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
  public double nextRandom() {
    return Math.exp(GammaDistribution.nextRandom(k, theta, random)) + shift;
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
    if(x <= 1.) {
      return 0.;
    }
    return GammaDistribution.regularizedGammaP(k, Math.log(x));
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
    if(x <= 1.) {
      return 0.;
    }
    return GammaDistribution.logregularizedGammaP(k, Math.log(x));
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
    if(x <= 1.) {
      return 0.;
    }
    return Math.pow(theta, -k) / GammaDistribution.gamma(k) * Math.pow(x, -(1. / theta + 1.)) * Math.pow(Math.log(x), k - 1.);
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
    if(x <= 1.) {
      return Double.NEGATIVE_INFINITY;
    }
    final double logx = Math.log(x);
    return -k * Math.log(theta) - GammaDistribution.logGamma(k) - (1. / theta + 1.) * logx + (k - 1) * Math.log(logx);
  }

  /**
   * Compute probit (inverse cdf) for LogGamma distributions.
   * 
   * @param p Probability
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return Probit for Gamma distribution
   */
  public static double quantile(double p, double k, double theta, double shift) {
    return Math.exp(GammaDistribution.quantile(p, k, theta)) + shift;
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
     * Shifting offset parameter.
     */
    public static final OptionID SHIFT_ID = new OptionID("distribution.loggamma.shift", "Shift offset parameter.");

    /** Parameters. */
    double k, theta, shift;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter kP = new DoubleParameter(GammaDistribution.Parameterizer.K_ID);
      if(config.grab(kP)) {
        k = kP.doubleValue();
      }

      DoubleParameter thetaP = new DoubleParameter(GammaDistribution.Parameterizer.THETA_ID);
      if(config.grab(thetaP)) {
        theta = thetaP.doubleValue();
      }

      DoubleParameter shiftP = new DoubleParameter(SHIFT_ID);
      if(config.grab(shiftP)) {
        shift = shiftP.doubleValue();
      }
    }

    @Override
    protected LogGammaDistribution makeInstance() {
      return new LogGammaDistribution(k, theta, shift, rnd);
    }
  }
}
