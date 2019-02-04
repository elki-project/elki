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
 * Beta Distribution with implementation of the regularized incomplete beta
 * function
 * 
 * @author Jan Brusis
 * @author Erich Schubert
 * @since 0.5.0
 */
public class BetaDistribution extends AbstractDistribution {
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
    this(a, b, (Random) null);
  }

  /**
   * Constructor.
   * 
   * @param a shape Parameter a
   * @param b shape Parameter b
   * @param random Random generator
   */
  public BetaDistribution(double a, double b, Random random) {
    super(random);
    if(a <= 0.0 || b <= 0.0) {
      throw new IllegalArgumentException("Invalid parameters for Beta distribution.");
    }

    this.alpha = a;
    this.beta = b;
    this.logbab = logBeta(a, b);
  }

  /**
   * Constructor.
   * 
   * @param a shape Parameter a
   * @param b shape Parameter b
   * @param random Random generator
   */
  public BetaDistribution(double a, double b, RandomFactory random) {
    super(random);
    if(a <= 0.0 || b <= 0.0) {
      throw new IllegalArgumentException("Invalid parameters for Beta distribution.");
    }
    this.alpha = a;
    this.beta = b;
    this.logbab = logBeta(a, b);
  }

  @Override
  public double pdf(double val) {
    return (val < 0. || val > 1.) ? 0. : //
        val == 0. ? (alpha > 1. ? 0. : alpha < 1. ? Double.POSITIVE_INFINITY : beta) : //
            val == 1. ? (beta > 1. ? 0. : beta < 1. ? Double.POSITIVE_INFINITY : alpha) : //
                FastMath.exp(-logbab + FastMath.log(val) * (alpha - 1) + FastMath.log1p(-val) * (beta - 1));
  }

  @Override
  public double logpdf(double val) {
    return (val < 0. || val > 1.) ? Double.NEGATIVE_INFINITY : //
        val == 0. ? (alpha > 1. ? Double.NEGATIVE_INFINITY : alpha < 1. ? Double.POSITIVE_INFINITY : FastMath.log(beta)) : //
            val == 1. ? (beta > 1. ? Double.NEGATIVE_INFINITY : beta < 1. ? Double.POSITIVE_INFINITY : FastMath.log(alpha)) : //
                -logbab + FastMath.log(val) * (alpha - 1) + FastMath.log1p(-val) * (beta - 1);
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
    double bt = FastMath.exp(-logbab + alpha * FastMath.log(x) + beta * FastMath.log1p(-x));
    return (x < (alpha + 1.0) / (alpha + beta + 2.0)) //
        ? bt * regularizedIncBetaCF(alpha, beta, x) / alpha //
        : 1.0 - bt * regularizedIncBetaCF(beta, alpha, 1.0 - x) / beta;
  }

  @Override
  public double quantile(double x) {
    return (x < 0 || x > 1 || Double.isNaN(x)) ? Double.NaN : //
        x == 0 ? 0 : x == 1 ? 1 : x > 0.5 //
            ? 1 - rawQuantile(1 - x, beta, alpha, logbab) //
            : rawQuantile(x, alpha, beta, logbab);
  }

  @Override
  public double nextRandom() {
    double x = GammaDistribution.nextRandom(alpha, 1, random);
    double y = GammaDistribution.nextRandom(beta, 1, random);
    return x / (x + y);
  }

  @Override
  public String toString() {
    return "BetaDistribution(alpha=" + alpha + ", beta=" + beta + ")";
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
    return (alpha <= 0. || beta <= 0. || Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(val)) ? Double.NaN : //
        (val < 0. || val > 1.) ? 0. : //
            val == 0. ? (alpha > 1. ? 0. : alpha < 1. ? Double.POSITIVE_INFINITY : beta) : //
                val == 1. ? (beta > 1. ? 0. : beta < 1. ? Double.POSITIVE_INFINITY : alpha) : //
                    FastMath.exp(-logBeta(alpha, beta) + FastMath.log(val) * (alpha - 1) + FastMath.log1p(-val) * (beta - 1));
  }

  /**
   * Static version of the PDF of the beta distribution
   * 
   * @param val Value
   * @param alpha Shape parameter a
   * @param beta Shape parameter b
   * @return probability density
   */
  public static double logpdf(double val, double alpha, double beta) {
    return (alpha <= 0. || beta <= 0. || Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(val)) ? Double.NaN : //
        (val < 0. || val > 1.) ? Double.NEGATIVE_INFINITY : //
            val == 0. ? (alpha > 1. ? Double.NEGATIVE_INFINITY : alpha < 1. ? Double.POSITIVE_INFINITY : FastMath.log(beta)) //
                : val == 1. ? (beta > 1. ? Double.NEGATIVE_INFINITY : beta < 1. ? Double.POSITIVE_INFINITY : FastMath.log(alpha)) //
                    : -logBeta(alpha, beta) + FastMath.log(val) * (alpha - 1) + FastMath.log1p(-val) * (beta - 1);
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
    double bt = FastMath.exp(-logBeta(alpha, beta) + alpha * FastMath.log(x) + beta * FastMath.log1p(-x));
    return (x < (alpha + 1.0) / (alpha + beta + 2.0)) //
        ? bt * regularizedIncBetaCF(alpha, beta, x) / alpha //
        : 1.0 - bt * regularizedIncBetaCF(beta, alpha, 1.0 - x) / beta;
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
    final double alphapbeta = alpha + beta;
    final double a1 = alpha - 1.0;
    final double b1 = beta - 1.0;
    final double mu = alpha / alphapbeta;
    final double lnmu = FastMath.log(mu);
    final double lnmuc = FastMath.log1p(-mu);
    double t = FastMath.sqrt(alpha * beta / (alphapbeta * alphapbeta * (alphapbeta + 1.0)));
    final double xu;
    if(x > alpha / alphapbeta) {
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
      sum += GAUSSLEGENDRE_W[i] * FastMath.exp(a1 * (FastMath.log(t) - lnmu) + b1 * (FastMath.log1p(-t) - lnmuc));
    }
    double ans = sum * (xu - x) * FastMath.exp(a1 * lnmu - GammaDistribution.logGamma(alpha) + b1 * lnmuc - GammaDistribution.logGamma(beta) + GammaDistribution.logGamma(alphapbeta));
    return ans > 0 ? 1.0 - ans : -ans;
  }

  /**
   * Compute quantile (inverse cdf) for Beta distributions.
   * 
   * @param p Probability
   * @param alpha Shape parameter a
   * @param beta Shape parameter b
   * @return Probit for Beta distribution
   */
  public static double quantile(double p, double alpha, double beta) {
    return (Double.isNaN(alpha) || Double.isNaN(beta) || Double.isNaN(p) || alpha < 0. || beta < 0. || p < 0 || p > 1) ? Double.NaN : //
        p == 0 ? 0. : p == 1 ? 1. : p > 0.5 //
            ? 1 - rawQuantile(1 - p, beta, alpha, logBeta(beta, alpha)) //
            : rawQuantile(p, alpha, beta, logBeta(alpha, beta));
  }

  /**
   * Raw quantile function
   *
   * @param p P, must be 0 &lt; p &lt;= .5
   * @param alpha Alpha
   * @param beta Beta
   * @param logbeta log Beta(alpha, beta)
   * @return Position
   */
  protected static double rawQuantile(double p, double alpha, double beta, final double logbeta) {
    // Initial estimate for x
    double x;
    {
      // Very fast approximation of y.
      double tmp = FastMath.sqrt(-2 * FastMath.log(p));
      double y = tmp - (2.30753 + 0.27061 * tmp) / (1. + (0.99229 + 0.04481 * tmp) * tmp);

      if(alpha > 1 && beta > 1) {
        double r = (y * y - 3.) / 6.;
        double s = 1. / (alpha + alpha - 1.);
        double t = 1. / (beta + beta - 1.);
        double h = 2. / (s + t);
        double w = y * FastMath.sqrt(h + r) / h - (t - s) * (r + 5. / 6. - 2. / (3. * h));
        x = alpha / (alpha + beta * FastMath.exp(w + w));
      }
      else {
        double r = beta + beta;
        double t = 1. / (9. * beta);
        final double a = 1. - t + y * FastMath.sqrt(t);
        t = r * a * a * a;
        if(t <= 0.) {
          x = 1. - FastMath.exp((FastMath.log1p(-p) + FastMath.log(beta) + logbeta) / beta);
        }
        else {
          t = (4. * alpha + r - 2.) / t;
          if(t <= 1.) {
            x = FastMath.exp((FastMath.log(p * alpha) + logbeta) / alpha);
          }
          else {
            x = 1. - 2. / (t + 1.);
          }
        }
      }
      // Degenerate initial approximations
      if(x < 3e-308 || x > 1 - 2.22e-16) {
        x = 0.5;
      }
    }

    // Newon-Raphson method using the CDF
    {
      final double ialpha = 1 - alpha;
      final double ibeta = 1 - beta;

      // Desired accuracy, as from GNU R adoption of AS 109
      final double acu = Math.max(1e-300, FastMath.pow(10., -13 - 2.5 / (alpha * alpha) - .5 / (p * p)));
      double prevstep = 0., y = 0., stepsize = 1;

      for(int outer = 0; outer < 1000; outer++) {
        // Current CDF value
        double ynew = cdf(x, alpha, beta);
        if(Double.isInfinite(ynew)) { // Degenerated.
          return Double.NaN;
        }
        // Error gradient
        ynew = (ynew - p) * FastMath.exp(logbeta + ialpha * FastMath.log(x) + ibeta * FastMath.log1p(-x));
        if(ynew * y <= 0.) {
          prevstep = Math.max(Math.abs(stepsize), 3e-308);
        }
        // Inner loop: try different step sizes: y * 3^-i
        double g = 1, xnew = 0.;
        for(int inner = 0; inner < 1000; inner++) {
          stepsize = g * ynew;
          if(Math.abs(stepsize) < prevstep) {
            xnew = x - stepsize; // Candidate x
            if(xnew >= 0. && xnew <= 1.) {
              // Close enough
              if(prevstep <= acu || Math.abs(ynew) <= acu) {
                return x;
              }
              if(xnew != 0. && xnew != 1.) {
                break;
              }
            }
          }
          g /= 3.;
        }
        // Convergence
        if(Math.abs(xnew - x) < 1e-15 * x) {
          return x;
        }
        // Iterate with new values
        x = xnew;
        y = ynew;
      }
    }
    // Not converged in Newton-Raphson
    throw new ArithmeticException("Beta quantile computation did not converge.");
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Alpha parameter.
     */
    public static final OptionID ALPHA_ID = new OptionID("distribution.beta.alpha", "Beta distribution alpha parameter");

    /**
     * Beta parameter.
     */
    public static final OptionID BETA_ID = new OptionID("distribution.beta.beta", "Beta distribution beta parameter");

    /** Parameters. */
    double alpha, beta;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      DoubleParameter betaP = new DoubleParameter(BETA_ID);
      if(config.grab(betaP)) {
        beta = betaP.doubleValue();
      }
    }

    @Override
    protected BetaDistribution makeInstance() {
      return new BetaDistribution(alpha, beta, rnd);
    }
  }
}
