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
import de.lmu.ifi.dbs.elki.utilities.exceptions.NotImplementedException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * INCOMPLETE implementation of the poisson distribution.
 * <p>
 * TODO: continue implementing, CDF, invcdf and nextRandom are missing
 * <p>
 * References:
 * <p>
 * Catherine Loader<br>
 * Fast and Accurate Computation of Binomial Probabilities.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class PoissonDistribution extends AbstractDistribution {
  /**
   * Number of tries
   */
  private int n;

  /**
   * Success probability
   */
  private double p;

  /** Stirling error constants: 1./12 */
  private static final double S0 = 0.08333333333333333333333d;

  /** Stirling error constants: 1./360 */
  private static final double S1 = 0.0027777777777777777777777777778d;

  /** Stirling error constants: 1./1260 */
  private static final double S2 = 0.00079365079365079365079365d;

  /** Stirling error constants: 1./1680 */
  private static final double S3 = 0.000595238095238095238095238095d;

  /** Stirling error constants: 1./1188 */
  private static final double S4 = 0.00084175084175084175084175084175d;

  /**
   * Exact table values for n &lt;= 15 in steps of 0.5
   * <p>
   * sfe[n] = ln( (n!*e^n)/((n^n)*sqrt(2*pi*n)) )
   */
  private static final double[] STIRLING_EXACT_ERROR = { //
      0.0, // 0.0
      0.1534264097200273452913848, // 0.5
      0.0810614667953272582196702, // 1.0
      0.0548141210519176538961390, // 1.5
      0.0413406959554092940938221, // 2.0
      0.03316287351993628748511048, // 2.5
      0.02767792568499833914878929, // 3.0
      0.02374616365629749597132920, // 3.5
      0.02079067210376509311152277, // 4.0
      0.01848845053267318523077934, // 4.5
      0.01664469118982119216319487, // 5.0
      0.01513497322191737887351255, // 5.5
      0.01387612882307074799874573, // 6.0
      0.01281046524292022692424986, // 6.5
      0.01189670994589177009505572, // 7.0
      0.01110455975820691732662991, // 7.5
      0.010411265261972096497478567, // 8.0
      0.009799416126158803298389475, // 8.5
      0.009255462182712732917728637, // 9.0
      0.008768700134139385462952823, // 9.5
      0.008330563433362871256469318, // 10.0
      0.007934114564314020547248100, // 10.5
      0.007573675487951840794972024, // 11.0
      0.007244554301320383179543912, // 11.5
      0.006942840107209529865664152, // 12.0
      0.006665247032707682442354394, // 12.5
      0.006408994188004207068439631, // 13.0
      0.006171712263039457647532867, // 13.5
      0.005951370112758847735624416, // 14.0
      0.005746216513010115682023589, // 14.5
      0.0055547335519628013710386900 // 15.0
  };

  /**
   * Constructor.
   *
   * @param n Number of tries
   * @param p Success probability
   */
  public PoissonDistribution(int n, double p) {
    this(n, p, (Random) null);
  }

  /**
   * Constructor.
   *
   * @param n Number of tries
   * @param p Success probability
   * @param random Random generator
   */
  public PoissonDistribution(int n, double p, Random random) {
    super(random);
    this.n = n;
    this.p = p;
  }

  /**
   * Constructor.
   *
   * @param n Number of tries
   * @param p Success probability
   * @param random Random generator
   */
  public PoissonDistribution(int n, double p, RandomFactory random) {
    super(random);
    this.n = n;
    this.p = p;
  }

  /**
   * Poisson probability mass function (PMF) for integer values.
   *
   * @param x integer values
   * @return Probability
   */
  public double pmf(int x) {
    return pmf(x, n, p);
  }

  @Override
  public double pdf(double x) {
    // FIXME: return 0 for non-integer x?
    return pmf(x, n, p);
  }

  @Override
  public double logpdf(double x) {
    return FastMath.log(pmf(x, n, p));
  }

  /**
   * Poisson probability mass function (PMF) for integer values.
   *
   * @param x integer values
   * @return Probability
   */
  @Reference(title = "Fast and accurate computation of binomial probabilities", //
      authors = "C. Loader", booktitle = "", //
      url = "http://projects.scipy.org/scipy/raw-attachment/ticket/620/loader2000Fast.pdf", //
      bibkey = "web/Loader00")
  public static double pmf(double x, int n, double p) {
    // Invalid values
    if(x < 0 || x > n) {
      return 0.;
    }
    // Extreme probabilities
    if(p <= 0.) {
      return x == 0 ? 1. : 0.;
    }
    if(p >= 1.) {
      return x == n ? 1. : 0.;
    }
    final double q = 1 - p;
    // FIXME: check for x to be integer, return 0 otherwise?

    // Extreme values of x
    if(x == 0) {
      if(p < .1) {
        return FastMath.exp(-devianceTerm(n, n * q) - n * p);
      }
      else {
        return FastMath.exp(n * FastMath.log(q));
      }
    }
    if(x == n) {
      if(p > .9) {
        return FastMath.exp(-devianceTerm(n, n * p) - n * q);
      }
      else {
        return FastMath.exp(n * FastMath.log(p));
      }
    }
    final double lc = stirlingError(n) - stirlingError(x) - stirlingError(n - x) - devianceTerm(x, n * p) - devianceTerm(n - x, n * q);
    final double f = (MathUtil.TWOPI * x * (n - x)) / n;
    return FastMath.exp(lc) / FastMath.sqrt(f);
  }

  /**
   * Poisson probability mass function (PMF) for integer values.
   *
   * @param x integer values
   * @return Probability
   */
  public static double logpmf(double x, int n, double p) {
    // Invalid values
    if(x < 0 || x > n) {
      return Double.NEGATIVE_INFINITY;
    }
    // Extreme probabilities
    if(p <= 0.) {
      return x == 0 ? 0. : Double.NEGATIVE_INFINITY;
    }
    if(p >= 1.) {
      return x == n ? 0. : Double.NEGATIVE_INFINITY;
    }
    final double q = 1 - p;
    // FIXME: check for x to be integer, return 0 otherwise?

    // Extreme values of x
    if(x == 0) {
      if(p < .1) {
        return -devianceTerm(n, n * q) - n * p;
      }
      else {
        return n * FastMath.log(q);
      }
    }
    if(x == n) {
      if(p > .9) {
        return -devianceTerm(n, n * p) - n * q;
      }
      else {
        return n * FastMath.log(p);
      }
    }
    final double lc = stirlingError(n) - stirlingError(x) - stirlingError(n - x) - devianceTerm(x, n * p) - devianceTerm(n - x, n * q);
    final double f = (MathUtil.TWOPI * x * (n - x)) / n;
    return lc - .5 * FastMath.log(f);
  }

  @Override
  public double cdf(double val) {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  @Override
  public double quantile(double val) {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  @Override
  public double nextRandom() {
    // FIXME: implement!
    throw new NotImplementedException();
  }

  /**
   * Compute the poisson distribution PDF with an offset of + 1
   * <p>
   * pdf(x_plus_1 - 1, lambda)
   *
   * @param x_plus_1 x+1
   * @param lambda Lambda
   * @return pdf
   */
  public static double poissonPDFm1(double x_plus_1, double lambda) {
    if(Double.isInfinite(lambda)) {
      return 0.;
    }
    if(x_plus_1 > 1) {
      return rawProbability(x_plus_1 - 1, lambda);
    }
    if(lambda > Math.abs(x_plus_1 - 1) * MathUtil.LOG2 * Double.MAX_EXPONENT / 1e-14) {
      return FastMath.exp(-lambda - GammaDistribution.logGamma(x_plus_1));
    }
    else {
      return rawProbability(x_plus_1, lambda) * (x_plus_1 / lambda);
    }
  }

  /**
   * Compute the poisson distribution PDF with an offset of + 1
   * <p>
   * log pdf(x_plus_1 - 1, lambda)
   *
   * @param x_plus_1 x+1
   * @param lambda Lambda
   * @return pdf
   */
  public static double logpoissonPDFm1(double x_plus_1, double lambda) {
    if(Double.isInfinite(lambda)) {
      return Double.NEGATIVE_INFINITY;
    }
    if(x_plus_1 > 1) {
      return rawLogProbability(x_plus_1 - 1, lambda);
    }
    if(lambda > Math.abs(x_plus_1 - 1) * MathUtil.LOG2 * Double.MAX_EXPONENT / 1e-14) {
      return -lambda - GammaDistribution.logGamma(x_plus_1);
    }
    else {
      return rawLogProbability(x_plus_1, lambda) + FastMath.log(x_plus_1 / lambda);
    }
  }

  /**
   * Calculates the Stirling Error
   * <p>
   * stirlerr(n) = ln(n!) - ln(sqrt(2*pi*n)*(n/e)^n)
   *
   * @param n Parameter n
   * @return Stirling error
   */
  @Reference(title = "Fast and accurate computation of binomial probabilities", //
      authors = "C. Loader", booktitle = "", //
      url = "http://projects.scipy.org/scipy/raw-attachment/ticket/620/loader2000Fast.pdf", //
      bibkey = "web/Loader00")
  private static double stirlingError(int n) {
    // Try to use a table value:
    if(n < 16) {
      return STIRLING_EXACT_ERROR[n << 1];
    }
    final double nn = n * n;
    // Use the appropriate number of terms
    if(n > 500) {
      return (S0 - S1 / nn) / n;
    }
    if(n > 80) {
      return ((S0 - (S1 - S2 / nn)) / nn) / n;
    }
    if(n > 35) {
      return ((S0 - (S1 - (S2 - S3 / nn) / nn) / nn) / n);
    }
    return ((S0 - (S1 - (S2 - (S3 - S4 / nn) / nn) / nn) / nn) / n);
  }

  /**
   * Calculates the Stirling Error
   * <p>
   * stirlerr(n) = ln(n!) - ln(sqrt(2*pi*n)*(n/e)^n)
   *
   * @param n Parameter n
   * @return Stirling error
   */
  @Reference(title = "Fast and accurate computation of binomial probabilities", //
      authors = "C. Loader", booktitle = "", //
      url = "http://projects.scipy.org/scipy/raw-attachment/ticket/620/loader2000Fast.pdf", //
      bibkey = "web/Loader00")
  private static double stirlingError(double n) {
    if(n < 16.0) {
      // Our table has a step size of 0.5
      final double n2 = 2.0 * n;
      if(FastMath.floor(n2) == n2) { // Exact match
        return STIRLING_EXACT_ERROR[(int) n2];
      }
      else {
        return GammaDistribution.logGamma(n + 1.0) - (n + 0.5) * FastMath.log(n) + n - MathUtil.LOGSQRTTWOPI;
      }
    }
    final double nn = n * n;
    if(n > 500.0) {
      return (S0 - S1 / nn) / n;
    }
    if(n > 80.0) {
      return ((S0 - (S1 - S2 / nn)) / nn) / n;
    }
    if(n > 35.0) {
      return ((S0 - (S1 - (S2 - S3 / nn) / nn) / nn) / n);
    }
    return ((S0 - (S1 - (S2 - (S3 - S4 / nn) / nn) / nn) / nn) / n);
  }

  /**
   * Evaluate the deviance term of the saddle point approximation.
   * <p>
   * bd0(x,np) = x*ln(x/np)+np-x
   *
   * @param x probability density function position
   * @param np product of trials and success probability: n*p
   * @return Deviance term
   */
  @Reference(title = "Fast and accurate computation of binomial probabilities", //
      authors = "C. Loader", booktitle = "", //
      url = "http://projects.scipy.org/scipy/raw-attachment/ticket/620/loader2000Fast.pdf", //
      bibkey = "web/Loader00")
  private static double devianceTerm(double x, double np) {
    if(Math.abs(x - np) < 0.1 * (x + np)) {
      final double v = (x - np) / (x + np);

      double s = (x - np) * v;
      double ej = 2.0d * x * v;
      for(int j = 1;; j++) {
        ej *= v * v;
        final double s1 = s + ej / (2 * j + 1);
        if(s1 == s) {
          return s1;
        }
        s = s1;
      }
    }
    return x * FastMath.log(x / np) + np - x;
  }

  /**
   * Poisson distribution probability, but also for non-integer arguments.
   * <p>
   * lb^x exp(-lb) / x!
   *
   * @param x X
   * @param lambda lambda
   * @return Poisson distribution probability
   */
  public static double rawProbability(double x, double lambda) {
    // Extreme lambda
    if(lambda == 0) {
      return ((x == 0) ? 1. : 0.);
    }
    // Extreme values
    if(Double.isInfinite(lambda) || x < 0) {
      return 0.;
    }
    if(x <= lambda * Double.MIN_NORMAL) {
      return FastMath.exp(-lambda);
    }
    if(lambda < x * Double.MIN_NORMAL) {
      double r = -lambda + x * FastMath.log(lambda) - GammaDistribution.logGamma(x + 1);
      return FastMath.exp(r);
    }
    final double f = MathUtil.TWOPI * x;
    final double y = -stirlingError(x) - devianceTerm(x, lambda);
    return FastMath.exp(y) / FastMath.sqrt(f);
  }

  /**
   * Poisson distribution probability, but also for non-integer arguments.
   * <p>
   * lb^x exp(-lb) / x!
   *
   * @param x X
   * @param lambda lambda
   * @return Poisson distribution probability
   */
  public static double rawLogProbability(double x, double lambda) {
    // Extreme lambda
    if(lambda == 0) {
      return ((x == 0) ? 1. : Double.NEGATIVE_INFINITY);
    }
    // Extreme values
    if(Double.isInfinite(lambda) || x < 0) {
      return Double.NEGATIVE_INFINITY;
    }
    if(x <= lambda * Double.MIN_NORMAL) {
      return -lambda;
    }
    if(lambda < x * Double.MIN_NORMAL) {
      return -lambda + x * FastMath.log(lambda) - GammaDistribution.logGamma(x + 1);
    }
    final double f = MathUtil.TWOPI * x;
    final double y = -stirlingError(x) - devianceTerm(x, lambda);
    return -0.5 * FastMath.log(f) + y;
  }

  @Override
  public String toString() {
    return "PoissonDistribution(n=" + n + ", p=" + p + ")";
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /**
     * Number of trials.
     */
    public static final OptionID N_ID = new OptionID("distribution.poisson.n", "Number of trials.");

    /**
     * Success probability.
     */
    public static final OptionID PROB_ID = new OptionID("distribution.poisson.probability", "Success probability.");

    /**
     * Number of trials.
     */
    int n;

    /**
     * Success probability.
     */
    double p;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter nP = new IntParameter(N_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(nP)) {
        n = nP.intValue();
      }

      DoubleParameter probP = new DoubleParameter(PROB_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
      if(config.grab(probP)) {
        p = probP.doubleValue();
      }
    }

    @Override
    protected PoissonDistribution makeInstance() {
      return new PoissonDistribution(n, p, rnd);
    }
  }
}
