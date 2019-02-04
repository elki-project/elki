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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Generalized Extreme Value (GEV) distribution, also known as Fisher–Tippett
 * distribution.
 * 
 * This is a generalization of the Frechnet, Gumbel and (reversed) Weibull
 * distributions.
 * 
 * Implementation notice: In ELKI 0.8.0, the sign of the shape was negated.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class GeneralizedExtremeValueDistribution extends AbstractDistribution {
  /**
   * Parameters (location, scale, shape)
   */
  final double mu, sigma, k;

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   */
  public GeneralizedExtremeValueDistribution(double mu, double sigma, double k) {
    this(mu, sigma, k, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   * @param random Random number generator
   */
  public GeneralizedExtremeValueDistribution(double mu, double sigma, double k, RandomFactory random) {
    super(random);
    this.mu = mu;
    this.sigma = sigma;
    this.k = k;
  }

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   * @param random Random number generator
   */
  public GeneralizedExtremeValueDistribution(double mu, double sigma, double k, Random random) {
    super(random);
    this.mu = mu;
    this.sigma = sigma;
    this.k = k;
  }

  /**
   * Get location.
   * 
   * @return Location
   */
  public double getMu() {
    return mu;
  }

  /**
   * Get sigma.
   * 
   * @return Sigma
   */
  public double getSigma() {
    return sigma;
  }

  /**
   * Get the k parameter.
   * 
   * @return k
   */
  public double getK() {
    return k;
  }

  @Override
  public double pdf(double x) {
    return pdf(x, mu, sigma, k);
  }

  /**
   * PDF of GEV distribution
   * 
   * @param x Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   * @return PDF at position x.
   */
  public static double pdf(double x, double mu, double sigma, double k) {
    if(x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY) {
      return 0.;
    }
    x = (x - mu) / sigma;
    if(k > 0 || k < 0) {
      if(k * x > 1) {
        return 0.;
      }
      double t = FastMath.log(1 - k * x);
      return t == Double.NEGATIVE_INFINITY ? 1. / sigma //
          : t == Double.POSITIVE_INFINITY ? 0. //
              : FastMath.exp((1 - k) * t / k - FastMath.exp(t / k)) / sigma;
    }
    else { // Gumbel case:
      return FastMath.exp(-x - FastMath.exp(-x)) / sigma;
    }
  }

  @Override
  public double logpdf(double x) {
    return logpdf(x, mu, sigma, k);
  }

  /**
   * log PDF of GEV distribution
   * 
   * @param x Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   * @return PDF at position x.
   */
  public static double logpdf(double x, double mu, double sigma, double k) {
    if(x == Double.POSITIVE_INFINITY || x == Double.NEGATIVE_INFINITY) {
      return Double.NEGATIVE_INFINITY;
    }
    x = (x - mu) / sigma;
    if(k > 0 || k < 0) {
      if(k * x > 1) {
        return Double.NEGATIVE_INFINITY;
      }
      double t = FastMath.log(1 - k * x);
      return t == Double.NEGATIVE_INFINITY ? -FastMath.log(sigma) //
          : t == Double.POSITIVE_INFINITY ? Double.NEGATIVE_INFINITY //
              : (1 - k) * t / k - FastMath.exp(t / k) - FastMath.log(sigma);
    }
    else { // Gumbel case:
      return -x - FastMath.exp(-x) - FastMath.log(sigma);
    }
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mu, sigma, k);
  }

  /**
   * CDF of GEV distribution
   * 
   * @param val Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   * @return CDF at position x.
   */
  public static double cdf(double val, double mu, double sigma, double k) {
    final double x = (val - mu) / sigma;
    if(k > 0 || k < 0) {
      if(k * x > 1) {
        return k > 0 ? 1 : 0;
      }
      return FastMath.exp(-FastMath.exp(FastMath.log(1 - k * x) / k));
    }
    else { // Gumbel case:
      return FastMath.exp(-FastMath.exp(-x));
    }
  }

  @Override
  public double quantile(double val) {
    return quantile(val, mu, sigma, k);
  }

  /**
   * Quantile function of GEV distribution
   * 
   * @param val Value
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   * @return Quantile function at position x.
   */
  public static double quantile(double val, double mu, double sigma, double k) {
    if(val < 0.0 || val > 1.0) {
      return Double.NaN;
    }
    if(k < 0) {
      return mu + sigma * Math.max((1. - FastMath.pow(-FastMath.log(val), k)) / k, 1. / k);
    }
    else if(k > 0) {
      return mu + sigma * Math.min((1. - FastMath.pow(-FastMath.log(val), k)) / k, 1. / k);
    }
    else { // Gumbel
      return mu + sigma * FastMath.log(1. / FastMath.log(1. / val));
    }
  }

  @Override
  public String toString() {
    return "GeneralizedExtremeValueDistribution(sigma=" + sigma + ", mu=" + mu + ", k=" + k + ")";
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double mu, sigma, k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter muP = new DoubleParameter(LOCATION_ID);
      if(config.grab(muP)) {
        mu = muP.doubleValue();
      }

      DoubleParameter sigmaP = new DoubleParameter(SCALE_ID);
      if(config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }

      DoubleParameter kP = new DoubleParameter(SHAPE_ID);
      if(config.grab(kP)) {
        k = kP.doubleValue();
      }
    }

    @Override
    protected GeneralizedExtremeValueDistribution makeInstance() {
      return new GeneralizedExtremeValueDistribution(mu, sigma, k, rnd);
    }
  }
}
