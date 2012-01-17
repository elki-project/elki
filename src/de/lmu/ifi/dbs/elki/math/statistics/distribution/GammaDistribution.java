package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Gamma Distribution, with random generation and density functions.
 * 
 * @author Erich Schubert
 */
public class GammaDistribution implements Distribution {
  /**
   * LANCZOS-Coefficients for Gamma approximation.
   * 
   * These are said to have higher precision than those in "Numerical Recipes".
   * They probably come from
   * 
   * Paul Godfrey: http://my.fit.edu/~gabdo/gamma.txt
   */
  static final double[] LANCZOS = { 0.99999999999999709182, 57.156235665862923517, -59.597960355475491248, 14.136097974741747174, -0.49191381609762019978, .33994649984811888699e-4, .46523628927048575665e-4, -.98374475304879564677e-4, .15808870322491248884e-3, -.21026444172410488319e-3, .21743961811521264320e-3, -.16431810653676389022e-3, .84418223983852743293e-4, -.26190838401581408670e-4, .36899182659531622704e-5, };

  /**
   * Numerical precision to use
   */
  static final double NUM_PRECISION = 1E-15;

  /**
   * Alpha == k
   */
  private final double k;

  /**
   * Theta == 1 / Beta
   */
  private final double theta;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param random Random generator
   */
  public GammaDistribution(double k, double theta, Random random) {
    super();
    if(k <= 0.0 || theta <= 0.0) {
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution.");
    }

    this.k = k;
    this.theta = theta;
    this.random = random;
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   */
  public GammaDistribution(double k, double theta) {
    this(k, theta, new Random());
  }

  @Override
  public double pdf(double val) {
    return pdf(val, k, theta);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, k, theta);
  }

  /**
   * Generate a random value with the generators parameters.
   * 
   * Along the lines of
   * 
   * - J.H. Ahrens, U. Dieter (1974): Computer methods for sampling from gamma,
   * beta, Poisson and binomial distributions, Computing 12, 223-246.
   * 
   * - J.H. Ahrens, U. Dieter (1982): Generating gamma variates by a modified
   * rejection technique, Communications of the ACM 25, 47-54.
   */
  @Override
  public double nextRandom() {
    /* Constants */
    final double q1 = 0.0416666664, q2 = 0.0208333723, q3 = 0.0079849875;
    final double q4 = 0.0015746717, q5 = -0.0003349403, q6 = 0.0003340332;
    final double q7 = 0.0006053049, q8 = -0.0004701849, q9 = 0.0001710320;
    final double a1 = 0.333333333, a2 = -0.249999949, a3 = 0.199999867;
    final double a4 = -0.166677482, a5 = 0.142873973, a6 = -0.124385581;
    final double a7 = 0.110368310, a8 = -0.112750886, a9 = 0.104089866;
    final double e1 = 1.000000000, e2 = 0.499999994, e3 = 0.166666848;
    final double e4 = 0.041664508, e5 = 0.008345522, e6 = 0.001353826;
    final double e7 = 0.000247453;

    if(k < 1.0) { // Base case, for small k
      final double b = 1.0 + 0.36788794412 * k; // Step 1
      while(true) {
        final double p = b * random.nextDouble();
        if(p <= 1.0) { // when gds <= 1
          final double gds = Math.exp(Math.log(p) / k);
          if(Math.log(random.nextDouble()) <= -gds) {
            return (gds / theta);
          }
        }
        else { // when gds > 1
          final double gds = -Math.log((b - p) / k);
          if(Math.log(random.nextDouble()) <= ((k - 1.0) * Math.log(gds))) {
            return (gds / theta);
          }
        }
      }
    }
    else {
      // Step 1. Preparations
      final double ss, s, d;
      if(k != -1.0) {
        ss = k - 0.5;
        s = Math.sqrt(ss);
        d = 5.656854249 - 12.0 * s;
      }
      else {
        // For k == -1.0:
        ss = 0.0;
        s = 0.0;
        d = 0.0;
      }
      // Random vector of maximum length 1
      final double v1, /* v2, */v12;
      { // Temporary values - candidate
        double tv1, tv2, tv12;
        do {
          tv1 = 2.0 * random.nextDouble() - 1.0;
          tv2 = 2.0 * random.nextDouble() - 1.0;
          tv12 = tv1 * tv1 + tv2 * tv2;
        }
        while(tv12 > 1.0);
        v1 = tv1;
        /* v2 = tv2; */
        v12 = tv12;
      }

      // double b = 0.0, c = 0.0;
      // double si = 0.0, q0 = 0.0;
      final double b, c, si, q0;

      // Simpler accept cases & parameter computation
      {
        final double t = v1 * Math.sqrt(-2.0 * Math.log(v12) / v12);
        final double x = s + 0.5 * t;
        final double gds = x * x;
        if(t >= 0.0) {
          return (gds / theta); // Immediate acceptance
        }

        // Random uniform
        final double un = random.nextDouble();
        // Squeeze acceptance
        if(d * un <= t * t * t) {
          return (gds / theta);
        }

        if(k != -1.0) { // Step 4. Set-up for hat case
          final double r = 1.0 / k;
          q0 = ((((((((q9 * r + q8) * r + q7) * r + q6) * r + q5) * r + q4) * r + q3) * r + q2) * r + q1) * r;
          if(k > 3.686) {
            if(k > 13.022) {
              b = 1.77;
              si = 0.75;
              c = 0.1515 / s;
            }
            else {
              b = 1.654 + 0.0076 * ss;
              si = 1.68 / s + 0.275;
              c = 0.062 / s + 0.024;
            }
          }
          else {
            b = 0.463 + s - 0.178 * ss;
            si = 1.235;
            c = 0.195 / s - 0.079 + 0.016 * s;
          }
        }
        else {
          // For k == -1.0:
          b = 0.0;
          c = 0.0;
          si = 0.0;
          q0 = 0.0;
        }
        // Compute v and q
        if(x > 0.0) {
          final double v = t / (s + s);
          final double q;
          if(Math.abs(v) > 0.25) {
            q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
          }
          else {
            q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
          }
          // Quotient acceptance:
          if(Math.log(1.0 - un) <= q) {
            return (gds / theta);
          }
        }
      }

      // Double exponential deviate t
      while(true) {
        double e, u, sign_u, t;
        // Retry until t is sufficiently large
        do {
          e = -Math.log(random.nextDouble());
          u = random.nextDouble();
          u = u + u - 1.0;
          sign_u = (u > 0) ? 1.0 : -1.0;
          t = b + (e * si) * sign_u;
        }
        while(t <= -0.71874483771719);

        // New v(t) and q(t)
        final double v = t / (s + s);
        final double q;
        if(Math.abs(v) > 0.25) {
          q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
        }
        else {
          q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
        }
        if(q <= 0.0) {
          continue; // retry
        }
        // Compute w(t)
        final double w;
        if(q > 0.5) {
          w = Math.exp(q) - 1.0;
        }
        else {
          w = ((((((e7 * q + e6) * q + e5) * q + e4) * q + e3) * q + e2) * q + e1) * q;
        }
        // Hat acceptance
        if(c * u * sign_u <= w * Math.exp(e - 0.5 * t * t)) {
          final double x = s + 0.5 * t;
          return (x * x / theta);
        }
      }
    }
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "Gamma Distribution (k=" + k + ", theta=" + theta + ")";
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
   * @param val Value
   * @param k Shape k
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return cdf value
   */
  public static double cdf(double val, double k, double theta) {
    return regularizedGammaP(k, val / theta);
  }

  /**
   * Gamma distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return probability density
   */
  public static double pdf(double x, double k, double theta) {
    if(x < 0) {
      return 0.0;
    }
    if(x == 0) {
      if(k == 1.0) {
        return theta;
      }
      else {
        return 0.0;
      }
    }
    if(k == 1.0) {
      return Math.exp(-x * theta) * theta;
    }

    return Math.exp((k - 1.0) * Math.log(x * theta) - x * theta - logGamma(k)) * theta;
  }

  /**
   * Compute logGamma.
   * 
   * Based loosely on "Numerical Recpies" and the work of Paul Godfrey at
   * http://my.fit.edu/~gabdo/gamma.txt
   * 
   * TODO: find out which approximation really is the best...
   * 
   * @param x Parameter x
   * @return @return log(&#915;(x))
   */
  public static double logGamma(final double x) {
    if(Double.isNaN(x) || (x <= 0.0)) {
      return Double.NaN;
    }
    double g = 607.0 / 128.0;
    double tmp = x + g + .5;
    tmp = (x + 0.5) * Math.log(tmp) - tmp;
    double ser = LANCZOS[0];
    for(int i = LANCZOS.length - 1; i > 0; --i) {
      ser += LANCZOS[i] / (x + i);
    }
    return tmp + Math.log(MathUtil.SQRTTWOPI * ser / x);
  }

  /**
   * Returns the regularized gamma function P(a, x).
   * 
   * Includes the quadrature way of computing.
   * 
   * TODO: find "the" most accurate version of this. We seem to agree with
   * others for the first 10+ digits, but diverge a bit later than that.
   * 
   * @param a Parameter a
   * @param x Parameter x
   * @return Gamma value
   */
  public static double regularizedGammaP(final double a, final double x) {
    // Special cases
    if(Double.isNaN(a) || Double.isNaN(x) || (a <= 0.0) || (x < 0.0)) {
      return Double.NaN;
    }
    if(x == 0.0) {
      return 0.0;
    }
    if(x >= a + 1) {
      // Expected to converge faster
      return 1.0 - regularizedGammaQ(a, x);
    }
    // Loosely following "Numerical Recipes"
    double del = 1.0 / a;
    double sum = del;
    for(int n = 1; n < Integer.MAX_VALUE; n++) {
      // compute next element in the series
      del *= x / (a + n);
      sum = sum + del;
      if(Math.abs(del / sum) < NUM_PRECISION || sum >= Double.POSITIVE_INFINITY) {
        break;
      }
    }
    if(Double.isInfinite(sum)) {
      return 1.0;
    }
    return Math.exp(-x + (a * Math.log(x)) - logGamma(a)) * sum;
  }

  /**
   * Returns the regularized gamma function Q(a, x) = 1 - P(a, x).
   * 
   * Includes the continued fraction way of computing, based loosely on the book
   * "Numerical Recipes"; but probably not with the exactly same precision,
   * since we reimplemented this in our coding style, not literally.
   * 
   * TODO: find "the" most accurate version of this. We seem to agree with
   * others for the first 10+ digits, but diverge a bit later than that.
   * 
   * @param a parameter a
   * @param x parameter x
   * @return Result
   */
  public static double regularizedGammaQ(final double a, final double x) {
    if(Double.isNaN(a) || Double.isNaN(x) || (a <= 0.0) || (x < 0.0)) {
      return Double.NaN;
    }
    if(x == 0.0) {
      return 1.0;
    }
    if(x < a + 1.0) {
      // Expected to converge faster
      return 1.0 - regularizedGammaP(a, x);
    }
    // Compute using continued fraction approach.
    final double FPMIN = Double.MIN_VALUE / NUM_PRECISION;
    double b = x + 1 - a;
    double c = 1.0 / FPMIN;
    double d = 1.0 / b;
    double fac = d;
    for(int i = 1; i < Integer.MAX_VALUE; i++) {
      double an = i * (a - i);
      b += 2;
      d = an * d + b;
      if(Math.abs(d) < FPMIN) {
        d = FPMIN;
      }
      c = b + an / c;
      if(Math.abs(c) < FPMIN) {
        c = FPMIN;
      }
      d = 1 / d;
      double del = d * c;
      fac *= del;
      if(Math.abs(del - 1.0) <= NUM_PRECISION) {
        break;
      }
    }
    return fac * Math.exp(-x + a * Math.log(x) - logGamma(a));
  }
}