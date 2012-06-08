package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import java.util.Random;


/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * Beta Distribution with implementation of the regularized incomplete beta
 * function
 * 
 * @author Jan Brusis
 * @author Erich Schubert
 */
public class BetaDistribution implements DistributionWithRandom {
  /**
   * Numerical precision to use
   */
  static final double NUM_PRECISION = 1E-15;

  /**
   * Limit of when to switch to quadrature method
   */
  static final double SWITCH = 3000;

  /**
   * Abscissas for Gauss-Legendre quadrature
   */
  static final double[] GAUSSLEGENDRE_Y = { 0.0021695375159141994, 0.011413521097787704, 0.027972308950302116, 0.051727015600492421, 0.082502225484340941, 0.12007019910960293, 0.16415283300752470, 0.21442376986779355, 0.27051082840644336, 0.33199876341447887, 0.39843234186401943, 0.46931971407375483, 0.54413605556657973, 0.62232745288031077, 0.70331500465597174, 0.78649910768313447, 0.87126389619061517, 0.95698180152629142 };

  /**
   * Weights for Gauss-Legendre quadrature
   */
  static final double[] GAUSSLEGENDRE_W = { 0.0055657196642445571, 0.012915947284065419, 0.020181515297735382, 0.027298621498568734, 0.034213810770299537, 0.040875750923643261, 0.047235083490265582, 0.053244713977759692, 0.058860144245324798, 0.064039797355015485, 0.068745323835736408, 0.072941885005653087, 0.076598410645870640, 0.079687828912071670, 0.082187266704339706, 0.084078218979661945, 0.085346685739338721, 0.085983275670394821 };

  /**
   * Shape parameter of beta distribution
   */
  private final double alpha;

  /**
   * Shape parameter of beta distribution
   */
  private final double beta;

  /**
   * For random number generation
   */
  private Random random;

  /**
   * Log beta(a, b) cache
   */
  private double logbab;

  /**
   * Constructor.
   * 
   * @param a shape Parameter a
   * @param b shape Parameter b
   */
  public BetaDistribution(double a, double b) {
    this(a, b, new Random());
  }

  /**
   * Constructor.
   * 
   * @param a shape Parameter a
   * @param b shape Parameter b
   * @param random Random generator
   */
  public BetaDistribution(double a, double b, Random random) {
    super();
    if(a <= 0.0 || b <= 0.0) {
      throw new IllegalArgumentException("Invalid parameters for Beta distribution.");
    }

    this.alpha = a;
    this.beta = b;
    this.logbab = logBeta(a, b);
    this.random = random;
  }

  @Override
  public double pdf(double val) {
    if(val < 0. || val > 1.) {
      return 0.;
    }
    if (val == 0.) {
      if (alpha > 1.) {
        return 0.;
      }
      if (alpha < 1.) {
        return Double.POSITIVE_INFINITY;
      }
      return beta;
    }
    if (val == 1.) {
      if (beta > 1.) {
        return 0.;
      }
      if (beta < 1.) {
        return Double.POSITIVE_INFINITY;
      }
      return alpha;
    }
    return Math.exp(-logbab + Math.log(val) * (alpha - 1) + Math.log(1 - val) * (beta - 1));
  }

  @Override
  public double cdf(double x) {
    if(alpha <= 0.0 || beta <= 0.0 || Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(x)) {
      return Double.NaN;
    }
    if(x <= 0.0) {
      return 0.0;
    }
    if(x >= 1.0) {
      return 1.0;
    }
    if(alpha > SWITCH && beta > SWITCH) {
      return regularizedIncBetaQuadrature(alpha, beta, x);
    }
    double bt = Math.exp(-logbab + alpha * Math.log(x) + beta * Math.log(1.0 - x));
    if(x < (alpha + 1.0) / (alpha + beta + 2.0)) {
      return bt * regularizedIncBetaCF(alpha, beta, x) / alpha;
    }
    else {
      return 1.0 - bt * regularizedIncBetaCF(beta, alpha, 1.0 - x) / beta;
    }
  }

  @Override
  public double nextRandom() {
    double x = GammaDistribution.nextRandom(alpha, 1, random);
    double y = GammaDistribution.nextRandom(beta, 1, random);
    return x / (x + y);
  }

  /**
   * Static version of the CDF of the beta distribution
   * 
   * @param val Value
   * @param alpha Shape parameter a
   * @param beta Shape parameter b
   * @return cumulative density
   */
  public static double cdf(double val, double alpha, double beta) {
    return regularizedIncBeta(val, alpha, beta);
  }

  /**
   * Static version of the PDF of the beta distribution
   * 
   * @param val Value
   * @param alpha Shape parameter a
   * @param beta Shape parameter b
   * @return probability density
   */
  public static double pdf(double val, double alpha, double beta) {
    if(alpha <= 0. || beta <= 0. || Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(val)) {
      return Double.NaN;
    }
    if(val < 0. || val > 1.) {
      return 0.;
    }
    if (val == 0.) {
      if (alpha > 1.) {
        return 0.;
      }
      if (alpha < 1.) {
        return Double.POSITIVE_INFINITY;
      }
      return beta;
    }
    if (val == 1.) {
      if (beta > 1.) {
        return 0.;
      }
      if (beta < 1.) {
        return Double.POSITIVE_INFINITY;
      }
      return alpha;
    }
    return Math.exp(-logBeta(alpha, beta) + Math.log(val) * (alpha - 1) + Math.log(1 - val) * (beta - 1));
  }

  /**
   * Compute log beta(a,b)
   * 
   * @param alpha Shape parameter a
   * @param beta Shape parameter b
   * @return Logarithm of result
   */
  public static double logBeta(double alpha, double beta) {
    return GammaDistribution.logGamma(alpha) + GammaDistribution.logGamma(beta) - GammaDistribution.logGamma(alpha + beta);
  }

  /**
   * Computes the regularized incomplete beta function I_x(a, b) which is also
   * the CDF of the beta distribution. Based on the book "Numerical Recipes"
   * 
   * @param alpha Parameter a
   * @param beta Parameter b
   * @param x Parameter x
   * @return Value of the regularized incomplete beta function
   */
  public static double regularizedIncBeta(double x, double alpha, double beta) {
    if(alpha <= 0.0 || beta <= 0.0 || Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(x)) {
      return Double.NaN;
    }
    if(x <= 0.0) {
      return 0.0;
    }
    if(x >= 1.0) {
      return 1.0;
    }
    if(alpha > SWITCH && beta > SWITCH) {
      return regularizedIncBetaQuadrature(alpha, beta, x);
    }
    double bt = Math.exp(-logBeta(alpha, beta) + alpha * Math.log(x) + beta * Math.log(1.0 - x));
    if(x < (alpha + 1.0) / (alpha + beta + 2.0)) {
      return bt * regularizedIncBetaCF(alpha, beta, x) / alpha;
    }
    else {
      return 1.0 - bt * regularizedIncBetaCF(beta, alpha, 1.0 - x) / beta;
    }
  }

  /**
   * Returns the regularized incomplete beta function I_x(a, b) Includes the
   * continued fraction way of computing, based on the book "Numerical Recipes".
   * 
   * @param alpha Parameter a
   * @param beta Parameter b
   * @param x Parameter x
   * @return result
   */
  protected static double regularizedIncBetaCF(double alpha, double beta, double x) {
    final double FPMIN = Double.MIN_VALUE / NUM_PRECISION;
    double qab = alpha + beta;
    double qap = alpha + 1.0;
    double qam = alpha - 1.0;
    double c = 1.0;
    double d = 1.0 - qab * x / qap;
    if(Math.abs(d) < FPMIN) {
      d = FPMIN;
    }
    d = 1.0 / d;
    double h = d;
    for(int m = 1; m < 10000; m++) {
      int m2 = 2 * m;
      double aa = m * (beta - m) * x / ((qam + m2) * (alpha + m2));
      d = 1.0 + aa * d;
      if(Math.abs(d) < FPMIN) {
        d = FPMIN;
      }
      c = 1.0 + aa / c;
      if(Math.abs(c) < FPMIN) {
        c = FPMIN;
      }
      d = 1.0 / d;
      h *= d * c;
      aa = -(alpha + m) * (qab + m) * x / ((alpha + m2) * (qap + m2));
      d = 1.0 + aa * d;
      if(Math.abs(d) < FPMIN) {
        d = FPMIN;
      }
      c = 1.0 + aa / c;
      if(Math.abs(c) < FPMIN) {
        c = FPMIN;
      }
      d = 1.0 / d;
      double del = d * c;
      h *= del;
      if(Math.abs(del - 1.0) <= NUM_PRECISION) {
        break;
      }
    }
    return h;
  }

  /**
   * Returns the regularized incomplete beta function I_x(a, b) by quadrature,
   * based on the book "Numerical Recipes".
   * 
   * @param alpha Parameter a
   * @param beta Parameter b
   * @param x Parameter x
   * @return result
   */
  protected static double regularizedIncBetaQuadrature(double alpha, double beta, double x) {
    double a1 = alpha - 1.0;
    double b1 = beta - 1.0;
    double mu = alpha / (alpha + beta);
    double lnmu = Math.log(mu);
    double lnmuc = Math.log(1.0 - mu);
    double t = Math.sqrt(alpha * beta / ((alpha + beta) * (alpha + beta) * (alpha + beta + 1.0)));
    double xu;
    if(x > alpha / (alpha + beta)) {
      if(x >= 1.0) {
        return 1.0;
      }
      xu = Math.min(1.0, Math.max(mu + 10.0 * t, x + 5.0 * t));
    }
    else {
      if(x <= 0.0) {
        return 0.0;
      }
      xu = Math.max(0.0, Math.min(mu - 10.0 * t, x - 5.0 * t));
    }
    double sum = 0.0;
    for(int i = 0; i < GAUSSLEGENDRE_Y.length; i++) {
      t = x + (xu - x) * GAUSSLEGENDRE_Y[i];
      sum += GAUSSLEGENDRE_W[i] * Math.exp(a1 * (Math.log(t) - lnmu) + b1 * (Math.log(1 - t) - lnmuc));
    }
    double ans = sum * (xu - x) * Math.exp(a1 * lnmu - GammaDistribution.logGamma(alpha) + b1 * lnmuc - GammaDistribution.logGamma(b1) + GammaDistribution.logGamma(alpha + beta));
    return ans > 0 ? 1.0 - ans : -ans;
  }
}