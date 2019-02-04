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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Generalized normal distribution by adding a skew term, similar to lognormal
 * distributions.
 * <p>
 * This is one kind of generalized normal distributions. Note that there are
 * multiple that go by the name of a "Generalized Normal Distribution"; this is
 * what is currently called "version 2" in English Wikipedia.
 * <p>
 * Reference:
 * <p>
 * J. R. M. Hosking, J. R. Wallis<br>
 * Regional frequency analysis: an approach based on L-moments
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. R. M. Hosking, J. R. Wallis", //
    title = "Regional frequency analysis: an approach based on L-moments", //
    booktitle = "Regional frequency analysis: an approach based on L-moments", //
    url = "https://doi.org/10.1017/CBO9780511529443", //
    bibkey = "doi:10.1017/CBO9780511529443")
public class SkewGeneralizedNormalDistribution extends AbstractDistribution {
  /**
   * Location.
   */
  private double loc;

  /**
   * Scale.
   */
  private double scale;

  /**
   * Skew.
   */
  private double skew;

  /**
   * Constructor for Gaussian distribution
   *
   * @param loc Location
   * @param scale Scale
   * @param skew Skew
   * @param random Random generator
   */
  public SkewGeneralizedNormalDistribution(double loc, double scale, double skew, Random random) {
    super(random);
    this.loc = loc;
    this.scale = scale;
    this.skew = skew;
  }

  /**
   * Constructor for Gaussian distribution
   *
   * @param loc Location
   * @param scale Scale
   * @param skew Skew
   * @param random Random generator
   */
  public SkewGeneralizedNormalDistribution(double loc, double scale, double skew, RandomFactory random) {
    super(random);
    this.loc = loc;
    this.scale = scale;
    this.skew = skew;
  }

  /**
   * Constructor for Gaussian distribution
   * 
   * @param loc Location
   * @param scale Scale
   * @param skew Skew
   */
  public SkewGeneralizedNormalDistribution(double loc, double scale, double skew) {
    this(loc, scale, skew, (Random) null);
  }

  /**
   * Get the location parameter
   *
   * @return Location
   */
  public double getLocation() {
    return loc;
  }

  /**
   * Get the scale parameter
   * 
   * @return Scale
   */
  public double getScale() {
    return scale;
  }

  /**
   * Get the skew parameter.
   *
   * @return Skew parameter
   */
  public double getSkew() {
    return skew;
  }

  @Override
  public double pdf(double val) {
    return pdf(val, loc, scale, skew);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, loc, scale, skew);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, loc, scale, skew);
  }

  @Override
  public double quantile(double q) {
    return quantile(q, loc, scale, skew);
  }

  @Override
  public double nextRandom() {
    double y = random.nextGaussian();
    if(Math.abs(skew) > 0.) {
      y = (1. - FastMath.exp(-skew * y)) / skew;
    }
    return loc + scale * y;

  }

  @Override
  public String toString() {
    return "SkewNormalDistribution(mean=" + loc + ", stddev=" + scale + ", skew=" + skew + ")";
  }

  /**
   * Probability density function of the skewed normal distribution.
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double pdf(double x, double mu, double sigma, double skew) {
    if(x != x) {
      return Double.NaN;
    }
    x = (x - mu) / sigma; // Scale
    if(skew == 0.) {
      return MathUtil.ONE_BY_SQRTTWOPI / sigma * FastMath.exp(-.5 * x * x);
    }
    final double y = -FastMath.log1p(-skew * x) / skew;
    if(y != y || y == Double.POSITIVE_INFINITY || y == Double.NEGATIVE_INFINITY) { // NaN
      return 0.;
    }
    return MathUtil.ONE_BY_SQRTTWOPI / sigma * FastMath.exp(-.5 * y * y) / (1 - skew * x);
  }

  /**
   * Probability density function of the skewed normal distribution.
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return log PDF of the given normal distribution at x.
   */
  public static double logpdf(double x, double mu, double sigma, double skew) {
    if(x != x) {
      return Double.NaN;
    }
    x = (x - mu) / sigma;
    if(skew == 0.) {
      return MathUtil.LOG_ONE_BY_SQRTTWOPI - FastMath.log(sigma) - .5 * x * x;
    }
    double y = -FastMath.log(1. - skew * x) / skew;
    if(y != y || y == Double.NEGATIVE_INFINITY || y == Double.NEGATIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    return -.5 * y * y - FastMath.log(MathUtil.ONE_BY_SQRTTWOPI * sigma * (1 - skew * x));
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * 
   * @param x value to evaluate CDF at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The CDF of the given normal distribution at x.
   */
  public static double cdf(double x, double mu, double sigma, double skew) {
    x = (x - mu) / sigma;
    if(Math.abs(skew) > 0.) {
      double tmp = 1 - skew * x;
      if(tmp < 1e-15) {
        return (skew < 0.) ? 0. : 1.;
      }
      x = -FastMath.log(tmp) / skew;
    }
    return .5 + .5 * NormalDistribution.erf(x * MathUtil.SQRTHALF);
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
  public static double quantile(double x, double mu, double sigma, double skew) {
    x = NormalDistribution.standardNormalQuantile(x);
    if(Math.abs(skew) > 0.) {
      x = (1. - FastMath.exp(-skew * x)) / skew;
    }
    return mu + sigma * x;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Skew parameter
     */
    public static final OptionID SKEW_ID = new OptionID("distribution.skewgnormal.skew", "Skew of the distribution.");

    /** Parameters */
    double mean, sigma, skew;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter meanP = new DoubleParameter(LOCATION_ID);
      if(config.grab(meanP)) {
        mean = meanP.doubleValue();
      }

      DoubleParameter sigmaP = new DoubleParameter(SCALE_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }

      DoubleParameter skewP = new DoubleParameter(SKEW_ID);
      if(config.grab(skewP)) {
        skew = skewP.doubleValue();
      }
    }

    @Override
    protected SkewGeneralizedNormalDistribution makeInstance() {
      return new SkewGeneralizedNormalDistribution(mean, sigma, skew, rnd);
    }
  }
}
