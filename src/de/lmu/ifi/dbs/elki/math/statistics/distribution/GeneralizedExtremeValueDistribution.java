package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import java.util.Random;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

/**
 * Generalized Extreme Value (GEV) distribution, also known as Fisher–Tippett
 * distribution.
 * 
 * This is a generalization of the Frechnet, Gumbel and (reversed) Weibull
 * distributions.
 * 
 * Implementation notice: apparently (see unit tests), our definition differs
 * from the scipy definition by having the negative shape.
 * 
 * @author Erich Schubert
 */
public class GeneralizedExtremeValueDistribution implements Distribution {
  /**
   * Parameters (location, scale, shape)
   */
  final double mu, sigma, k;

  /**
   * Random number generator.
   */
  Random random;

  /**
   * Constructor.
   * 
   * @param mu Location parameter mu
   * @param sigma Scale parameter sigma
   * @param k Shape parameter k
   */
  public GeneralizedExtremeValueDistribution(double mu, double sigma, double k) {
    this(mu, sigma, k, null);
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
    super();
    this.mu = mu;
    this.sigma = sigma;
    this.k = k;
    this.random = random;
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
    x = (x - mu) / sigma;
    if (k > 0 || k < 0) {
      if (k * x < -1) {
        return 0.;
      }
      final double tx = Math.pow(1 + k * x, -1. / k);
      return Math.pow(tx, k + 1) * Math.exp(-tx) / sigma;
    } else { // Gumbel case:
      return Math.exp(-x - Math.exp(-x)) / sigma;
    }
  }

  @Override
  public double pdf(double x) {
    return pdf(x, mu, sigma, k);
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
    if (k > 0 || k < 0) {
      if (k * x <= -1) {
        return (k > 0) ? 0 : 1;
      }
      return Math.exp(-Math.pow(1 + k * x, -1. / k));
    } else { // Gumbel case:
      return Math.exp(-Math.exp(-x));
    }
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mu, sigma, k);
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
    if (val < 0.0 || val > 1.0) {
      return Double.NaN;
    }
    if (k > 0) {
      return mu + sigma * Math.max((Math.pow(-Math.log(val), -k) - 1.) / k, -1. / k);
    } else if (k < 0) {
      return mu + sigma * Math.min((Math.pow(-Math.log(val), -k) - 1.) / k, -1. / k);
    } else { // Gumbel
      return mu + sigma * Math.log(1. / Math.log(1. / val));
    }
  }

  @Override
  public double quantile(double val) {
    return quantile(val, mu, sigma, k);
  }

  @Override
  public double nextRandom() {
    return quantile(random.nextDouble());
  }

  @Override
  public String toString() {
    return "GeneralizedExtremeValueDistribution(sigma=" + sigma + ", mu=" + mu + ", k=" + k + ")";
  }
}
