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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Gamma Distribution, with random generation and density functions.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Alias("de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.GammaDistribution")
public class GammaDistribution extends AbstractDistribution {
  /**
   * Euler–Mascheroni constant
   */
  public static final double EULERS_CONST = 0.5772156649015328606065120900824024;

  /**
   * LANCZOS-Coefficients for Gamma approximation.
   * <p>
   * These are said to have higher precision than those in "Numerical Recipes".
   * They probably come from
   * <p>
   * Paul Godfrey: http://my.fit.edu/~gabdo/gamma.txt
   */
  static final double[] LANCZOS = { 0.99999999999999709182, 57.156235665862923517, -59.597960355475491248, 14.136097974741747174, -0.49191381609762019978, .33994649984811888699e-4, .46523628927048575665e-4, -.98374475304879564677e-4, .15808870322491248884e-3, -.21026444172410488319e-3, .21743961811521264320e-3, -.16431810653676389022e-3, .84418223983852743293e-4, -.26190838401581408670e-4, .36899182659531622704e-5, };

  /**
   * Numerical precision to use (data type dependent!)
   * 
   * If you change this, make sure to test exhaustively!
   */
  static final double NUM_PRECISION = 1E-15;

  /**
   * Maximum number of iterations for regularizedGammaP. To prevent degeneration
   * for extreme values.
   * 
   * FIXME: is this too high, too low? Can we improve behavior for extreme
   * cases?
   */
  static final int MAX_ITERATIONS = 1000;

  /**
   * Alpha == k
   */
  private final double k;

  /**
   * Theta == 1 / Beta
   */
  private final double theta;

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param random Random generator
   */
  public GammaDistribution(double k, double theta, Random random) {
    super(random);
    if(!(k > 0.0) || !(theta > 0.0)) { // Note: also tests for NaNs!
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution: " + k + " " + theta);
    }

    this.k = k;
    this.theta = theta;
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @param random Random generator
   */
  public GammaDistribution(double k, double theta, RandomFactory random) {
    super(random);
    if(!(k > 0.0) || !(theta > 0.0)) { // Note: also tests for NaNs!
      throw new IllegalArgumentException("Invalid parameters for Gamma distribution: " + k + " " + theta);
    }

    this.k = k;
    this.theta = theta;
  }

  /**
   * Constructor for Gamma distribution.
   * 
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   */
  public GammaDistribution(double k, double theta) {
    this(k, theta, (Random) null);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, k, theta);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, k, theta);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, k, theta);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, k, theta);
  }

  @Override
  public double nextRandom() {
    return nextRandom(k, theta, random);
  }

  /**
   * Simple toString explaining the distribution parameters.
   * 
   * Used in producing a model description.
   */
  @Override
  public String toString() {
    return "GammaDistribution(k=" + k + ", theta=" + theta + ")";
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
    if(val < 0) {
      return 0.;
    }
    final double vt = val * theta;
    return (vt == Double.POSITIVE_INFINITY) ? 1. : regularizedGammaP(k, vt);
  }

  /**
   * The log CDF, static version.
   * 
   * @param val Value
   * @param k Shape k
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return cdf value
   */
  public static double logcdf(double val, double k, double theta) {
    if(val < 0) {
      return Double.NEGATIVE_INFINITY;
    }
    double vt = val * theta;
    return (val == Double.POSITIVE_INFINITY) ? 0. : logregularizedGammaP(k, vt);
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
      return 0.;
    }
    if(x == 0) {
      return (k == 1.) ? theta : 0;
    }
    if(k == 1.) {
      return FastMath.exp(-x * theta) * theta;
    }
    final double xt = x * theta;
    return (xt == Double.POSITIVE_INFINITY) ? 0. : //
        FastMath.exp((k - 1.0) * FastMath.log(xt) - xt - logGamma(k)) * theta;
  }

  /**
   * Gamma distribution PDF (with 0.0 for x &lt; 0)
   * 
   * @param x query value
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @return probability density
   */
  public static double logpdf(double x, double k, double theta) {
    if(x < 0) {
      return Double.NEGATIVE_INFINITY;
    }
    if(x == 0) {
      return (k == 1.0) ? FastMath.log(theta) : Double.NEGATIVE_INFINITY;
    }
    if(k == 1.0) {
      return FastMath.log(theta) - x * theta;
    }
    final double xt = x * theta;
    return (xt == Double.POSITIVE_INFINITY) ? Double.NEGATIVE_INFINITY : //
        FastMath.log(theta) + (k - 1.0) * FastMath.log(xt) - xt - logGamma(k);
  }

  /**
   * Compute logGamma.
   * <p>
   * Based loosely on "Numerical Recpies" and the work of Paul Godfrey at
   * http://my.fit.edu/~gabdo/gamma.txt
   * <p>
   * TODO: find out which approximation really is the best...
   *
   * @param x Parameter x
   * @return log(&#915;(x))
   */
  public static double logGamma(final double x) {
    if(Double.isNaN(x) || (x <= 0.0)) {
      return Double.NaN;
    }
    double g = 607.0 / 128.0;
    double tmp = x + g + .5;
    tmp = (x + 0.5) * FastMath.log(tmp) - tmp;
    double ser = LANCZOS[0];
    for(int i = LANCZOS.length - 1; i > 0; --i) {
      ser += LANCZOS[i] / (x + i);
    }
    return tmp + FastMath.log(MathUtil.SQRTTWOPI * ser / x);
  }

  /**
   * Compute the regular Gamma function.
   * <p>
   * Note: for numerical reasons, it is preferable to use {@link #logGamma} when
   * possible! In particular, this method just computes
   * {@code FastMath.exp(logGamma(x))} anyway.
   * <p>
   * Try to postpone the {@code FastMath.exp} call to preserve numeric range!
   *
   * @param x Position
   * @return Gamma at this position
   */
  public static double gamma(double x) {
    return FastMath.exp(logGamma(x));
  }

  /**
   * Returns the regularized gamma function P(a, x).
   * <p>
   * Includes the quadrature way of computing.
   * <p>
   * TODO: find "the" most accurate version of this. We seem to agree with
   * others for the first 10+ digits, but diverge a bit later than that.
   *
   * @param a Parameter a
   * @param x Parameter x
   * @return Gamma value
   */
  public static double regularizedGammaP(final double a, final double x) {
    // Special cases
    if(Double.isInfinite(a) || Double.isInfinite(x) || !(a > 0.0) || !(x >= 0.0)) {
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
    double term = 1.0 / a;
    double sum = term;
    for(int n = 1; n < MAX_ITERATIONS; n++) {
      // compute next element in the series
      term = x / (a + n) * term;
      sum = sum + term;
      if(sum == Double.POSITIVE_INFINITY) {
        return 1.0;
      }
      if(Math.abs(term / sum) < NUM_PRECISION) {
        break;
      }
    }
    return FastMath.exp(-x + (a * FastMath.log(x)) - logGamma(a)) * sum;
  }

  /**
   * Returns the regularized gamma function log P(a, x).
   * <p>
   * Includes the quadrature way of computing.
   * <p>
   * TODO: find "the" most accurate version of this. We seem to agree with
   * others for the first 10+ digits, but diverge a bit later than that.
   *
   * @param a Parameter a
   * @param x Parameter x
   * @return Gamma value
   */
  public static double logregularizedGammaP(final double a, final double x) {
    // Special cases
    if(Double.isNaN(a) || Double.isNaN(x) || (a <= 0.0) || (x < 0.0)) {
      return Double.NaN;
    }
    if(x == 0.0) {
      return Double.NEGATIVE_INFINITY;
    }
    if(x >= a + 1) {
      // Expected to converge faster
      // FIXME: and in log?
      return FastMath.log(1.0 - regularizedGammaQ(a, x));
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
      return 0;
    }
    // TODO: reread numerical recipes, can we replace log(sum)?
    return -x + (a * FastMath.log(x)) - logGamma(a) + FastMath.log(sum);
  }

  /**
   * Returns the regularized gamma function Q(a, x) = 1 - P(a, x).
   * <p>
   * Includes the continued fraction way of computing, based loosely on the book
   * "Numerical Recipes"; but probably not with the exactly same precision,
   * since we reimplemented this in our coding style, not literally.
   * <p>
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
    for(int i = 1; i < MAX_ITERATIONS; i++) {
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
    return fac * FastMath.exp(-x + a * FastMath.log(x) - logGamma(a));
  }

  /**
   * Generate a random value with the generators parameters.
   * <p>
   * Along the lines of
   * <p>
   * J. H. Ahrens, U. Dieter<br>
   * Computer methods for sampling from gamma, beta, Poisson and binomial
   * distributions<br>
   * Computing 12
   * <p>
   * J. H. Ahrens, U. Dieter<br>
   * Generating gamma variates by a modified rejection technique<br>
   * Communications of the ACM 25
   *
   * @param k K parameter
   * @param theta Theta parameter
   * @param random Random generator
   */
  @Reference(authors = "J. H. Ahrens, U. Dieter", //
      title = "Computer methods for sampling from gamma, beta, Poisson and binomial distributions", //
      booktitle = "Computing 12", //
      url = "https://doi.org/10.1007/BF02293108", //
      bibkey = "DBLP:journals/computing/AhrensD74")
  @Reference(authors = "J. H. Ahrens, U. Dieter", //
      title = "Generating gamma variates by a modified rejection technique", //
      booktitle = "Communications of the ACM 25", //
      url = "https://doi.org/10.1145/358315.358390", //
      bibkey = "DBLP:journals/cacm/AhrensD82")
  public static double nextRandom(double k, double theta, Random random) {
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
          final double gds = FastMath.exp(FastMath.log(p) / k);
          if(FastMath.log(random.nextDouble()) <= -gds) {
            return (gds / theta);
          }
        }
        else { // when gds > 1
          final double gds = -FastMath.log((b - p) / k);
          if(FastMath.log(random.nextDouble()) <= ((k - 1.0) * FastMath.log(gds))) {
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
        s = FastMath.sqrt(ss);
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
        final double t = v1 * FastMath.sqrt(-2.0 * FastMath.log(v12) / v12);
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
            q = q0 - s * t + 0.25 * t * t + (ss + ss) * FastMath.log(1.0 + v);
          }
          else {
            q = q0 + 0.5 * t * t * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
          }
          // Quotient acceptance:
          if(FastMath.log(1.0 - un) <= q) {
            return (gds / theta);
          }
        }
      }

      // Double exponential deviate t
      while(true) {
        double e, u, sign_u, t;
        // Retry until t is sufficiently large
        do {
          e = -FastMath.log(random.nextDouble());
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
          q = q0 - s * t + 0.25 * t * t + (ss + ss) * FastMath.log(1.0 + v);
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
          w = FastMath.exp(q) - 1.0;
        }
        else {
          w = ((((((e7 * q + e6) * q + e5) * q + e4) * q + e3) * q + e2) * q + e1) * q;
        }
        // Hat acceptance
        if(c * u * sign_u <= w * FastMath.exp(e - 0.5 * t * t)) {
          final double x = s + 0.5 * t;
          return (x * x / theta);
        }
      }
    }
  }

  /**
   * Approximate probit for chi squared distribution
   * <p>
   * Based on first half of algorithm AS 91
   * <p>
   * Reference:
   * <p>
   * D. J. Best, D. E. Roberts<br>
   * Algorithm AS 91: The percentage points of the χ² distribution<br>
   * Journal of the Royal Statistical Society. Series C (Applied Statistics)
   *
   * @param p Probit value
   * @param nu Shape parameter for Chi, nu = 2 * k
   * @param g log(nu)
   * @return Probit for chi squared
   */
  @Reference(authors = "D. J. Best, D. E. Roberts", //
      title = "Algorithm AS 91: The percentage points of the χ² distribution", //
      booktitle = "Journal of the Royal Statistical Society. Series C (Applied Statistics)", //
      url = "https://doi.org/10.2307/2347113", //
      bibkey = "doi:10.2307/2347113")
  protected static double chisquaredProbitApproximation(final double p, double nu, double g) {
    final double EPS1 = 1e-14; // Approximation quality
    // Sanity checks
    if(Double.isNaN(p) || Double.isNaN(nu)) {
      return Double.NaN;
    }
    // Range check
    if(p <= 0) {
      return 0;
    }
    if(p >= 1) {
      return Double.POSITIVE_INFINITY;
    }
    // Invalid parameters
    if(nu <= 0) {
      return Double.NaN;
    }
    // Shape of gamma distribution, "XX" in AS 91
    final double k = 0.5 * nu;

    // For small chi squared values - AS 91
    final double logp = FastMath.log(p);
    if(nu < -1.24 * logp) {
      // FIXME: implement and use logGammap1 instead - more stable?
      //
      // final double lgam1pa = (alpha < 0.5) ? logGammap1(alpha) :
      // (FastMath.log(alpha) + g);
      // return FastMath.exp((lgam1pa + logp) / alpha + MathUtil.LOG2);
      // This is literal AS 91, above is the GNU R variant.
      return FastMath.pow(p * k * FastMath.exp(g + k * MathUtil.LOG2), 1. / k);
    }
    else if(nu > 0.32) {
      // Wilson and Hilferty estimate: - AS 91 at 3
      final double x = NormalDistribution.quantile(p, 0, 1);
      final double p1 = 2. / (9. * nu);
      final double a = x * FastMath.sqrt(p1) + 1 - p1;
      double ch = nu * a * a * a;

      // Better approximation for p tending to 1:
      if(ch > 2.2 * nu + 6) {
        ch = -2 * (FastMath.log1p(-p) - (k - 1) * FastMath.log(0.5 * ch) + g);
      }
      return ch;
    }
    else {
      // nu <= 0.32, AS 91 at 1
      final double C7 = 4.67, C8 = 6.66, C9 = 6.73, C10 = 13.32;
      final double ag = FastMath.log1p(-p) + g + (k - 1) * MathUtil.LOG2;
      double ch = 0.4;
      while(true) {
        final double p1 = 1 + ch * (C7 + ch);
        final double p2 = ch * (C9 + ch * (C8 + ch));
        final double t = -0.5 + (C7 + 2 * ch) / p1 - (C9 + ch * (C10 + 3 * ch)) / p2;
        final double delta = (1 - FastMath.exp(ag + 0.5 * ch) * p2 / p1) / t;
        ch -= delta;
        if(Math.abs(delta) <= EPS1 * Math.abs(ch)) {
          return ch;
        }
      }
    }
  }

  /**
   * Compute probit (inverse cdf) for Gamma distributions.
   * <p>
   * Based on algorithm AS 91:
   * <p>
   * Reference:
   * <p>
   * D. J. Best, D. E. Roberts<br>
   * Algorithm AS 91: The percentage points of the χ² distribution<br>
   * Journal of the Royal Statistical Society. Series C (Applied Statistics)
   *
   * @param p Probability
   * @param k k, alpha aka. "shape" parameter
   * @param theta Theta = 1.0/Beta aka. "scaling" parameter
   * @return Probit for Gamma distribution
   */
  @Reference(authors = "D. J. Best, D. E. Roberts", //
      title = "Algorithm AS 91: The percentage points of the χ² distribution", //
      booktitle = "Journal of the Royal Statistical Society. Series C (Applied Statistics)", //
      url = "https://doi.org/10.2307/2347113", //
      bibkey = "doi:10.2307/2347113")
  public static double quantile(double p, double k, double theta) {
    final double EPS2 = 5e-7; // final precision of AS 91
    final int MAXIT = 1000;

    // Avoid degenerates
    if(!(p >= 0) || p > 1 || Double.isNaN(k) || Double.isNaN(theta)) {
      return Double.NaN;
    }
    // Range check
    if(p == 0) {
      return 0;
    }
    if(p == 1) {
      return Double.POSITIVE_INFINITY;
    }
    // Shape parameter check
    if(k < 0 || theta <= 0) {
      return Double.NaN;
    }
    // Corner case - all at 0
    if(k == 0) {
      return 0.;
    }

    int max_newton_iterations = 1;
    // For small values, ensure some refinement iterations
    if(k < 1e-10) {
      max_newton_iterations = 7;
    }

    final double g = logGamma(k); // == logGamma(v/2)

    // Phase I, an initial rough approximation
    // First half of AS 91
    double ch = chisquaredProbitApproximation(p, 2 * k, g);
    // Second half of AS 91 follows:
    // Refine ChiSquared approximation
    chisq: {
      if(Double.isInfinite(ch)) {
        // Cannot refine infinity
        max_newton_iterations = 0;
        break chisq;
      }
      if(ch < EPS2) {
        // Do not iterate, but refine with Newton method
        max_newton_iterations = 20;
        break chisq;
      }
      if(p > 1 - 1e-14 || p < 1e-100) {
        // Not in appropriate value range for AS 91
        max_newton_iterations = 20;
        break chisq;
      }

      // Phase II: Iteration
      final double c = k - 1;
      final double ch0 = ch; // backup initial approximation
      for(int i = 1; i <= MAXIT; i++) {
        final double q = ch; // previous approximation
        final double p1 = 0.5 * ch;
        final double p2 = p - regularizedGammaP(k, p1);
        if(Double.isInfinite(p2) || ch <= 0) {
          ch = ch0;
          max_newton_iterations = 27;
          break chisq;
        }
        { // Taylor series of AS 91: iteration via "goto 4"
          final double t = p2 * FastMath.exp(k * MathUtil.LOG2 + g + p1 - c * FastMath.log(ch));
          final double b = t / ch;
          final double a = 0.5 * t - b * c;
          final double s1 = (210. + a * (140. + a * (105. + a * (84. + a * (70. + 60. * a))))) / 420.;
          final double s2 = (420. + a * (735. + a * (966. + a * (1141. + 1278 * a)))) / 2520.;
          final double s3 = (210. + a * (462. + a * (707. + 932. * a))) / 2520.;
          final double s4 = (252. + a * (672. + 1182. * a) + c * (294. + a * (889. + 1740. * a))) / 5040.;
          final double s5 = (84. + 2264. * a + c * (1175. + 606. * a)) / 2520.;
          final double s6 = (120. + c * (346. + 127. * c)) / 5040.;
          ch += t * (1 + 0.5 * t * s1 - b * c * (s1 - b * (s2 - b * (s3 - b * (s4 - b * (s5 - b * s6))))));
        }
        if(Math.abs(q - ch) < EPS2 * ch) {
          break chisq;
        }
        // Divergence treatment, from GNU R
        if(Math.abs(q - ch) > 0.1 * Math.abs(ch)) {
          ch = ((ch < q) ? 0.9 : 1.1) * q;
        }
      }
      LoggingUtil.warning("No convergence in AS 91 Gamma probit.");
      // no convergence in MAXIT iterations -- but we add Newton now...
    }
    double x = 0.5 * ch / theta;
    if(max_newton_iterations > 0) {
      // Refine result using final Newton steps.
      // TODO: add unit tests that show an improvement! Maybe in logscale only?
      x = gammaQuantileNewtonRefinement(FastMath.log(p), k, theta, max_newton_iterations, x);
    }
    return x;
  }

  /**
   * Refinement of ChiSquared probit using Newton iterations.
   * 
   * A trick used by GNU R to improve precision.
   * 
   * @param logpt Target value of log p
   * @param k Alpha
   * @param theta Theta = 1 / Beta
   * @param maxit Maximum number of iterations to do
   * @param x Initial estimate
   * @return Refined value
   */
  protected static double gammaQuantileNewtonRefinement(final double logpt, final double k, final double theta, final int maxit, double x) {
    final double EPS_N = 1e-15; // Precision threshold
    // 0 is not possible, try MIN_NORMAL instead
    if(x <= 0) {
      x = Double.MIN_NORMAL;
    }
    // Current estimation
    double logpc = logcdf(x, k, theta);
    if(x == Double.MIN_NORMAL && logpc > logpt * (1. + 1e-7)) {
      return 0.;
    }
    if(logpc == Double.NEGATIVE_INFINITY) {
      return 0.;
    }
    // Refine by newton iterations
    for(int i = 0; i < maxit; i++) {
      // Error of current approximation
      final double logpe = logpc - logpt;
      if(Math.abs(logpe) < Math.abs(EPS_N * logpt)) {
        break;
      }
      // Step size is controlled by PDF:
      final double g = logpdf(x, k, theta);
      if(g == Double.NEGATIVE_INFINITY) {
        break;
      }
      final double newx = x - logpe * FastMath.exp(logpc - g);
      // New estimate:
      logpc = logcdf(newx, k, theta);
      if(Math.abs(logpc - logpt) > Math.abs(logpe) || (i > 0 && Math.abs(logpc - logpt) == Math.abs(logpe))) {
        // no further improvement
        break;
      }
      x = newx;
    }
    return x;
  }

  /**
   * Compute the Psi / Digamma function
   * <p>
   * Reference:
   * <p>
   * J. M. Bernando<br>
   * Algorithm AS 103: Psi (Digamma) Function<br>
   * Statistical Algorithms
   * <p>
   * TODO: is there a more accurate version maybe in R?
   *
   * @param x Position
   * @return digamma value
   */
  @Reference(authors = "J. M. Bernando", //
      title = "Algorithm AS 103: Psi (Digamma) Function", //
      booktitle = "Statistical Algorithms", //
      url = "https://doi.org/10.2307/2347257", //
      bibkey = "doi:10.2307/2347257")
  public static double digamma(double x) {
    if(!(x > 0)) {
      return Double.NaN;
    }
    // Method of equation 5:
    if(x <= 1e-5) {
      return -EULERS_CONST - 1. / x;
    }
    // Method of equation 4:
    else if(x > 49.) {
      final double ix2 = 1. / (x * x);
      // Partial series expansion
      return FastMath.log(x) - 0.5 / x - ix2 * ((1.0 / 12.) + ix2 * (1.0 / 120. - ix2 / 252.));
      // + O(x^8) error
    }
    else {
      // Stirling expansion
      return digamma(x + 1.) - 1. / x;
    }
  }

  /**
   * Compute the Trigamma function. Based on digamma.
   * <p>
   * TODO: is there a more accurate version maybe in R?
   *
   * @param x Position
   * @return trigamma value
   */
  public static double trigamma(double x) {
    if(!(x > 0)) {
      return Double.NaN;
    }
    // Method of equation 5:
    if(x <= 1e-5) {
      return 1. / (x * x);
    }
    // Method of equation 4:
    else if(x > 49.) {
      final double ix2 = 1. / (x * x);
      // Partial series expansion
      return 1 / x - ix2 / 2. + ix2 / x * (1.0 / 6. - ix2 * (1.0 / 30. + ix2 / 42.));
      // + O(x^8) error
    }
    else {
      // Stirling expansion
      return trigamma(x + 1.) - 1. / (x * x);
    }
  }

  /**
   * Parameterization class
   * <p>
   * TODO: allow alternate parameterization, with alpha+beta?
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * K parameter.
     */
    public static final OptionID K_ID = new OptionID("distribution.gamma.k", "Gamma distribution k = alpha parameter.");

    /**
     * Theta parameter.
     */
    public static final OptionID THETA_ID = new OptionID("distribution.gamma.theta", "Gamma distribution theta = 1/beta parameter.");

    /** Parameters. */
    double k, theta;

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
    }

    @Override
    protected GammaDistribution makeInstance() {
      return new GammaDistribution(k, theta, rnd);
    }
  }
}
