package de.lmu.ifi.dbs.elki.math.statistics.distribution;

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
import java.util.Arrays;
import java.util.Random;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.fitting.GaussianFittingFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.fitting.LevenbergMarquardtMethod;
import de.lmu.ifi.dbs.elki.math.statistics.GaussianKernelDensityFunction;
import de.lmu.ifi.dbs.elki.math.statistics.KernelDensityEstimator;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Log-Normal distribution.
 * 
 * The parameterization of this class is somewhere inbetween of GNU R and SciPy.
 * Similar to GNU R we use the logmean and logstddev. Similar to Scipy, we also
 * have a location parameter that shifts the distribution.
 * 
 * Our implementation maps to SciPy's as follows:
 * <tt>scipy.stats.lognorm(logstddev, shift, math.exp(logmean))</tt>
 * 
 * @author Erich Schubert
 */
@Alias({ "lognormal" })
public class LogNormalDistribution implements DistributionWithRandom {
  /**
   * Static estimator, using mean and variance.
   */
  public static NaiveEstimator NAIVE_ESTIMATOR = new NaiveEstimator();

  /**
   * Static estimator, more robust to outliers by using the median.
   */
  public static MADEstimator MAD_ESTIMATOR = new MADEstimator();

  /**
   * Static estimator for small sample sizes and <em>partial</em> data.
   */
  public static final LevenbergMarquardtKDEEstimator LM_KDE_ESTIMATOR = new LevenbergMarquardtKDEEstimator();

  /**
   * Mean value for the generator
   */
  private double logmean;

  /**
   * Standard deviation
   */
  private double logstddev;

  /**
   * Additional shift factor
   */
  private double shift = 0.;

  /**
   * The random generator.
   */
  private Random random;

  /**
   * Constructor for Log-Normal distribution
   * 
   * @param logmean Mean
   * @param logstddev Standard Deviation
   * @param shift Shifting offset
   * @param random Random generator
   */
  public LogNormalDistribution(double logmean, double logstddev, double shift, Random random) {
    super();
    this.logmean = logmean;
    this.logstddev = logstddev;
    this.shift = shift;
    this.random = random;
  }

  /**
   * Constructor.
   * 
   * @param logmean Mean
   * @param logstddev Standard deviation
   * @param shift Shifting offset
   */
  public LogNormalDistribution(double logmean, double logstddev, double shift) {
    this(logmean, logstddev, shift, null);
  }

  @Override
  public double pdf(double val) {
    return pdf(val - shift, logmean, logstddev);
  }

  @Override
  public double cdf(double val) {
    return cdf(val - shift, logmean, logstddev);
  }

  @Override
  public double quantile(double val) {
    return quantile(val, logmean, logstddev) + shift;
  }

  /**
   * Probability density function of the normal distribution.
   * 
   * <pre>
   * 1/(SQRT(2*pi)*sigma*x) * e^(-log(x-mu)^2/2sigma^2)
   * </pre>
   * 
   * 
   * @param x The value.
   * @param mu The mean.
   * @param sigma The standard deviation.
   * @return PDF of the given normal distribution at x.
   */
  public static double pdf(double x, double mu, double sigma) {
    final double x_mu = Math.log(x) - mu;
    final double sigmasq = sigma * sigma;
    return 1 / (MathUtil.SQRTTWOPI * sigma * x) * Math.exp(-.5 * x_mu * x_mu / sigmasq);
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
    return .5 * (1 + NormalDistribution.erf((Math.log(x) - mu) / (MathUtil.SQRT2 * sigma)));
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
    return Math.exp(mu + sigma * NormalDistribution.standardNormalQuantile(x));
  }

  @Override
  public double nextRandom() {
    return Math.exp(logmean + random.nextGaussian() * logstddev) + shift;
  }

  @Override
  public String toString() {
    return "LogNormalDistribution(logmean=" + logmean + ", logstddev=" + logstddev + ", shift=" + shift + ")";
  }

  /**
   * Naive distribution estimation using mean and sample variance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has LogNormalDistribution - - estimates
   */
  public static class NaiveEstimator implements DistributionEstimator<LogNormalDistribution> {
    /**
     * Private constructor, use static instance!
     */
    private NaiveEstimator() {
      // Do not instantiate
    }

    @Override
    public <A> LogNormalDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      MeanVariance mv = new MeanVariance();
      int size = adapter.size(data);
      for (int i = 0; i < size; i++) {
        final double val = adapter.getDouble(data, i);
        if (!(val > 0)) {
          throw new ArithmeticException("Cannot fit logNormal to a data set which includes non-positive values: " + val);
        }
        mv.put(Math.log(val));
      }
      return new LogNormalDistribution(mv.getMean(), mv.getSampleStddev(), 0.);
    }

    @Override
    public Class<? super LogNormalDistribution> getDistributionClass() {
      return LogNormalDistribution.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected NaiveEstimator makeInstance() {
        return NAIVE_ESTIMATOR;
      }
    }
  }

  /**
   * Estimator using Medians. More robust to outliers, and just slightly more
   * expensive (needs to copy the data for partial sorting to find the median).
   * 
   * References:
   * <p>
   * F. R. Hampel<br />
   * The Influence Curve and Its Role in Robust Estimation<br />
   * in: Journal of the American Statistical Association, June 1974, Vol. 69,
   * No. 346
   * </p>
   * <p>
   * P. J. Rousseeuw, C. Croux<br />
   * Alternatives to the Median Absolute Deviation<br />
   * in: Journal of the American Statistical Association, December 1993, Vol.
   * 88, No. 424, Theory and Methods
   * </p>
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has LogNormalDistribution - - estimates
   */
  @Reference(authors = "F. R. Hampel", title = "The Influence Curve and Its Role in Robust Estimation", booktitle = "Journal of the American Statistical Association, June 1974, Vol. 69, No. 346", url = "http://www.jstor.org/stable/10.2307/2285666")
  public static class MADEstimator implements DistributionEstimator<LogNormalDistribution> {
    @Override
    public <A> LogNormalDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      // TODO: detect pre-sorted data?
      final int len = adapter.size(data);
      // Modifiable copy:
      double[] x = new double[len];
      for (int i = 0; i < len; i++) {
        final double val = adapter.getDouble(data, i);
        if (!(val > 0)) {
          throw new ArithmeticException("Cannot fit logNormal to a data set which includes non-positive values: " + val);
        }
        x[i] = Math.log(val);
      }
      double median = QuickSelect.median(x);
      // Compute absolute deviations:
      for (int i = 0; i < len; i++) {
        x[i] = Math.abs(x[i] - median);
      }
      double mdev = QuickSelect.median(x);
      // Fallback if we have more than 50% ties to next largest.
      if (!(mdev > 0.)) {
        double min = Double.POSITIVE_INFINITY;
        for (double xi : x) {
          if (xi > 0. && xi < min) {
            min = xi;
          }
        }
        if (min < Double.POSITIVE_INFINITY) {
          mdev = min;
        } else {
          mdev = 1.0; // Maybe all constant. No real value.
        }
      }
      // The scaling factor is for consistency
      return new LogNormalDistribution(median, NormalDistribution.ONEBYPHIINV075 * mdev, 0.);
    }

    @Override
    public Class<? super LogNormalDistribution> getDistributionClass() {
      return LogNormalDistribution.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected MADEstimator makeInstance() {
        return MAD_ESTIMATOR;
      }
    }
  }

  /**
   * Distribution parameter estimation using Levenberg-Marquardt iterative
   * optimization and a kernel density estimation.
   * 
   * Note: this estimator is rather expensive, and needs optimization in the KDE
   * phase, which currently is O(n^2)!
   * 
   * This estimator is primarily attractive when only part of the distribution
   * was observed.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has LogNormalDistribution - - estimates
   */
  public static class LevenbergMarquardtKDEEstimator implements DistributionEstimator<LogNormalDistribution> {
    @Override
    public <A> LogNormalDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
      // We first need the basic parameters:
      final int len = adapter.size(data);
      MeanVariance mv = new MeanVariance();
      // X positions of samples
      double[] x = new double[len];
      for (int i = 0; i < len; i++) {
        final double val = adapter.getDouble(data, i);
        if (!(val > 0)) {
          throw new ArithmeticException("Cannot fit logNormal to a data set which includes non-positive values: " + val);
        }
        x[i] = Math.log(val);
        mv.put(x[i]);
      }
      // Sort our copy.
      Arrays.sort(x);
      double median = (x[len >> 1] + x[(len + 1) >> 1]) * .5;

      // Height = density, via KDE.
      KernelDensityEstimator de = new KernelDensityEstimator(x, GaussianKernelDensityFunction.KERNEL, 1e-6);
      double[] y = de.getDensity();

      // Weights:
      double[] s = new double[len];
      Arrays.fill(s, 1.0);

      // Initial parameter estimate:
      double[] params = { median, mv.getSampleStddev(), 1 };
      boolean[] dofit = { true, true, false };
      LevenbergMarquardtMethod fit = new LevenbergMarquardtMethod(GaussianFittingFunction.STATIC, params, dofit, x, y, s);
      fit.run();
      double[] ps = fit.getParams();
      return new LogNormalDistribution(ps[0], ps[1], 0.);
    }

    @Override
    public Class<? super LogNormalDistribution> getDistributionClass() {
      return LogNormalDistribution.class;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      @Override
      protected LevenbergMarquardtKDEEstimator makeInstance() {
        return LM_KDE_ESTIMATOR;
      }
    }
  }
}
