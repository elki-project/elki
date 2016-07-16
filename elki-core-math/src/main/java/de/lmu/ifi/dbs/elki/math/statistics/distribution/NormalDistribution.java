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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Gaussian distribution aka normal distribution
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Alias({ "GaussianDistribution", "normal", "gauss" })
public class NormalDistribution extends AbstractDistribution {
  /**
   * Coefficients for erf approximation.
   * 
   * Loosely based on http://www.netlib.org/specfun/erf
   */
  static final double[] ERFAPP_A = { 1.85777706184603153e-1, 3.16112374387056560e+0, 1.13864154151050156E+2, 3.77485237685302021e+2, 3.20937758913846947e+3 };

  /**
   * Coefficients for erf approximation.
   * 
   * Loosely based on http://www.netlib.org/specfun/erf
   */
  static final double[] ERFAPP_B = { 1.00000000000000000e00, 2.36012909523441209e01, 2.44024637934444173e02, 1.28261652607737228e03, 2.84423683343917062e03 };

  /**
   * Coefficients for erf approximation.
   * 
   * Loosely based on http://www.netlib.org/specfun/erf
   */
  static final double[] ERFAPP_C = { 2.15311535474403846e-8, 5.64188496988670089e-1, 8.88314979438837594e00, 6.61191906371416295e01, 2.98635138197400131e02, 8.81952221241769090e02, 1.71204761263407058e03, 2.05107837782607147e03, 1.23033935479799725E03 };

  /**
   * Coefficients for erf approximation.
   * 
   * Loosely based on http://www.netlib.org/specfun/erf
   */
  static final double[] ERFAPP_D = { 1.00000000000000000e00, 1.57449261107098347e01, 1.17693950891312499e02, 5.37181101862009858e02, 1.62138957456669019e03, 3.29079923573345963e03, 4.36261909014324716e03, 3.43936767414372164e03, 1.23033935480374942e03 };

  /**
   * Coefficients for erf approximation.
   * 
   * Loosely based on http://www.netlib.org/specfun/erf
   */
  static final double[] ERFAPP_P = { 1.63153871373020978e-2, 3.05326634961232344e-1, 3.60344899949804439e-1, 1.25781726111229246e-1, 1.60837851487422766e-2, 6.58749161529837803e-4 };

  /**
   * Coefficients for erf approximation.
   * 
   * Loosely based on http://www.netlib.org/specfun/erf
   */
  static final double[] ERFAPP_Q = { 1.00000000000000000e00, 2.56852019228982242e00, 1.87295284992346047e00, 5.27905102951428412e-1, 6.05183413124413191e-2, 2.33520497626869185e-3 };

  /**
   * Treshold for switching nethods for erfinv approximation
   */
  static final double P_LOW = 0.02425D;

  /**
   * Treshold for switching nethods for erfinv approximation
   */
  static final double P_HIGH = 1.0D - P_LOW;

  /**
   * Coefficients for erfinv approximation, rational version
   */
  static final double[] ERFINV_A = { -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00 };

  /**
   * Coefficients for erfinv approximation, rational version
   */
  static final double[] ERFINV_B = { -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01 };

  /**
   * Coefficients for erfinv approximation, rational version
   */
  static final double[] ERFINV_C = { -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00 };

  /**
   * Coefficients for erfinv approximation, rational version
   */
  static final double[] ERFINV_D = { 7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00 };

  /**
   * CDFINV(0.75)
   */
  public static final double PHIINV075 = 0.67448975019608171;

  /**
   * 1 / CDFINV(0.75)
   */
  public static final double ONEBYPHIINV075 = 1.48260221850560186054;

  /**
   * Mean value for the generator
   */
  private double mean;

  /**
   * Standard deviation
   */
  private double stddev;

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param random Random generator
   */
  public NormalDistribution(double mean, double stddev, RandomFactory random) {
    super(random);
    this.mean = mean;
    this.stddev = stddev;
  }

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   * @param random Random generator
   */
  public NormalDistribution(double mean, double stddev, Random random) {
    super(random);
    this.mean = mean;
    this.stddev = stddev;
  }

  /**
   * Constructor for Gaussian distribution
   * 
   * @param mean Mean
   * @param stddev Standard Deviation
   */
  public NormalDistribution(double mean, double stddev) {
    this(mean, stddev, (Random) null);
  }

  @Override
  public double pdf(double val) {
    return pdf(val, mean, stddev);
  }

  @Override
  public double logpdf(double val) {
    return logpdf(val, mean, stddev);
  }

  @Override
  public double cdf(double val) {
    return cdf(val, mean, stddev);
  }

  @Override
  public double quantile(double q) {
    return quantile(q, mean, stddev);
  }

  @Override
  public double nextRandom() {
    return mean + random.nextGaussian() * stddev;
  }

  @Override
  public String toString() {
    return "NormalDistribution(mean=" + mean + ", stddev=" + stddev + ")";
  }

  /**
   * @return the mean
   */
  public double getMean() {
    return mean;
  }

  /**
   * @return the standard deviation
   */
  public double getStddev() {
    return stddev;
  }

  /**
   * Complementary error function for Gaussian distributions = Normal
   * distributions.
   * 
   * Numerical approximation using taylor series. Implementation loosely based
   * on http://www.netlib.org/specfun/erf
   * 
   * @param x parameter value
   * @return erfc(x)
   */
  public static double erfc(double x) {
    if(Double.isNaN(x)) {
      return Double.NaN;
    }
    if(Double.isInfinite(x)) {
      return (x < 0.0) ? 2 : 0;
    }

    double result = Double.NaN;
    double absx = Math.abs(x);
    // First approximation interval
    if(absx < 0.46875) {
      double z = x * x;
      result = 1 - x * ((((ERFAPP_A[0] * z + ERFAPP_A[1]) * z + ERFAPP_A[2]) * z + ERFAPP_A[3]) * z + ERFAPP_A[4]) / ((((ERFAPP_B[0] * z + ERFAPP_B[1]) * z + ERFAPP_B[2]) * z + ERFAPP_B[3]) * z + ERFAPP_B[4]);
    }
    // Second approximation interval
    else if(absx < 4.0) {
      double z = absx;
      result = ((((((((ERFAPP_C[0] * z + ERFAPP_C[1]) * z + ERFAPP_C[2]) * z + ERFAPP_C[3]) * z + ERFAPP_C[4]) * z + ERFAPP_C[5]) * z + ERFAPP_C[6]) * z + ERFAPP_C[7]) * z + ERFAPP_C[8]) / ((((((((ERFAPP_D[0] * z + ERFAPP_D[1]) * z + ERFAPP_D[2]) * z + ERFAPP_D[3]) * z + ERFAPP_D[4]) * z + ERFAPP_D[5]) * z + ERFAPP_D[6]) * z + ERFAPP_D[7]) * z + ERFAPP_D[8]);
      double rounded = Math.round(result * 16.0) / 16.0;
      double del = (absx - rounded) * (absx + rounded);
      result = Math.exp(-rounded * rounded) * Math.exp(-del) * result;
      if(x < 0.0) {
        result = 2.0 - result;
      }
    }
    // Third approximation interval
    else {
      double z = 1.0 / (absx * absx);
      result = z * (((((ERFAPP_P[0] * z + ERFAPP_P[1]) * z + ERFAPP_P[2]) * z + ERFAPP_P[3]) * z + ERFAPP_P[4]) * z + ERFAPP_P[5]) / (((((ERFAPP_Q[0] * z + ERFAPP_Q[1]) * z + ERFAPP_Q[2]) * z + ERFAPP_Q[3]) * z + ERFAPP_Q[4]) * z + ERFAPP_Q[5]);
      result = (MathUtil.ONE_BY_SQRTPI - result) / absx;
      double rounded = Math.round(result * 16.0) / 16.0;
      double del = (absx - rounded) * (absx + rounded);
      result = Math.exp(-rounded * rounded) * Math.exp(-del) * result;
      if(x < 0.0) {
        result = 2.0 - result;
      }
    }
    return result;
  }

  /**
   * Error function for Gaussian distributions = Normal distributions.
   * 
   * Numerical approximation using taylor series. Implementation loosely based
   * on http://www.netlib.org/specfun/erf
   * 
   * @param x parameter value
   * @return erf(x)
   */
  public static double erf(double x) {
    return 1 - erfc(x);
  }

  /**
   * Inverse error function.
   * 
   * @param x parameter value
   * @return erfinv(x)
   */
  public static double erfinv(double x) {
    return standardNormalQuantile(0.5 * (x + 1)) * MathUtil.SQRTHALF;
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi*sigma^2)) * e^(-(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double pdf(double x, double mu, double sigma) {
    x = (x - mu) / sigma;
    return MathUtil.ONE_BY_SQRTTWOPI / sigma * Math.exp(-.5 * x * x);
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi*sigma^2)) * e^(-(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double logpdf(double x, double mu, double sigma) {
    x = (x - mu) / sigma;
    return MathUtil.LOG_ONE_BY_SQRTTWOPI - Math.log(sigma) -.5 * x * x;
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi*sigma^2)) * e^(-(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @return PDF of the given normal distribution at x.
   */
  public static double standardNormalLogPDF(double x) {
    return -.5 * x * x + MathUtil.LOG_ONE_BY_SQRTTWOPI;
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi*sigma^2)) * e^(-(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * @param x The value.
   * @return PDF of the given normal distribution at x.
   */
  public static double standardNormalPDF(double x) {
    return Math.exp(-.5 * x * x) * MathUtil.ONE_BY_SQRTTWOPI;
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * 
   * @param x value to evaluate CDF at
   * @param mu Mean value
   * @param sigma Standard deviation.
   * @return The CDF of the given normal distribution at x.
   */
  public static double cdf(double x, double mu, double sigma) {
    x = (x - mu) / sigma;
    return .5 + .5 * erf(x * MathUtil.SQRTHALF);
  }

  /**
   * Cumulative probability density function (CDF) of a normal distribution.
   * 
   * @param x value to evaluate CDF at
   * @return The CDF of the given normal distribution at x.
   */
  public static double standardNormalCDF(double x) {
    return .5 + .5 * erf(x * MathUtil.SQRTHALF);
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
  public static double quantile(double x, double mu, double sigma) {
    return mu + sigma * standardNormalQuantile(x);
  }

  /**
   * Approximate the inverse error function for normal distributions.
   * 
   * Largely based on:
   * <p>
   * http://www.math.uio.no/~jacklam/notes/invnorm/index.html <br>
   * by Peter John Acklam
   * </p>
   * 
   * FIXME: precision of this seems to be rather low, compared to our other
   * functions. Only about 8-9 digits agree with SciPy/GNU R.
   * 
   * @param d Quantile. Must be in [0:1], obviously.
   * @return Inverse erf.
   */
  public static double standardNormalQuantile(double d) {
    if(d == 0) {
      return Double.NEGATIVE_INFINITY;
    }
    else if(d == 1) {
      return Double.POSITIVE_INFINITY;
    }
    else if(Double.isNaN(d) || d < 0 || d > 1) {
      return Double.NaN;
    }
    else if(d < P_LOW) {
      // Rational approximation for lower region:
      double q = Math.sqrt(-2 * Math.log(d));
      return (((((ERFINV_C[0] * q + ERFINV_C[1]) * q + ERFINV_C[2]) * q + ERFINV_C[3]) * q + ERFINV_C[4]) * q + ERFINV_C[5]) / ((((ERFINV_D[0] * q + ERFINV_D[1]) * q + ERFINV_D[2]) * q + ERFINV_D[3]) * q + 1);
    }
    else if(P_HIGH < d) {
      // Rational approximation for upper region:
      double q = Math.sqrt(-2 * Math.log(1 - d));
      return -(((((ERFINV_C[0] * q + ERFINV_C[1]) * q + ERFINV_C[2]) * q + ERFINV_C[3]) * q + ERFINV_C[4]) * q + ERFINV_C[5]) / ((((ERFINV_D[0] * q + ERFINV_D[1]) * q + ERFINV_D[2]) * q + ERFINV_D[3]) * q + 1);
    }
    else {
      // Rational approximation for central region:
      double q = d - 0.5D;
      double r = q * q;
      return (((((ERFINV_A[0] * r + ERFINV_A[1]) * r + ERFINV_A[2]) * r + ERFINV_A[3]) * r + ERFINV_A[4]) * r + ERFINV_A[5]) * q / (((((ERFINV_B[0] * r + ERFINV_B[1]) * r + ERFINV_B[2]) * r + ERFINV_B[3]) * r + ERFINV_B[4]) * r + 1);
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDistribution.Parameterizer {
    /** Parameters. */
    double mu, sigma;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter muP = new DoubleParameter(LOCATION_ID);
      if(config.grab(muP)) {
        mu = muP.doubleValue();
      }

      DoubleParameter sigmaP = new DoubleParameter(SCALE_ID);
      sigmaP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(sigmaP)) {
        sigma = sigmaP.doubleValue();
      }
    }

    @Override
    protected NormalDistribution makeInstance() {
      return new NormalDistribution(mu, sigma, rnd);
    }
  }
}
