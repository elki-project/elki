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
 * Exp-Gamma Distribution, with random generation and density functions.
 *
 * This distribution can be outlined as Y ~ log[Gamma] distributed, or
 * equivalently exp(Y) ~ Gamma.
 *
 * Note: this matches the loggamma of SciPy, whereas Wolfram calls this the
 * Exponential Gamma Distribution "at times confused with the
 * LogGammaDistribution".
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class ExpGammaDistribution extends AbstractDistribution {
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
   * @param shift Location offset
   * @param random Random generator
   */
  public ExpGammaDistribution(double k, double theta, double shift, Random random) {
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
   * @param random Random generator
   */
  public ExpGammaDistribution(double k, double theta, double shift, RandomFactory random) {
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
  public ExpGammaDistribution(double k, double theta, double shift) {
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
    return quantile(random.nextDouble(), k, theta, shift);
    // return FastMath.log(GammaDistribution.nextRandom(k, 1., random)) / theta
    // + shift;
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "ExpGammaDistribution(k=" + k + ", theta=" + theta + ", shift=" + shift + ")";
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
    if(x == Double.NEGATIVE_INFINITY) {
      return 0.;
    }
    if(x == Double.POSITIVE_INFINITY) {
      return 1.;
    }
    if(x != x) {
      return Double.NaN;
    }
    final double e = FastMath.exp((x - shift) * theta);
    return e < Double.POSITIVE_INFINITY ? GammaDistribution.regularizedGammaP(k, e) : 1.;
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
    final double e = FastMath.exp((x - shift) * theta);
    return e < Double.POSITIVE_INFINITY ? GammaDistribution.logregularizedGammaP(k, e) : 0.;
  }

  /**
   * ExpGamma distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return probability density
   */
  public static double pdf(double x, double k, double theta, double shift) {
    if(x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY) {
      return 0.;
    }
    if(x != x) {
      return Double.NaN;
    }
    x = (x - shift) * theta;
    final double ex = FastMath.exp(x);
    return ex < Double.POSITIVE_INFINITY ? FastMath.exp(k * x - ex - GammaDistribution.logGamma(k)) * theta : 0.;
  }

  /**
   * ExpGamma distribution logPDF
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return log probability density
   */
  public static double logpdf(double x, double k, double theta, double shift) {
    if(x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    if(x != x) {
      return Double.NaN;
    }
    x = (x - shift) * theta;
    double ex = FastMath.exp(x);
    return ex < Double.POSITIVE_INFINITY ? k * x - ex - GammaDistribution.logGamma(k) + FastMath.log(theta) : Double.NEGATIVE_INFINITY;
  }

  /**
   * Compute probit (inverse cdf) for ExpGamma distributions.
   * 
   * @param p Probability
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param shift Shift parameter
   * @return Probit for ExpGamma distribution
   */
  public static double quantile(double p, double k, double theta, double shift) {
    return FastMath.log(GammaDistribution.quantile(p, k, 1)) / theta + shift;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Theta parameter, same as
     * {@link GammaDistribution.Parameterizer#THETA_ID}.
     */
    public static final OptionID THETA_ID = GammaDistribution.Parameterizer.THETA_ID;

    /**
     * k parameter, same as {@link GammaDistribution.Parameterizer#K_ID}.
     */
    public static final OptionID K_ID = GammaDistribution.Parameterizer.K_ID;

    /**
     * Shifting offset parameter.
     */
    public static final OptionID SHIFT_ID = new OptionID("distribution.expgamma.shift", "Shift offset parameter.");

    /**
     * Alpha == k.
     */
    double k;

    /**
     * Theta == 1 / Beta.
     */
    double theta;

    /**
     * Translation offset.
     */
    double shift;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter kP = new DoubleParameter(K_ID);
      if(config.grab(kP)) {
        k = kP.doubleValue();
      }

      DoubleParameter thetaP = new DoubleParameter(THETA_ID);
      if(config.grab(thetaP)) {
        theta = thetaP.doubleValue();
      }

      DoubleParameter shiftP = new DoubleParameter(SHIFT_ID) //
          .setDefaultValue(0.);
      if(config.grab(shiftP)) {
        shift = shiftP.doubleValue();
      }
    }

    @Override
    protected ExpGammaDistribution makeInstance() {
      return new ExpGammaDistribution(k, theta, shift, rnd);
    }
  }
}
